# FoodMap 后端架构审查报告

- 审查日期：2026-11-06
- 审查范围：`after/` 下全部 Java 微服务模块、`deploy/` 部署配置、`harness/` 质量门禁
- 审查视角：架构搭建、编码结构、编码规范、安全、数据一致性、可观测性、测试与交付
- 审查方式：静态代码审阅 + 本地构建测试 + 关键安全点动态验证
- 结论口径：以“可对外生产上线”的企业级标准评估，不以“MVP 能跑通”为标准

---

## 1. 总体结论

**FoodMap 的文档体系、数据库设计和基础设施抽象已经明显高于普通个人项目，但当前代码仍属于“高质量骨架”，尚未达到企业级生产上线标准。**

一句话判断：

> 架构蓝图是 7 分，代码骨架是 6 分，生产就绪度是 3 分。

主要问题不是“不会写”，而是**最后一公里没有闭环**：

- 认证密钥没有真正接入配置，签发端和网关端可能使用不同密钥；
- 内部服务之间采用“明文身份头”信任模型，没有服务身份认证；
- 登出拒绝名单可以被 Token 后缀绕过；
- 日志平台设计完整，但应用日志没有落盘、应用服务没有编排，链路实际不通；
- 没有 Dockerfile、没有 CI、没有 HTTPS、没有指标和链路追踪；
- 测试以单元测试为主，没有真实数据库/中间件集成测试，也没有强制质量门禁。

### 1.1 成熟度评分

| 维度 | 评分（1-5） | 判断 |
| --- | --- | --- |
| 架构搭建与边界 | 3.5 | 服务拆分、数据库归属、文档清晰；核心域未实现，common 过重，内部信任模型有硬伤 |
| 编码结构 | 2.5 | 目录齐全，但依赖倒置、领域层空心化、God Service、遗留兼容代码多 |
| 编码规范 | 3.5 | Javadoc、命名、统一响应、日志脱敏意识好；异常映射不全、规范执行靠脚本 |
| 安全与合规 | 1.5 | 密钥硬编码、内部接口无认证、后台权限头可伪造、无 TLS、无限流 |
| 数据与一致性 | 3.0 | Schema 质量高、Flyway 基本规范；跨服务一致性、并发控制、日志库迁移有缺口 |
| 测试与质量门禁 | 2.0 | 118 个单测，0 个真实集成测试；无 CI、无覆盖率门禁、无静态分析 |
| 部署与运维 | 1.5 | 无 Dockerfile、应用服务未编排、无 TLS、可观测性缺失 |
| 文档与治理 | 4.5 | CODEX、AGENTS、harness、skills 体系完整；但存在文档与代码漂移 |
| **综合** | **2.8** | **未达企业级上线门槛；建议先完成 P0 再进入核心业务开发** |

### 1.2 建议的推进顺序

1. 先修 5 个 P0（安全与可部署性），否则后续业务越写越难改。
2. 再补 P1（分层、一致性、可观测性、测试、CI）。
3. 最后做 P2（代码结构优化、规范收口），同时开始门店/推荐/可见范围等核心域。
4. 核心域开发时，优先把“可见范围”做成独立策略组件并配集成测试，这是 FoodMap 最核心、最容易出错的部分。

---

## 2. 审查范围与规模

| 指标 | 数值 |
| --- | --- |
| Maven 模块 | 11 个（含 common、gateway、auth、user、relation、store、recommendation、community、media、admin、log） |
| Java 主代码 | 284 个文件，约 14,339 行 |
| 测试代码 | 52 个文件，118 个 `@Test` |
| Flyway 迁移 | 9 个（auth 5、user 3、admin 1；log 不在模块内） |
| Mapper XML | 34 个 |
| 已实现业务 | auth、user、admin、log、gateway |
| 空壳服务 | relation、store、recommendation、community、media，每个约 33 行（仅启动类 + health） |

### 2.1 已执行的验证

| 验证 | 结果 |
| --- | --- |
| `mvn -q -B test`（after 全模块） | 通过，exit 0 |
| `./harness/scripts/validate-backend.sh` | 输出 PASS，但只执行 `mvn -q -DskipTests validate`，不编译、不跑测试 |
| Token 后缀验证（本地 probe） | 追加 `.suffix` 后 `parseAccessToken` 仍成功，Token 哈希变化 |

> 注意：harness 的 PASS 不能等同于“质量通过”。见 6.4 节。

---

## 3. 做得好的地方

先明确优点，避免把“骨架阶段”误判为“全面不合格”。

### 3.1 文档与规范体系

- `CODEX-product.md`、`CODEX-front.md`、`CODEX-after.md`、`CODEX-gen.md`、`AGENTS.md` 的分层清晰，产品、前端、后端、协作规则各有归属。
- `skills/`、`.agents/`、`harness/` 形成了可复用的开发约束，这在个人项目中很少见。
- 服务边界、数据库归属、API 路径、错误码、日志字段都有文字约定。

### 3.2 数据库设计质量较高

- 业务主键与数据库自增主键分离，跨服务引用业务主键。
- 逻辑删除配合 PostgreSQL 部分唯一索引，支持删除后重建。
- 表、字段中文注释齐全，Flyway 版本化迁移。
- auth 的 `auth_identity_bindings`、`auth_sessions` 设计考虑了身份绑定和会话事实，方向正确。

### 3.3 统一契约与日志意识

- `ApiResponse`、`PageResponse`、`CommonErrorCode` 统一了响应和错误码。
- `SafeLog`、`LogMasker`、`LogContext`、`TraceHeaders` 统一了日志字段、脱敏和 requestId/traceId 透传。
- 网关会移除外部伪造的 `X-FoodMap-User-Id` / `X-FoodMap-Account-Id`，这一点做对了。
- 测试对 Token 解析、脱敏、SQL 日志、分布式锁等基础能力有覆盖。

### 3.4 分层与端口适配器的尝试

- Controller → Service 接口 → ServiceImpl → Repository 端口 → MyBatis 实现的链路基本成型。
- 没有发现跨服务直接访问对方数据库。
- ServiceImpl 只依赖仓储端口，基础设施实现没有大面积向上穿透（但端口返回实体的问题见 4.2）。

---

## 4. 架构搭建层面的问题

### P0-1 认证密钥体系没有闭环，生产环境无法安全签发/校验 Token

**现象**

- 网关从配置读取 `foodmap.security.token-secret`，默认值是公开字符串 `foodmap-local-token-secret-please-change`。
- 认证服务的 `HmacTokenIssuer` 是 `@Component`，但只有一个无参构造器使用硬编码默认密钥；认证服务没有任何配置类或 `@Value` 把 `FOODMAP_TOKEN_SECRET` 注入进去。
- `deploy/env.example` 里写的是 `JWT_SECRET`，代码里没有任何地方读取这个变量。
- `Pbkdf2PasswordHashService` 的 pepper 同样使用硬编码默认值 `foodmap-local-pepper`。
- 后台管理服务的 pepper 使用 `@Value("${foodmap.admin.password.pepper:foodmap-admin-local-pepper}")`，默认值也是公开的。

**证据**

- [HmacTokenIssuer.java](after/foodmap-auth-service/src/main/java/com/foodmap/auth/domain/HmacTokenIssuer.java#L23-L24)
- [GatewayAuthFilter.java](after/foodmap-gateway-service/src/main/java/com/foodmap/gateway/filter/GatewayAuthFilter.java#L57)
- [application.yml（gateway）](after/foodmap-gateway-service/src/main/resources/application.yml#L65)
- [env.example](deploy/env.example#L145)
- [Pbkdf2PasswordHashService.java](after/foodmap-auth-service/src/main/java/com/foodmap/auth/domain/Pbkdf2PasswordHashService.java#L34)

**影响**

- 如果不设置 `FOODMAP_TOKEN_SECRET`：生产环境使用公开默认密钥，任何人都可以伪造任意 userId 的 Access Token。
- 如果只在网关设置 `FOODMAP_TOKEN_SECRET`：认证服务仍用默认密钥签发，网关校验全部失败，登录链路直接不可用。
- 即使设置了网关变量，`JWT_SECRET` 也不是代码读取的变量，运维按 env.example 配置会踩坑。
- 密码 pepper 公开后不提供额外安全性，等于没有 pepper。

**建议**

1. 认证服务和网关统一使用 `foodmap.security.token-secret`，通过环境变量/密钥系统注入，**生产 profile 缺失时启动失败**，不要保留默认值。
2. `deploy/env.example` 改为 `FOODMAP_TOKEN_SECRET`，并补充 pepper 变量。
3. 迁移到标准 JWT 库（Nimbus JOSE + JWT 或 Spring Security 的 JWT 支持），补齐 `iss`、`aud`、`jti`、`iat`，并校验签发方和受众。
4. 密码哈希迁移到 Argon2id 或 bcrypt；如果保留 PBKDF2，至少把迭代次数提升到当前 OWASP 建议值，并移除硬编码 pepper 默认值。

---

### P0-2 Access Token 可通过追加后缀绕过登出拒绝名单

**现象**

`HmacTokenCodec.parse` 只要求 `parts.length >= 3`，签名只校验 `parts[0] + "." + parts[1]`，第 4 段及之后被直接忽略。

**证据**

- [HmacTokenCodec.java](after/foodmap-common/src/main/java/com/foodmap/common/security/HmacTokenCodec.java#L161-L167)

本地验证结果：

```text
original hash: GPq_KpWdCu4qUnDu3Oa42AMvsrmdxlPpNeJ_au_EeWw
tampered hash: vmteQuzi4IRQLh78yRYcuOEeQeTPuQlKCUQax4xTHuQ
tampered parsed userId: 200001
```

**影响**

- 用户登出后，原 Access Token 会被写入 Redis 拒绝名单，key 是原 Token 的哈希。
- 攻击者只要在 Token 末尾追加 `.anything`，签名仍然通过，哈希变成另一个值，拒绝名单查不到，Token 在剩余有效期内继续可用。
- 这是“登出失效”语义的直接绕过，属于认证安全问题，不是代码风格问题。

**建议**

1. Access Token 必须严格校验 `parts.length == 3`；Refresh Token 严格校验 `parts.length == 4`。
2. 更推荐使用标准 JWT，并在 Token 中加入 `jti`，拒绝名单按 `jti` 而不是整串 Token 哈希存储。
3. 增加回归测试：对“原 Token、追加后缀、篡改 payload、篡改签名、错误类型”逐项断言。

---

### P0-3 内部接口与后台权限模型可被伪造

**现象 A：内部接口没有服务身份认证**

- `/internal/users/provision` 没有任何认证，调用方可以传任意 `userId`、`accountId`、`nickname` 创建用户。
- `/internal/logs/api-access` 没有任何认证，调用方可以读取全部接口访问摘要。
- 网关虽然拦截了外部 `/internal/**`，但服务之间是扁平 Docker 网络，任何内部调用方都可以绕过网关直连服务。

**证据**

- [InternalUserProvisionController.java](after/foodmap-user-service/src/main/java/com/foodmap/user/controller/InternalUserProvisionController.java#L34)
- [LogApiAccessLogController.java](after/foodmap-log-service/src/main/java/com/foodmap/log/controller/LogApiAccessLogController.java#L45)

**现象 B：后台权限完全信任客户端请求头**

- `AdminPermissionGuard` 从 `X-Admin-User-Id` 和 `X-Admin-Permissions` 读取身份与权限。
- 网关没有 admin 路由，也没有移除 `X-Admin-*` 请求头。
- 只要 admin 服务被任何路径触达，调用方自带 `X-Admin-Permissions: LOG_ACCESS_READ` 就能通过校验。

**证据**

- [AdminPermissionGuard.java](after/foodmap-admin-service/src/main/java/com/foodmap/admin/security/AdminPermissionGuard.java#L22-L32)
- [AdminAuthHeaders.java](after/foodmap-admin-service/src/main/java/com/foodmap/admin/security/AdminAuthHeaders.java#L14)

**现象 C：网关 discovery locator 打开，存在路径前缀绕过风险**

- `spring.cloud.gateway.discovery.locator.enabled: true` 会为每个 Nacos 服务自动生成 `/{serviceId}/**` 路由。
- `GatewayAuthFilter` 只对原始路径以 `/api/` 开头的请求做认证，并且只拦截原始路径以 `/internal/` 开头的请求。
- 访问 `/foodmap-log-service/internal/logs/api-access` 时，原始路径既不是 `/api/` 也不是 `/internal/`，过滤器会放行；discovery locator 再把路径重写为 `/internal/logs/api-access` 转发到 log-service。
- 当前 nginx 只代理 `/api/`，外部暂时打不到这个路径；但如果网关被内部调用方、错误配置的 LB 或未来的直连入口访问，就会暴露内部接口。

**证据**

- [application.yml（gateway）](after/foodmap-gateway-service/src/main/resources/application.yml#L27-L31)
- [GatewayAuthFilter.java](after/foodmap-gateway-service/src/main/java/com/foodmap/gateway/filter/GatewayAuthFilter.java#L88-L95)

**影响**

- 内部接口可以创建用户、读取日志，横向移动风险高。
- 后台权限可以被客户端直接声明，等于没有 RBAC。
- discovery locator 扩大了攻击面，且与“显式路由”设计原则冲突。

**建议**

1. 关闭 `spring.cloud.gateway.discovery.locator.enabled`，只保留显式路由。
2. 为内部调用增加服务身份：短期可用内部签名头（服务名 + 时间戳 + HMAC + nonce），长期用 mTLS 或服务网格；至少不能只靠“网关挡外部”。
3. 内部接口增加调用方白名单和最小权限校验；`/internal/users/provision` 只允许 auth-service 调用。
4. 后台权限必须从服务端会话/数据库读取，不能接受客户端传入的权限码；网关需要移除所有 `X-Admin-*` 头。
5. admin 服务要么走网关显式路由，要么明确只在内网暴露并加服务身份认证。

---

### P0-4 生产链路没有 TLS、没有镜像、应用服务没有编排

**现象**

- `deploy/nginx/foodmap.conf` 只有 `listen 80`，没有 443、证书和 HSTS。
- `docker-compose.ecs2.yml` 只定义了 gateway、nacos、postgres、redis、rabbitmq，auth/user 等应用服务没有加入，注释明确写着 placeholder。
- 全仓库没有 Dockerfile，`foodmap/foodmap-gateway-service:latest` 无法从仓库构建。
- `deploy/env.example` 缺少 `ADMIN_DB_USERNAME`、`ADMIN_DB_PASSWORD`、`ADMIN_LOG_SERVICE_BASE_URL`、`FOODMAP_TOKEN_SECRET` 等变量；admin 的 prod profile 会因缺少 `ADMIN_DB_USERNAME` 启动失败，admin 调用 log-service 的默认地址是 `127.0.0.1:8089`，在容器内不可达。

**证据**

- [foodmap.conf](deploy/nginx/foodmap.conf#L1-L3)
- [docker-compose.ecs2.yml](deploy/docker-compose.ecs2.yml#L96-L97)
- [application-prod.yml（admin）](after/foodmap-admin-service/src/main/resources/application-prod.yml#L1-L5)
- [env.example](deploy/env.example#L40)

**影响**

- 登录、注册、Token 全部走明文 HTTP，移动端凭证可被中间人截获。
- 生产 compose 起不来完整业务链路，当前只能算“基础设施编排模板”。
- 环境变量模板不完整，部署时会连续踩启动失败。

**建议**

1. 为每个服务补多阶段 Dockerfile，镜像打 Git commit/版本标签。
2. `docker-compose.ecs2.yml` 补齐 auth、user、admin、log 等服务，并配置 healthcheck、依赖顺序、资源限制。
3. nginx 补 443、证书挂载、HTTP 跳转 HTTPS、HSTS、安全响应头；生产禁止 80 明文承载认证流量。
4. 按服务整理 env 清单，并增加启动期配置校验（缺关键变量直接 fail fast）。
5. 增加一套 `docker compose config` + 启动 smoke test，作为部署前门禁。

---

### P0-5 日志平台没有闭环，当前链路无法产出设计中的数据

**现象**

- 所有服务都没有 `logback-spring.xml`，也没有 `logging.file.name`，日志只输出到控制台。
- Fluent Bit  tails 的是 `/foodmap/logs/*.log` 共享卷，但这个卷只挂载给 Fluent Bit，没有挂载给任何应用服务。
- `docker-compose.dev.yml` 没有应用服务，生产 compose 也没有应用服务，所以没有应用日志写入该卷。
- Fluent Bit 的 parser 配置为 `Format json`，但 `SafeLog` 输出的是 `eventName key=value` 纯文本，不是 JSON。
- `audit` / `security` topic 有路由规则，但代码里没有看到审计事件或安全事件的生产者。

**证据**

- [ApiAccessLogFilter.java](after/foodmap-common/src/main/java/com/foodmap/common/logging/ApiAccessLogFilter.java#L70-L82)
- [SafeLog.java](after/foodmap-common/src/main/java/com/foodmap/common/logging/SafeLog.java#L23-L35)
- [fluent-bit.conf](deploy/logging/fluent-bit/fluent-bit.conf#L1-L40)
- [parsers.conf](deploy/logging/fluent-bit/parsers.conf#L1-L5)
- [docker-compose.dev.yml](deploy/docker-compose.dev.yml#L151-L158)

**影响**

- 花了大量代码实现的 Kafka → Logstash → Elasticsearch → 日志 PostgreSQL → 归档链路，实际没有数据源。
- 后台日志查询、15 天保留、OSS 归档都只能验证“代码能跑”，不能验证“生产有数据”。
- 排障时会发现 requestId 在日志里，但日志根本没有进入采集平台。

**建议**

1. 增加统一的 `logback-spring.xml`，配置 JSON encoder + 文件 appender，输出到 `/foodmap/logs/{service}.log`。
2. 所有应用服务挂载 `foodmap-app-logs` 卷；Fluent Bit 用 JSON parser，字段与 `SafeLog` 对齐。
3. 在 compose 中加一个最小 smoke 场景：启动 auth + gateway，调用一次登录，断言 Kafka 有消息、ES 能按 requestId 查到、日志 PostgreSQL 有摘要。
4. 审计日志要明确事件模型和生产者，否则先不要把 `audit` / `security` 标为“已完成”。

---

### P1-1 分层依赖方向被破坏，领域层空心化

**现象**

- `application/port` 接口直接返回 `infrastructure.persistence.entity` 实体。
- `service/impl` 直接 import 持久化实体并做字段赋值、状态判断、DTO 转换。
- `domain` 层只有枚举和两个工具类（Token 签发、密码哈希），没有 `Account`、`User` 等领域对象；登录状态流转、账号状态校验、身份绑定规则都在 743 行的 `AuthServiceImpl` 里。
- `Pbkdf2PasswordHashService`、`HmacTokenIssuer` 在 domain 包里直接依赖 Spring `@Component`。

**证据**

- [AuthAccountRepository.java](after/foodmap-auth-service/src/main/java/com/foodmap/auth/application/port/AuthAccountRepository.java#L3)
- [UserRepository.java](after/foodmap-user-service/src/main/java/com/foodmap/user/application/port/UserRepository.java#L3-L5)
- [AuthServiceImpl.java](after/foodmap-auth-service/src/main/java/com/foodmap/auth/service/impl/AuthServiceImpl.java#L31-L36)
- [AuthServiceImpl.java](after/foodmap-auth-service/src/main/java/com/foodmap/auth/service/impl/AuthServiceImpl.java#L65)

**影响**

- 应用层和领域层无法脱离 MyBatis/数据库实体独立演进，基础设施变更会穿透到用例层。
- 领域规则散落在 ServiceImpl，单元测试只能测“编排结果”，很难测“业务规则本身”。
- 与 CODEX-after.md 自己写的 `controller -> application -> domain -> infrastructure` 依赖方向不一致。

**建议**

1. 端口返回领域对象或应用层 DTO，不返回持久化实体；在 infrastructure 内做 Entity ↔ Domain 转换。
2. 为 auth 建立 `Account`、`Credential`、`Session` 等领域对象，把状态流转、登录校验、身份绑定规则收进 domain。
3. 把 Spring 注解从 domain 移出，用 `@Configuration` 显式装配；domain 保持纯 Java。
4. 用 ArchUnit 固化分层规则：`domain` 不依赖 `infrastructure`，`application` 不依赖 `persistence.entity`，`controller` 不依赖 `mapper`。

---

### P1-2 跨服务写流程没有 Saga/Outbox，注册在数据库事务内同步调用用户服务

**现象**

`AuthServiceImpl.register` 标注 `@Transactional`，在同一个事务内完成账号、凭证、身份绑定写入后，调用 `userProfileProvisionClient.provision(...)`（Feign 调用用户服务），然后才提交事务。

**证据**

- [AuthServiceImpl.java](after/foodmap-auth-service/src/main/java/com/foodmap/auth/service/impl/AuthServiceImpl.java#L178-L210)
- [FeignUserProfileProvisionClient.java](after/foodmap-auth-service/src/main/java/com/foodmap/auth/infrastructure/client/user/FeignUserProfileProvisionClient.java#L35-L52)

**影响**

- 事务持有数据库连接期间等待跨服务调用（连接 3s、读取 5s），违反 CODEX-after.md 自己写的“不能在持有数据库连接或事务时等待跨服务慢接口”。
- 用户服务已经提交、认证服务回滚时，会留下孤儿用户；没有补偿任务、失败状态和人工处理入口。
- 用户服务重复开通返回 409，认证服务没有幂等键，重试会把可恢复失败变成 502。
- 用户服务不可用时，注册整体不可用，可用性耦合。

**建议**

1. 注册用例改为“本地事务写认证库 + Outbox 记录待开通事件”，事务提交后异步发事件；或先创建 `PENDING` 用户，再由补偿任务重试。
2. 用户开通接口支持幂等键（`userId` 或 `Idempotency-Key`），重复请求返回同一结果。
3. 明确失败状态机：`PENDING`、`PROVISIONING`、`ACTIVE`、`FAILED`、`COMPENSATING`、`COMPENSATED`。
4. 增加补偿定时任务和人工处理入口；至少要有失败告警。

---

### P1-3 可观测性只有日志，没有指标、链路追踪和就绪探针

**现象**

- 所有服务的 `management.endpoints.web.exposure.include` 只有 `health,info`。
- 没有 `micrometer-registry-prometheus`，没有 Prometheus/Grafana 配置，没有自定义业务指标。
- 没有 OpenTelemetry/Sleuth/Micrometer Tracing 依赖，只有自定义的 `requestId` / `traceId` 字符串。
- 没有 readiness/liveness 分组，没有 HikariCP、Redis pool、线程池指标暴露。
- 没有告警规则、看板、SLO。

**证据**

- [application.yml（auth）](after/foodmap-auth-service/src/main/resources/application.yml#L44-L48)
- [pom.xml（auth）](after/foodmap-auth-service/pom.xml#L1-L100)（无 Prometheus/Tracing 依赖）
- [GatewayTraceFilter.java](after/foodmap-gateway-service/src/main/java/com/foodmap/gateway/filter/GatewayTraceFilter.java#L1-L40)

**影响**

- 线上只能看日志，无法回答“QPS、P99、错误率、连接池是否耗尽、Redis 是否超时”。
- 没有真正的 span，跨服务调用只能靠自定义 traceId 字符串串联，无法看到调用耗时分解。
- 这与 CODEX-after.md 的“每个服务必须暴露并关注 HikariCP、Redis pool 和请求线程池指标”不一致。

**建议**

1. 每个服务加 `micrometer-registry-prometheus`，暴露 `/actuator/prometheus`，只在内部网络可访问。
2. 加 `management.endpoint.health.probes.enabled=true`，区分 liveness/readiness。
3. 引入 Micrometer Tracing + OpenTelemetry bridge，至少采集 HTTP、Feign、JDBC、Redis 的 span。
4. 定义最小告警集：5xx 率、P99、Hikari pending、Redis timeout、登录失败率、Kafka lag。

---

### P1-4 API 契约与文档漂移，缺少 OpenAPI 和版本策略

**现象**

- 技术栈文档写“OpenAPI + Knife4j”，但仓库没有 springdoc/knife4j 依赖，也没有 OpenAPI 文件。
- 文档写“Spring Security + JWT”“Refresh Token 轮换”，代码实际是自定义 HMAC Token，且明确注释“MVP 不轮换 Refresh Token”。
- `AuthController.refresh` 的 Javadoc 写“客户端必须保存响应中轮换后的 Refresh Token”，实现却返回原 Refresh Token。
- `docs/api/auth-user.md` 写 `/internal/users/bootstrap`、`/internal/users/{userId}/login-state`，代码只有 `/internal/users/provision`。
- API 路径没有版本号（如 `/api/v1`），后续破坏性变更没有演进空间。
- 网关对 404、405、下游 5xx、429 没有统一 `ApiResponse` 包装，和业务服务错误结构不一致。

**证据**

- [AuthController.java](after/foodmap-auth-service/src/main/java/com/foodmap/auth/controller/AuthController.java#L60-L75)
- [AuthServiceImpl.java](after/foodmap-auth-service/src/main/java/com/foodmap/auth/service/impl/AuthServiceImpl.java#L260-L300)
- [auth-user.md](docs/api/auth-user.md#L1-L5)
- [pom.xml（auth）](after/foodmap-auth-service/pom.xml)（无 OpenAPI 依赖）

**影响**

- 前端/联调方无法从代码生成稳定客户端，只能手抄 Markdown。
- 文档与实现不一致，会直接误导联调和验收。
- 网关错误结构不统一，客户端需要写两套错误处理。

**建议**

1. 引入 springdoc-openapi 或 Knife4j，至少让 DTO、错误码、鉴权要求可从代码生成。
2. 建立 `/api/v1` 版本策略，网关按版本路由。
3. 用契约测试（Spring Cloud Contract 或 OpenAPI diff）保证文档与实现一致。
4. 网关补统一错误响应，覆盖 404、405、429、502/503/504。
5. 立即修正 `refresh` 的 Javadoc 和 `docs/api` 中未实现的接口描述。

---

### P1-5 安全基线缺口：没有 Spring Security、限流、CORS、刷新轮换

**现象**

- 没有 `spring-boot-starter-security`，认证完全靠自定义 Filter + HMAC Token。
- 没有登录失败计数、账号锁定、IP 限流、验证码；`CommonErrorCode.TOO_MANY_REQUESTS` 只定义了错误码，没有实现。
- 没有 CORS 配置；网关文档要求支持 CORS。
- Refresh Token 不轮换，30 天有效；没有 reuse detection。
- 密码策略只有长度 8-128，没有复杂度/弱密码/泄露库校验。
- `application.yml` 的 Kafka 消费者配置 `spring.json.trusted.packages: "*"`，反序列化信任范围过宽。
- `FoodMapAccessTokenDenylistAutoConfiguration` 在 Redis 关闭时静默回退到 `NoopAccessTokenDenylistClient`，生产环境会 fail-open。
- 手机号/邮箱在 `auth_accounts` 仍是明文，文档却写“保存哈希和必要密文”。

**证据**

- [pom.xml（auth）](after/foodmap-auth-service/pom.xml)（无 Spring Security）
- [application.yml（log）](after/foodmap-log-service/src/main/resources/application.yml#L34)
- [FoodMapAccessTokenDenylistAutoConfiguration.java](after/foodmap-common/src/main/java/com/foodmap/common/security/FoodMapAccessTokenDenylistAutoConfiguration.java#L14-L25)
- [V1__create_auth_tables.sql](after/foodmap-auth-service/src/main/resources/db/migration/V1__create_auth_tables.sql#L21-L27)

**影响**

- 登录接口可被暴力破解；没有限流时，单机可承受的撞库成本很低。
- Refresh Token 长期不轮换，泄露后 30 天内都可续签。
- Redis 配置错误时，登出拒绝名单静默失效，属于安全 fail-open。
- 明文手机号/邮箱扩大数据泄露影响面。

**建议**

1. 登录/注册/刷新接口加 IP + 账号双维度限流，失败计数和临时锁定。
2. Refresh Token 轮换 + reuse detection；或至少缩短有效期并支持按会话撤销。
3. 密码策略升级，接入弱密码库；哈希升级到 Argon2id/bcrypt。
4. 生产环境禁止 Noop denylist，Redis 不可用时启动失败或降级为“拒绝所有登出后请求”。
5. CORS 按环境白名单配置；Kafka `trusted.packages` 收紧到具体 DTO 包。
6. 手机号/邮箱按文档做加密或至少做字段级访问控制。

---

### P1-6 测试体系以单元测试为主，没有真实集成测试和 CI 门禁

**现象**

- 52 个测试文件、118 个 `@Test`，全部是普通 JUnit 单元测试。
- 没有 `@SpringBootTest`、`@Testcontainers`、真实 PostgreSQL、Redis、Kafka 的集成测试。
- Flyway 迁移、Mapper XML、唯一索引、事务回滚、Kafka 幂等这些最容易出问题的部分没有自动化验证。
- 没有 CI 配置文件（`.github/workflows`、Jenkinsfile、GitLab CI 都没有）。
- 没有覆盖率门禁、没有静态分析（SpotBugs/Checkstyle/PMD/Spotless）、没有依赖漏洞扫描。

**证据**

- `after/**/src/test/**`（无 Testcontainers 依赖）
- `after/pom.xml`（无 JaCoCo/Surefire/Failsafe/Enforcer/Checkstyle/SpotBugs）
- 仓库根目录无 `.github/`

**影响**

- “mvn test 通过”不能证明数据库 schema、SQL、事务、幂等是对的。
- 每次改 Mapper/Flyway 都可能把问题带到联调环境。
- 没有 CI，质量门禁依赖开发者自觉；harness 也只是本地脚本。

**建议**

1. 引入 Testcontainers PostgreSQL，至少覆盖 auth/user 的 Flyway + Repository + Service 事务。
2. 为网关、auth、user 加 `@SpringBootTest` + MockMvc/WebTestClient 的接口级测试。
3. 为 Kafka 消费幂等、Redis denylist、Token 过期/撤销加集成测试。
4. 加 CI：`mvn verify`、依赖漏洞扫描、镜像构建、harness 检查、覆盖率报告。
5. 用 JaCoCo 设最低覆盖率门槛（先 60%，逐步提高），但不要只看行覆盖率，核心安全用例必须显式覆盖。

---

### P1-7 构建与供应链基础薄弱

**现象**

- 没有 Maven Wrapper（`mvnw`），构建依赖本机 Maven 版本。
- 父 POM 没有 `maven-enforcer-plugin`，无法强制 Java 21、Maven 版本、依赖收敛。
- 没有 `dependency-check` / SCA / SBOM 生成。
- 没有 Dockerfile，也没有镜像扫描。
- 插件版本只固定了 compiler 和 spring-boot，Surefire/Failsafe/Jar/Resources 依赖 Maven 默认版本。
- `foodmap-common` 把 Redisson 作为非 optional 依赖，导致 gateway、user、admin、log 都被动引入 Netty/Redisson，增加启动时间和攻击面。

**证据**

- [pom.xml（after）](after/pom.xml#L1-L90)
- [pom.xml（common）](after/foodmap-common/pom.xml#L1-L90)

**建议**

1. 加 `mvnw`，统一 Maven 版本。
2. 加 enforcer：Java 21、Maven 3.9+、禁止 SNAPSHOT 依赖进入生产。
3. 加 OWASP dependency-check 或 Trivy/Grype，CI 中阻断高危漏洞。
4. 生成 SBOM（CycloneDX），记录每次发布依赖。
5. 把 Redisson 改为 optional，只有真正需要 Redis 的服务显式引入。

---

### P2-1 数据访问层细节问题

**现象**

- `UserRepositoryImpl.save` 采用“先 update，未命中再 insert”的 upsert，没有捕获唯一键冲突；并发下可能抛 `DuplicateKeyException`，最终返回 500 而不是 409。
- `AuthServiceImpl.ensureLoginIdentifierAvailable` 是 check-then-act，唯一索引能兜底，但异常没有映射为业务错误。
- 没有乐观锁/版本字段，用户资料、账号状态并发更新是 last-write-wins。
- log-service 的 `api_access_log`、`log_archive_records` 表结构放在 `deploy/logging/postgres/migration`，log-service 自己没有 Flyway 依赖和 `db/migration`，不满足“每个服务管理自己的迁移”。
- `findByLoginIdentifier` 用 `lower(account_name) = ? or phone = ? or lower(email) = ? limit 1`，跨列匹配且无排序；如果账号名与别的用户手机号/邮箱相同，结果不确定。
- `selectListByCondition` 没有 limit，当前只用于取一条，但接口本身容易被误用为全表查询。
- `LIKE concat('%', #{...}, '%')` 没有转义 `%`、`_`，用户输入会变成通配符。

**证据**

- [UserRepositoryImpl.java](after/foodmap-user-service/src/main/java/com/foodmap/user/infrastructure/persistence/mybatis/UserRepositoryImpl.java#L36-L44)
- [AuthAccountDefineMapper.xml](after/foodmap-auth-service/src/main/resources/mapper/auth/AuthAccountDefineMapper.xml#L21-L29)
- [application-local.yml（log）](after/foodmap-log-service/src/main/resources/application-local.yml)（无 Flyway）
- [V1__create_api_access_log.sql](deploy/logging/postgres/migration/V1__create_api_access_log.sql)

**建议**

1. 用数据库唯一索引 + `ON CONFLICT` 或捕获 `DuplicateKeyException` 做幂等 upsert，并映射为 409。
2. 对可变聚合加 `version` 或 `updated_time` 乐观锁。
3. log-service 补 Flyway，迁移脚本放回模块内；deploy 只负责执行，不拥有 schema。
4. 登录标识解析拆成明确类型（accountName/phone/email），避免跨列 `OR`；账号名限制不能与手机号/邮箱格式冲突。
5. 所有列表查询显式 limit；LIKE 查询转义通配符。

---

### P2-2 异常映射不完整，部分场景会返回 500 而不是 400/401/415

**现象**

`FoodMapGlobalExceptionHandler` 覆盖了业务异常、Bean Validation、JSON 解析、缺参、类型不匹配、405、404、ResponseStatus、IllegalArgument、兜底 Exception，但没有覆盖：

- `MissingRequestHeaderException`：`UserController.me` 缺少 `X-FoodMap-User-Id` 时会落到 `Exception` 兜底，返回 500。
- `HttpMediaTypeNotSupportedException`：Content-Type 不支持时可能返回 500 而不是 415。
- `MissingPathVariableException`、`HandlerMethodValidationException` 等 Spring 6 异常。

**证据**

- [FoodMapGlobalExceptionHandler.java](after/foodmap-common/src/main/java/com/foodmap/common/web/FoodMapGlobalExceptionHandler.java)
- [UserController.java](after/foodmap-user-service/src/main/java/com/foodmap/user/controller/UserController.java#L31)

**影响**

- API 错误语义不稳定，客户端无法区分“没登录”和“服务端错误”。
- 监控上会把客户端错误统计成 5xx，污染错误率。

**建议**

1. 继承 `ResponseEntityExceptionHandler` 或补齐常见 Spring MVC 异常映射。
2. 为缺 header 统一返回 401，为 Content-Type/方法/参数错误返回 4xx。
3. 增加 MockMvc 契约测试，覆盖所有错误分支。

---

### P2-3 日志隐私与审计事件缺口

**现象**

- `ApiAccessLogFilter` 把完整 `clientIp` 写入日志字段。
- `ApiAccessLogEventParser` 直接把这个值写入 `client_ip_masked` 字段，没有做掩码。
- 文档要求“关键业务动作必须记录审计日志”，但代码里没有审计事件生产者；Fluent Bit 的 `audit` / `security` 路由没有对应日志。
- 登录日志没有采集 IP/User-Agent（列存在，但服务层没有请求上下文）。

**证据**

- [ApiAccessLogFilter.java](after/foodmap-common/src/main/java/com/foodmap/common/logging/ApiAccessLogFilter.java#L77)
- [ApiAccessLogEventParser.java](after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/messaging/ApiAccessLogEventParser.java#L56)

**建议**

1. `clientIp` 在进入日志前做掩码（如保留前两段），字段名和实际语义一致。
2. 定义审计事件模型（actor、action、resource、result、requestId），通过统一 `AuditLog` 接口输出。
3. 登录日志补 IP/User-Agent，注意脱敏和保留周期。

---

### P2-4 编码规范执行存在漏洞，God Service 和遗留代码较多

**现象**

- `AuthServiceImpl` 743 行，承担注册、登录、刷新、登出、身份绑定、会话、登录日志、Token 拒绝名单等职责。
- 11 个 `@Deprecated` 元素，auth 同时维护旧 `accountId + userId` 和新 `userId` 模型，双写逻辑长期留在生产代码。
- `UserController.me` 的 Javadoc 没有 `@param` / `@return`，但 `validate-backend.sh` 仍输出 PASS；Javadoc 扫描是 warning 级别，且正则不匹配多行方法签名。
- `Check.notBlank` 对密码做 trim，注册和登录都会静默去掉首尾空格；虽然两端一致，但不符合“密码不做规范化”的常见安全实践。
- 打包结构不统一：auth 有 `service/impl` + `application/port`，其他服务多用 `service` + `application/port`；`mapper` 放在 `infrastructure/persistence/mapper`，与文档建议的顶层 `mapper` 不一致。

**证据**

- [AuthServiceImpl.java](after/foodmap-auth-service/src/main/java/com/foodmap/auth/service/impl/AuthServiceImpl.java#L65)
- [validate-backend.sh](harness/scripts/validate-backend.sh#L179-L180)
- [UserController.java](after/foodmap-user-service/src/main/java/com/foodmap/user/controller/UserController.java#L26-L33)
- [Check.java](after/foodmap-common/src/main/java/com/foodmap/common/validation/Check.java#L20-L27)

**建议**

1. 按用例拆分 `AuthServiceImpl`：`RegisterUseCase`、`LoginUseCase`、`SessionUseCase`、`IdentityBindingUseCase`。
2. 给迁移期代码设定退出日期，用 TODO/issue 跟踪，不允许无限期保留 deprecated 双写。
3. 用 Spotless/Checkstyle 做机械规范，用 ArchUnit 做分层约束；harness 只做补充，不做唯一门禁。
4. 密码字段不要 trim；只对登录标识做标准化。
5. 统一包结构，至少在 CODEX-after.md 和实际代码中二选一。

---

### P2-5 核心业务服务还是空壳，最关键的可见范围规则没有验证

**现象**

- relation、store、recommendation、community、media 五个服务各约 33 行，只有启动类和 health。
- 推荐可见范围（PRIVATE / SPECIFIC_USERS / FRIENDS / COUPLE / GROUP / PUBLIC）、评论权限、社区 PUBLIC 统计这些核心规则没有任何实现或测试。
- 当前架构只在 auth/user 链路上被验证，跨服务关系校验、事件最终一致性、PostGIS 查询、媒体上传都没有真实代码支撑。

**影响**

- 现在无法判断“微服务拆分是否合理”“关系服务内部接口是否够用”“推荐服务查询会不会变成 N+1 跨服务调用”。
- 如果等到核心域开发时才补安全和一致性基础设施，返工面会很大。

**建议**

1. 先做一条最小纵向切片：门店创建 → 推荐创建（PRIVATE/PUBLIC）→ 好友可见 → 社区统计，端到端跑通。
2. 把可见范围做成独立 `VisibilityPolicy`，输入 `viewerId + ownerId + visibilityType + relation facts`，输出 allow/deny；用表驱动测试覆盖所有组合。
3. 关系校验要有缓存、超时、fail-closed 策略；关系服务不可用时不能默认放行。
4. 在核心切片中验证 Outbox、事件幂等、社区统计只收 PUBLIC。

---

## 5. 根因分析

这些问题不是零散的，背后有三条主线：

### 5.1 “文档先行”跑得比“工程闭环”快

CODEX 文档把企业级能力写得很完整，代码也按文档建了骨架，但很多能力停在“有接口、有类、有配置项”：

- 有 Token 配置项，但签发端没接；
- 有日志平台，但没有日志落盘；
- 有审计 topic，但没有审计事件；
- 有 Docker 编排，但没有镜像和应用服务；
- 有 harness，但只跑 `mvn validate`。

这会导致一种危险状态：**看起来什么都有，实际关键链路没有闭合**。企业级评审最怕这种“假完成”。

### 5.2 共享 `foodmap-common` 过大，服务边界在代码层被弱化

`foodmap-common` 同时包含 API 契约、异常、日志、Redis、Redisson、对象存储、Elasticsearch、Feign、安全、持久化基类、领域枚举。它已经不是一个“薄公共库”，而是“平台 SDK + 领域词典 + 基础设施全家桶”。

后果：

- 任何 common 变更都可能要求所有服务重新发布；
- 服务之间通过共享类产生隐式耦合；
- Redisson 等重依赖被所有服务被动引入；
- 领域枚举跨服务共享，后续容易出现语义漂移。

建议拆分为 `common-api`（响应/错误码/日志契约）、`common-infra`（Redis/存储/搜索/Feign）、`common-test`（测试替身），领域枚举回到各自服务或独立的 `domain-contract`。

### 5.3 安全模型是“网关单点信任 + 明文头”，没有服务身份

当前设计假设：

- 外部只能经过网关；
- 网关会移除伪造头；
- 内部网络是可信的。

这三个假设在生产环境都不够稳：

- 网关可能被绕过（discovery locator、直连、错误 LB）；
- 内部网络可能有被攻陷的容器；
- 后台权限头由客户端提供。

企业级做法是“零信任内部网络”：服务间调用要有身份（mTLS / 服务 token / 签名头），每个服务自己校验权限，不能只依赖网关。

---

## 6. 整改路线

### 6.1 第 0-2 周：P0，先让系统“敢上线”

| 优先级 | 事项 | 验收标准 |
| --- | --- | --- |
| P0 | 统一 Token 密钥注入，生产缺失即启动失败 | auth 与 gateway 使用同一变量；无默认密钥；集成测试覆盖签发/校验 |
| P0 | 修复 Token 后缀绕过 | Access Token 严格 3 段；登出后追加后缀也返回 401；有回归测试 |
| P0 | 关闭 discovery locator | 只保留显式路由；`/foodmap-*-service/**` 不可达 |
| P0 | 内部接口服务身份认证 | `/internal/**` 无有效服务身份返回 401/403；有测试 |
| P0 | 后台 RBAC 服务端化 | 权限来自服务端，`X-Admin-Permissions` 被网关移除或忽略 |
| P0 | HTTPS + Dockerfile + 应用编排 | 生产 compose 可启动完整链路；80 强制跳 443 |
| P0 | 日志落盘与采集闭环 | 登录一次可在 ES 按 requestId 查到日志，日志 PostgreSQL 有摘要 |

### 6.2 第 2-6 周：P1，补齐工程能力

| 事项 | 验收标准 |
| --- | --- |
| 分层重构 + ArchUnit | domain 不依赖 infrastructure；端口不返回 Entity；CI 中 ArchUnit 失败即阻断 |
| 注册流程 Saga/Outbox | 用户服务不可用时注册不产生孤儿数据；有补偿任务和失败状态 |
| 可观测性 | Prometheus 指标、OTel trace、readiness/liveness、最小告警集 |
| OpenAPI + 契约测试 | 接口文档从代码生成；文档与实现 diff 为 0 |
| 安全加固 | 登录限流、Refresh 轮换、密码策略、CORS、Kafka trusted packages 收紧 |
| 测试体系 | Testcontainers 覆盖 Flyway/Mapper/事务；CI 跑 `mvn verify` |
| 构建供应链 | mvnw、enforcer、SCA、SBOM、镜像扫描 |

### 6.3 第 6-12 周：P2 + 核心域

| 事项 | 验收标准 |
| --- | --- |
| 拆分 AuthServiceImpl | 单类不超过 300 行；用例边界清晰 |
| 统一包结构 | CODEX-after.md 与代码一致 |
| 数据访问加固 | upsert 幂等、乐观锁、LIKE 转义、列表强制 limit |
| 异常映射补全 | 缺 header/415/参数错误均返回正确 4xx |
| 日志隐私 | clientIp 掩码、审计事件落地 |
| 核心纵向切片 | 门店 → 推荐 → 可见范围 → 社区统计端到端通过 |
| 可见范围策略 | 表驱动测试覆盖 6 种可见范围 × 关系组合 |

---

## 7. 企业级就绪检查表

| 检查项 | 状态 | 说明 |
| --- | --- | --- |
| 服务边界与数据库归属 | ✅ | 文档清晰，未发现跨库访问 |
| 统一 API 响应与错误码 | ✅ | `ApiResponse` / `CommonErrorCode` |
| 数据库迁移 | ⚠️ | auth/user/admin 有 Flyway；log 不在模块内 |
| 业务主键与逻辑删除 | ✅ | 设计规范，部分唯一索引使用正确 |
| 认证签发/校验闭环 | ❌ | 密钥未注入，auth/gateway 可能不一致 |
| Token 撤销 | ❌ | 后缀可绕过 denylist |
| 内部服务认证 | ❌ | `/internal/**` 无认证 |
| 后台 RBAC | ❌ | 权限来自客户端头 |
| HTTPS | ❌ | nginx 只有 80 |
| 限流/防爆破 | ❌ | 无实现 |
| 刷新令牌轮换 | ❌ | 明确不轮换 |
| 统一日志与脱敏 | ⚠️ | 日志封装好；clientIp 未脱敏、审计事件缺失 |
| 日志采集闭环 | ❌ | 无文件 appender、无应用编排、JSON 不匹配 |
| 指标/追踪 | ❌ | 无 Prometheus/OTel |
| 集成测试 | ❌ | 0 个真实 DB/中间件测试 |
| CI/CD | ❌ | 无 CI 配置 |
| 镜像与部署 | ❌ | 无 Dockerfile，应用服务未编排 |
| OpenAPI 契约 | ❌ | 无 springdoc/knife4j |
| 文档与代码一致 | ⚠️ | 文档质量高，但存在 JWT/轮换/内部接口漂移 |
| 核心业务实现 | ❌ | 5 个核心服务为空壳 |

---

## 8. 附录：关键文件索引

| 主题 | 文件 |
| --- | --- |
| 认证服务实现 | [AuthServiceImpl.java](after/foodmap-auth-service/src/main/java/com/foodmap/auth/service/impl/AuthServiceImpl.java) |
| Token 编解码 | [HmacTokenCodec.java](after/foodmap-common/src/main/java/com/foodmap/common/security/HmacTokenCodec.java) |
| 网关认证 | [GatewayAuthFilter.java](after/foodmap-gateway-service/src/main/java/com/foodmap/gateway/filter/GatewayAuthFilter.java) |
| 网关配置 | [application.yml](after/foodmap-gateway-service/src/main/resources/application.yml) |
| 后台权限 | [AdminPermissionGuard.java](after/foodmap-admin-service/src/main/java/com/foodmap/admin/security/AdminPermissionGuard.java) |
| 内部接口 | [InternalUserProvisionController.java](after/foodmap-user-service/src/main/java/com/foodmap/user/controller/InternalUserProvisionController.java)、[LogApiAccessLogController.java](after/foodmap-log-service/src/main/java/com/foodmap/log/controller/LogApiAccessLogController.java) |
| 统一异常 | [FoodMapGlobalExceptionHandler.java](after/foodmap-common/src/main/java/com/foodmap/common/web/FoodMapGlobalExceptionHandler.java) |
| 日志封装 | [SafeLog.java](after/foodmap-common/src/main/java/com/foodmap/common/logging/SafeLog.java)、[LogMasker.java](after/foodmap-common/src/main/java/com/foodmap/common/logging/LogMasker.java) |
| 日志采集 | [fluent-bit.conf](deploy/logging/fluent-bit/fluent-bit.conf)、[parsers.conf](deploy/logging/fluent-bit/parsers.conf) |
| 部署 | [docker-compose.ecs2.yml](deploy/docker-compose.ecs2.yml)、[foodmap.conf](deploy/nginx/foodmap.conf) |
| 质量门禁 | [validate-backend.sh](harness/scripts/validate-backend.sh) |

---

## 9. 审查声明

- 本次审查是静态代码审查 + 本地验证，不包含渗透测试、压力测试和真实生产环境验证。
- “未发现”仅代表本次审查范围内未发现，不代表不存在。
- 本报告不修改业务代码，只给出评估和整改建议；具体修复应按 AGENTS.md 的多代理文件边界规则分派。
