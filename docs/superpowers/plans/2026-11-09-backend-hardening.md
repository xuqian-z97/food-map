# FoodMap 后端加固方案与任务拆分

- 制定日期：2026-11-09
- 依据：[2026-11-06-backend-architecture-review.md](docs/reports/2026-11-06-backend-architecture-review.md) + [复核评估](docs/reports/2026-11-06-backend-architecture-review-assessment.md)
- 复核基线：`d9bf502`（2026-11-09 核对，P0 五项**全部仍然成立**）
- 目标口径：把后端从「骨架工程 3.3 分 / 生产就绪 1.4 分」提升到「可承载真实用户」
- 约束：遵守 `AGENTS.md` §4.5 文件边界、§6.3.1 验证纪律、§6.2 台账唯一书写位置

---

## 第一部分：完善方案

### 1. 核心判断

不要试图一次修完所有问题。评估结论里最关键的一句是：

> 安全问题不是「各方面都差一点」，而是**少数几个洞足以致命，其余都还不错**。

所以方案按「**解死结 → 建可验证性 → 补信任模型 → 可部署 → 提质量**」五步推进，每步结束都有可验证的产出，不做大爆炸式重构。

### 2. 五个波次

```text
Wave 0  密钥与 Token 闭环        ← 解开认证死结，1-2 天
   ↓
Wave 1  日志落盘闭环              ← 后续所有排障的地基，2-3 天
   ↓
Wave 2  内部信任与后台 RBAC       ← 零信任改造，3-5 天
   ↓
Wave 3  生产可部署性              ← 镜像/编排/HTTPS/CI，5-7 天
   ↓
Wave 4  质量门禁与结构收口        ← 集成测试/分层/可观测性，持续
```

**为什么是这个顺序**（与原报告顺序的差异）：

| 调整 | 原报告 | 本方案 | 理由 |
| --- | --- | --- | --- |
| 日志从最后提到第 2 位 | P0-5 排第 5 | Wave 1 | 它是**可验证性基础设施**。没有日志落盘，后面三层安全改造出问题只能靠猜，验收也无法取证 |
| 密钥放第 1 位 | P0-1 | Wave 0 | 唯一「结构上无法配置」的问题，改动最小（约 30 行），且是 Docker 编排和集成测试的**前置依赖** |
| 部署拆成 Wave 3 独立推进 | 与安全混在 P0 | Wave 3 | 部署工作量最大但对安全无阻塞；先修代码安全，再打包，避免镜像反复重建 |

### 3. 各波次的方案要点

#### Wave 0：密钥与 Token 闭环

**要解决的结构性问题**（复核确认，比原报告描述更严重）：

`HmacTokenIssuer` 标注 `@Component`，Spring 会选择**无参构造器**，密钥恒为硬编码常量。认证服务源码中 **0 处**引用 `foodmap.security.token-secret`。这意味着：

> 这个代码版本下，无论运维怎么配环境变量，认证服务都用默认密钥签发。只要网关按 env.example 配了 `JWT_SECRET`（而代码根本不读这个变量），登录链路必然断。

**做法**：

1. `HmacTokenIssuer` 改为**构造器注入** `@Value("${foodmap.security.token-secret}")`，**不给默认值**。
2. 新增 `TokenSecretProperties` 配置类，在 `prod` profile 下做启动期校验（长度 ≥ 32），缺失或过短直接 `fail fast`。
3. 网关同步去掉默认值，与 auth 使用**同一变量名**。
4. `deploy/env.example` 把 `JWT_SECRET` 改为 `FOODMAP_TOKEN_SECRET`，补 `AUTH_PEPPER`、`ADMIN_PEPPER`、`ADMIN_DB_USERNAME`、`ADMIN_DB_PASSWORD`。
5. `Pbkdf2PasswordHashService` / `Pbkdf2AdminPasswordHashService` 的 pepper 同样去默认值。

**Token 分段校验**（原报告 P0-2）：

这里有个原报告没写到的**技术陷阱**，必须在实现时注意：

```java
// 现状：Access 和 Refresh 共用 parse()
private TokenClaims parse(String token) {
    if (parts.length < 3) throw ...;   // ← 第 4 段被忽略
```

但 Refresh Token 是 **4 段**（`header.payload.signature + "." + UUID nonce`，见 `issueRefreshToken`）。所以**不能**简单改成 `parts.length == 3`，否则 Refresh Token 全部失效。

正确做法：把校验下沉到类型化入口：

```java
private TokenClaims parse(String token, int expectedParts) { ... }  // 严格 equals
public TokenClaims parseAccessToken(String token)  → expectedParts = 3
public TokenClaims parseRefreshToken(String token) → expectedParts = 4
```

`tokenHash()` 对整串签名，denylist 按整串哈希存储，所以段数严格化后，追加后缀会直接 401，绕过路径关闭。

**测试要求**：现有 `HmacTokenCodecTest.shouldRejectTamperedToken` 只测了篡改签名（`token + "x"`），**没测追加完整段**（`token + ".x"`），这正是漏洞没被发现的原因。必须补 5 个断言：原样通过、追加后缀拒绝、篡改 payload 拒绝、篡改签名拒绝、类型错配拒绝。

#### Wave 1：日志落盘闭环

**为什么这是 Wave 1 而不是 Wave 4**：

现在所有服务的日志只进控制台，Fluent Bit 监听 `/foodmap/logs/*.log` 这个卷，但**没有任何应用服务挂载它**。同时 `parsers.conf` 是 `Format json`，而 `SafeLog` 输出 `eventName key=value` 纯文本——即使有数据也解析不出字段。

结论：**花了 3000+ 行实现的 Kafka→ES→日志库→归档链路，当前生产数据量为 0。**

**做法**：

1. 在 `foodmap-common` 加 `logstash-logback-encoder`，提供统一 `logback-spring.xml` 模板：
   - JSON encoder，`requestId` / `traceId` / `serviceName` / `spanId` 从 MDC 自动带入
   - 文件 appender 输出到 `/foodmap/logs/{service}.log`
   - 保留控制台 appender（本地开发习惯不变）
2. **关键改进**：新增 MDC 字段 `logType`（`application` / `api-access` / `sql` / `audit` / `security`），由 `SafeLog` 的调用点写入。Fluent Bit 改用 `logType` 路由，**不再用正则匹配 message 文本**——现在 `rewrite_tag` 的 `.​*api\.access\..*` 这种正则匹配，一旦事件名改动就静默失效，是不可接受的脆弱设计。
3. 所有应用服务在 compose 中挂载 `foodmap-app-logs:/foodmap/logs` 卷。
4. Fluent Bit parser 保持 `Format json`，此时与 logback JSON 输出天然对齐。
5. 补一个端到端 verify 场景：登录一次 → 断言 ES 能按 requestId 查到 → 断言 `api_access_log` 表有摘要。

**验收必须能取证**，不能只看「代码写了」。

#### Wave 2：内部信任与后台 RBAC

三个独立问题，按改动量从小到大：

1. **关闭 discovery locator**（1 行）。`/foodmap-*-service/**` 自动路由会让 `GatewayAuthFilter` 的路径前缀判断失效，攻击面凭空扩大。只需显式路由。
2. **内部接口服务身份**。`/internal/users/provision` 和 `/internal/logs/api-access` 当前无任何认证，同网段任何容器可调用。短期用签名头：`X-FoodMap-Service` + `X-FoodMap-Timestamp` + `X-FoodMap-Nonce` + HMAC 签名，5 分钟时间窗 + nonce 去重。长期 mTLS。
3. **后台 RBAC 服务端化**。`AdminPermissionGuard` 直接信任 `X-Admin-Permissions` 请求头，网关**完全不处理** `X-Admin-*`。这是「客户端自己声明权限」= 没有 RBAC。必须改为服务端会话/数据库读取，且网关无条件剥除 `X-Admin-*`。

#### Wave 3：生产可部署性

1. **Dockerfile**：多阶段构建（`maven:3.9-eclipse-temurin-21` → `eclipse-temurin:21-jre-alpine`），11 个模块各自一个，或统一用 `--build-arg MODULE=` 复用一份。
2. **compose 补齐**：auth/user/admin/log 加入 `docker-compose.ecs2.yml`，配 `healthcheck`、`depends_on: condition: service_healthy`、资源限制、`restart: unless-stopped`。
3. **HTTPS**：nginx 加 443、证书挂载、80 强制跳转、HSTS、安全响应头。
4. **CI**：`.github/workflows/backend.yml` — `mvn verify` + 依赖漏洞扫描 + 镜像构建 + harness + verify 场景。

**前置依赖**：必须先完成 Wave 0，否则编排出来的 auth 和 gateway 会因密钥不一致而互相签验失败。

#### Wave 4：质量门禁与结构收口

1. **Testcontainers**：至少覆盖 auth/user 的 Flyway 迁移 + Repository + 事务回滚 + 唯一索引冲突。
2. **可观测性**：`micrometer-registry-prometheus` + `/actuator/prometheus` + readiness/liveness 分组 + 最小告警集。
3. **构建供应链**：`mvnw`、`maven-enforcer-plugin`（Java 21 / Maven 3.9+ / 禁 SNAPSHOT）、Redisson 改 optional。
4. **分层重构**：端口不返回持久化实体；`AuthServiceImpl`（743 行）按用例拆分；ArchUnit 固化依赖方向。
5. **API 契约**：springdoc-openapi + `/api/v1` 版本策略。

---

## 第二部分：任务拆分

### 4. 编号与归属

- 任务编号从 **T-025** 起（现有最大 T-024）
- 新功能点从 **F-024** 起（现有最大 F-023）
- **复用现有功能点**，不另立体系：

| 现有功能点 | 状态 | 承接任务 |
| --- | --- | --- |
| F-019 日志采集、查询与归档 | testing | T-028 / T-029 / T-030 |
| F-020 管理后台 | in_progress | T-033 |
| F-021 测试与部署 | in_progress | T-034 ~ T-039 |

新增功能点：F-024 ~ F-030（见 §5 表格）。

> 注意：`status` CLI 只有 `feature set`，**没有 `feature add`**。新增功能点需手写 `status/data/features/F-0xx.yml`，然后跑 `node status/tool/cli.mjs check` 校验。

### 5. 任务总览

| ID | 标题 | 功能点 | 优先级 | 波次 | 预估 | 依赖 |
| --- | --- | --- | --- | --- | --- | --- |
| T-025 | Token 密钥统一注入与 fail-fast | F-024 | P0 | W0 | 0.5d | — |
| T-026 | Access/Refresh Token 严格分段校验 | F-024 | P0 | W0 | 0.5d | — |
| T-027 | 登出必须写拒绝名单 | F-024 | P0 | W0 | 0.5d | T-025 |
| T-028 | 统一 logback JSON 与文件落盘 | F-019 | P0 | W1 | 1.5d | — |
| T-029 | 应用服务挂载日志卷并对齐采集 | F-019 | P0 | W1 | 1d | T-028 |
| T-030 | 日志闭环端到端 verify 场景 | F-019 | P0 | W1 | 1d | T-028, T-029 |
| T-031 | 关闭 gateway discovery locator | F-025 | P0 | W2 | 0.2d | — |
| T-032 | 内部接口服务身份签名 | F-025 | P0 | W2 | 2d | T-031 |
| T-033 | 后台 RBAC 服务端化 | F-020 | P0 | W2 | 2d | T-031 |
| T-034 | 各服务多阶段 Dockerfile | F-021 | P0 | W3 | 1.5d | T-025 |
| T-035 | 生产 compose 补齐应用服务 | F-021 | P0 | W3 | 1.5d | T-034 |
| T-036 | nginx HTTPS / HSTS / 安全头 | F-021 | P0 | W3 | 0.5d | — |
| T-037 | GitHub Actions CI 流水线 | F-021 | P1 | W3 | 1d | T-034 |
| T-038 | Testcontainers 集成测试 | F-021 | P1 | W4 | 3d | T-026 |
| T-039 | mvnw + enforcer + Redisson optional | F-021 | P1 | W4 | 1d | — |
| T-040 | Prometheus 指标与探针 | F-026 | P1 | W4 | 2d | — |
| T-041 | springdoc OpenAPI 与 /api/v1 | F-027 | P1 | W4 | 2d | — |
| T-042 | AuthServiceImpl 按用例拆分 | F-028 | P2 | W4 | 2d | T-038 |
| T-043 | ArchUnit 分层约束 | F-028 | P2 | W4 | 1.5d | — |
| T-044 | 全局异常映射补全 | F-029 | P2 | W4 | 0.5d | — |
| T-045 | 数据访问加固（upsert/锁/LIKE） | F-029 | P2 | W4 | 1.5d | — |
| T-046 | clientIp 掩码与审计事件 | F-029 | P2 | W4 | 1.5d | T-028 |
| T-047 | 注册流程 Outbox 最终一致性 | F-030 | P1 | W4 | 2.5d | T-038 |

**合计约 31.2 人日**。Wave 0 完成后（1.5 天）即可开始核心业务域开发，不必等全部完成。

---

### 6. 任务详细规格

以下规格可直接作为 `harness/agent-task-template.md` 的输入分派给子代理。

---

#### T-025 Token 密钥统一注入与 fail-fast

**背景**：`HmacTokenIssuer` 的 `@Component` 无参构造器硬编码密钥，auth 服务无任何配置项可注入。生产环境无论怎么配都用默认值。

**允许修改**：
```text
after/foodmap-auth-service/src/main/java/com/foodmap/auth/domain/HmacTokenIssuer.java
after/foodmap-auth-service/src/main/java/com/foodmap/auth/domain/Pbkdf2PasswordHashService.java
after/foodmap-auth-service/src/main/java/com/foodmap/auth/config/（新建）
after/foodmap-auth-service/src/main/resources/application.yml
after/foodmap-auth-service/src/main/resources/application-local.yml
after/foodmap-auth-service/src/main/resources/application-orbstack.yml
after/foodmap-auth-service/src/main/resources/application-prod.yml
after/foodmap-gateway-service/src/main/java/com/foodmap/gateway/filter/GatewayAuthFilter.java
after/foodmap-gateway-service/src/main/resources/application.yml
after/foodmap-gateway-service/src/main/resources/application-prod.yml
after/foodmap-admin-service/src/main/java/com/foodmap/admin/service/Pbkdf2AdminPasswordHashService.java
deploy/env.example
```

**禁止修改**：`front/`、其他 after 服务、`CODEX-*.md`（文档同步由 T-025 完成后主代理统一处理）

**交付物**：
1. `TokenSecretProperties`（`@ConfigurationProperties("foodmap.security")`），校验 secret 非空且 ≥ 32 字符
2. `HmacTokenIssuer` 改为构造器注入，**删除无参构造器和硬编码默认值**
3. `Pbkdf2PasswordHashService` / `Pbkdf2AdminPasswordHashService` 的 pepper 去默认值
4. `local` / `orbstack` profile 保留开发默认值（允许，因为不对外）；**`prod` profile 无默认值**
5. `application-prod.yml` 补 `foodmap.security.token-secret: ${FOODMAP_TOKEN_SECRET}`
6. `deploy/env.example`：`JWT_SECRET` → `FOODMAP_TOKEN_SECRET`，补 pepper 与缺失的 `ADMIN_DB_*`

**验收标准**：
- [ ] `prod` profile 缺 `FOODMAP_TOKEN_SECRET` 时启动失败，错误信息明确指出缺失变量
- [ ] secret 短于 32 字符时启动失败
- [ ] auth 与 gateway 引用**同一**变量名，无硬编码默认值残留（`grep -r "foodmap-local-token-secret" after/` 应为 0 命中）
- [ ] `mvn -q -B test` 通过
- [ ] 新增单测：缺失 secret 抛异常、过短 secret 抛异常、合法 secret 正常构造

**验证**：`node tools/verify/cli.mjs run harness/scenarios/`（需本地服务就绪）

---

#### T-026 Access/Refresh Token 严格分段校验

**背景**：`HmacTokenCodec.parse` 只校验 `parts.length >= 3`，签名只覆盖前两段，追加第 4 段后签名仍有效但 tokenHash 改变，denylist 失效。

**允许修改**：
```text
after/foodmap-common/src/main/java/com/foodmap/common/security/HmacTokenCodec.java
after/foodmap-common/src/test/java/com/foodmap/common/security/HmacTokenCodecTest.java
```

**关键陷阱（必须遵守）**：Refresh Token 是 **4 段**（第 4 段为 UUID nonce）。**不能**把 `parse()` 直接改成 `parts.length == 3`，否则 Refresh Token 全部失效。必须按类型传入期望段数。

**交付物**：
1. `parse(String token, int expectedParts)` — 严格 `equals` 校验
2. `parseAccessToken` → 3 段；`parseRefreshToken` → 4 段
3. 补 5 个回归断言：原样通过 / 追加 `.suffix` 拒绝 / 篡改 payload 拒绝 / 篡改 signature 拒绝 / Access 传 Refresh Token 拒绝

**验收标准**：
- [ ] 合法 Access Token（3 段）解析成功
- [ ] 合法 Refresh Token（4 段）解析成功（**防止修复引入回归**）
- [ ] `accessToken + ".anything"` 抛 `UNAUTHORIZED`
- [ ] 上述 5 个断言全部通过
- [ ] `mvn -q -B -pl foodmap-common test` 通过

---

#### T-027 登出必须写拒绝名单

**背景**：网关把 `/api/auth/logout` 列为公开路径，不校验 Access Token；`AuthController.logout` 的 `Authorization` 是 `required=false`。客户端不带 Authorization 时登出仍返回 200，但旧 Access Token **不进 denylist**，与 T-026 叠加会削弱「登出即失效」保证。

**允许修改**：
```text
after/foodmap-auth-service/src/main/java/com/foodmap/auth/controller/AuthController.java
after/foodmap-auth-service/src/main/java/com/foodmap/auth/service/impl/AuthServiceImpl.java
after/foodmap-auth-service/src/test/java/com/foodmap/auth/**
harness/scenarios/b1-logout.yml
```

**禁止修改**：`after/foodmap-gateway-service`（公开路径策略由主代理评估后另行授权）

**交付物**：
1. `logout` 在携带 `Authorization` 时**必须**校验 Token 有效性并写 denylist；Token 无效返回 401 而非静默 200
2. 不携带 `Authorization` 时仅撤销 Refresh Token（保持幂等，不泄露 Token 是否存在）
3. 补充 Javadoc 说明两种分支的语义差异
4. `b1-logout.yml` 增加断言：**不带** Authorization 登出后，旧 Access Token 仍可用（明确记录该行为边界）

**验收标准**：
- [ ] 带有效 Token 登出 → denylist +1
- [ ] 带无效 Token 登出 → 401，denylist 不变
- [ ] 不带 Token 登出 → 200，仅 Refresh 撤销
- [ ] `b1-logout.yml` 全部断言通过

---

#### T-028 统一 logback JSON 与文件落盘

**背景**：全仓库 0 个 `logback-spring.xml`，无 `logging.file.name`，日志只进控制台。Fluent Bit 监听 `/foodmap/logs/*.log` 但无人写入，且 `parsers.conf` 为 `Format json` 而 `SafeLog` 输出纯文本。

**允许修改**：
```text
after/foodmap-common/pom.xml
after/foodmap-common/src/main/resources/logback-common.xml（新建）
after/foodmap-common/src/main/java/com/foodmap/common/logging/**（SafeLog / LogField / LogContext）
after/*/src/main/resources/logback-spring.xml（各服务新建）
after/*/src/main/resources/application.yml（logging 段）
```

**交付物**：
1. `foodmap-common` 引入 `logstash-logback-encoder`
2. 公共模板：JSON encoder，MDC 字段（`requestId` / `traceId` / `spanId` / `serviceName`）自动输出
3. **新增 MDC 字段 `logType`**，取值 `application` / `api-access` / `sql` / `audit` / `security`，由日志调用点写入
4. 文件 appender → `/foodmap/logs/${spring.application.name}.log`，滚动策略按天 + 大小
5. 保留控制台 appender

**验收标准**：
- [ ] 每个服务输出单行合法 JSON，可被 `jq` 解析
- [ ] JSON 含 `requestId` / `traceId` / `serviceName` / `logType` / `timestamp` / `level`
- [ ] `/foodmap/logs/*.log` 能实际生成文件
- [ ] `mvn -q -B test` 通过（现有 `SafeLogTest`、`LogMdcFilterTest` 不得回归）

**验证**：本地起 auth，调用一次接口，`jq` 校验日志行

---

#### T-029 应用服务挂载日志卷并对齐采集

**依赖**：T-028

**允许修改**：
```text
deploy/docker-compose.dev.yml
deploy/docker-compose.ecs2.yml
deploy/logging/fluent-bit/fluent-bit.conf
deploy/logging/fluent-bit/parsers.conf
harness/scripts/validate-logging.sh
```

**交付物**：
1. dev / ecs2 compose 中所有应用服务挂载 `foodmap-app-logs:/foodmap/logs:rw`
2. Fluent Bit `rewrite_tag` 改为**按 `logType` 字段路由**，删除基于 message 的 `.​*api\.access\..*` 正则
3. `validate-logging.sh` 增加检查：parser 与 logback encoder 格式一致、路由字段存在

**验收标准**：
- [ ] 启动 auth + gateway 后，`/foodmap/logs/` 卷内有对应 `.log` 文件
- [ ] Fluent Bit 日志无 parse error
- [ ] Kafka `foodmap.logs.api-access` topic 有消息（`kafka-console-consumer` 抽查）
- [ ] `./harness/scripts/validate-logging.sh` 通过

---

#### T-030 日志闭环端到端 verify 场景

**依赖**：T-028, T-029

**允许修改**：
```text
harness/scenarios/b1-logging-closure.yml（新建）
harness/verify-dependencies.json
status/data/runs/（仅追加）
```

**场景步骤**：
1. 注册 → 登录（拿到 Access Token，捕获 requestId 响应头）
2. 断言 `foodmap-postgres` 的 `foodmap_log_db.api_access_log` 存在该 requestId 行（轮询，超时可配）
3. 断言 ES 能按 `requestId` 检索到至少 1 条日志
4. 断言日志 JSON 含 `traceId` / `serviceName`，且**不含**明文密码、完整 Token

**验收标准**：
- [ ] 三处数据（文件 / Kafka→ES / 日志 PG）全部可查
- [ ] 断言中无明文密码与完整 Token
- [ ] `VERIFY_REQUIRE_FULL=1 ./harness/scripts/verify.sh` 通过
- [ ] 写入 `status/data/runs/` 运行记录

---

#### T-031 关闭 gateway discovery locator

**允许修改**：
```text
after/foodmap-gateway-service/src/main/resources/application.yml
after/foodmap-gateway-service/src/test/java/com/foodmap/gateway/**
harness/scenarios/（新增断言场景）
```

**交付物**：`spring.cloud.gateway.discovery.locator.enabled: false`，并补测试断言 `/foodmap-*-service/**` 不被自动路由。

**验收标准**：
- [ ] `/foodmap-log-service/internal/logs/api-access` 返回 404（非 200）
- [ ] 显式路由（`/api/**`、`/internal/**`）正常工作
- [ ] `GatewayAuthFilterTest` 通过

**注意**：这是**唯一**一个改动量 1 行但收益很高的任务，建议最先做。

---

#### T-032 内部接口服务身份签名

**背景**：`/internal/users/provision`、`/internal/logs/api-access` 无任何认证，同网段容器可直连调用。网关只挡外部。

**允许修改**：
```text
after/foodmap-common/src/main/java/com/foodmap/common/security/InternalServiceAuth*.java（新建）
after/foodmap-common/src/main/java/com/foodmap/common/security/InternalServiceSignature.java（新建）
after/foodmap-common/src/test/java/com/foodmap/common/security/**
after/foodmap-user-service/src/main/java/com/foodmap/user/controller/InternalUserProvisionController.java
after/foodmap-log-service/src/main/java/com/foodmap/log/controller/LogApiAccessLogController.java
after/foodmap-auth-service/src/main/java/com/foodmap/auth/infrastructure/client/user/FeignUserProfileProvisionClient.java
after/foodmap-admin-service/src/main/java/com/foodmap/admin/infrastructure/client/**
deploy/env.example
```

**交付物**：
1. `InternalServiceSignature`：签名 `serviceName + timestamp + nonce + bodyHash`，HMAC-SHA256
2. 请求拦截器自动注入 `X-FoodMap-Service` / `X-FoodMap-Timestamp` / `X-FoodMap-Nonce` / `X-FoodMap-Signature`
3. 服务端校验：时间窗 ≤ 5 分钟、nonce 去重（Redis，TTL 10 分钟）、调用方白名单
4. 白名单：`/internal/users/provision` 仅允许 `foodmap-auth-service`

**验收标准**：
- [ ] 无签名头的内部请求返回 401
- [ ] 过期时间戳（> 5 分钟）返回 401
- [ ] 重放同一 nonce 返回 401
- [ ] 非白名单服务调用 provision 返回 403
- [ ] 正常内部调用链路不受影响（注册流程仍成功）
- [ ] 新增单测覆盖上述 4 个拒绝分支

**风险**：改动横跨 5 个模块，建议主代理串行推进，不要并行分派。

---

#### T-033 后台 RBAC 服务端化

**背景**：`AdminPermissionGuard` 从 `X-Admin-User-Id` / `X-Admin-Permissions` 请求头读身份与权限；网关**完全不处理** `X-Admin-*`。任何能触达 admin 服务的调用方自带权限头即可通过。

> 2026-11-10 更新：实现方案已评审定稿为独立管理员 RSA JWT + `admin_access_sessions` 会话表 + RBAC 五表，文件边界按
> [T-033 设计稿](../specs/2026-11-10-t033-admin-rbac-implementation.md) §5 扩展（含 admin 登录/登出 DTO、Service、密钥配置、
> 依赖声明、gateway 路由与后台验签分支、t033 场景与 T-032 探针改造），以任务卡 T-033 §3 为准。

**允许修改**：
```text
after/foodmap-admin-service/src/main/java/com/foodmap/admin/security/**
after/foodmap-admin-service/src/main/java/com/foodmap/admin/controller/**
after/foodmap-admin-service/src/main/resources/db/migration/V2__*.sql（新建）
after/foodmap-admin-service/src/main/resources/mapper/**
after/foodmap-gateway-service/src/main/java/com/foodmap/gateway/filter/GatewayAuthFilter.java
after/foodmap-gateway-service/src/test/java/com/foodmap/gateway/**
```

**交付物**：
1. 网关**无条件剥除** `X-Admin-*` 请求头（与现有 `X-FoodMap-*` 剥除逻辑一致）
2. admin 权限改为从数据库读取（`admin_user_roles` / `admin_role_permissions` 或等价表，需 Flyway 迁移）
3. `AdminPermissionGuard` 只接受已验证的 admin 会话标识，不接受权限码
4. 补充迁移的表/字段中文注释（遵守 AGENTS.md §8.2）

**验收标准**：
- [ ] 客户端自带 `X-Admin-Permissions: LOG_ACCESS_READ` 无法通过校验
- [ ] 网关剥除 `X-Admin-*` 有单测断言
- [ ] 正常管理员会话可访问 `/api/admin/logs`
- [ ] Flyway 迁移可在空库执行成功
- [ ] 表与字段含 `comment on table` / `comment on column`

---

#### T-034 各服务多阶段 Dockerfile

**依赖**：T-025

**允许修改**：
```text
after/Dockerfile（新建，或 after/foodmap-*/Dockerfile）
after/.dockerignore（新建）
```

**交付物**：多阶段构建，`maven:3.9-eclipse-temurin-21` 构建 → `eclipse-temurin:21-jre-alpine` 运行；非 root 用户；`JAVA_OPTS` 可注入；镜像标签含 commit sha。

**验收标准**：
- [ ] 11 个模块镜像均可构建
- [ ] 镜像内不含源码与 Maven 缓存
- [ ] 容器内以非 root 运行
- [ ] `docker run` 后 `/actuator/health` 可达（连测试库）

---

#### T-035 生产 compose 补齐应用服务

**依赖**：T-034

**允许修改**：`deploy/docker-compose.ecs2.yml`、`deploy/env.example`

**交付物**：auth / user / admin / log 服务定义，含 `healthcheck`、`depends_on: service_healthy`、`restart: unless-stopped`、资源限制、日志卷挂载。

**验收标准**：
- [ ] `docker compose -f deploy/docker-compose.ecs2.yml config` 无错误
- [ ] 全栈启动后 `curl http://gateway/actuator/health` 为 UP
- [ ] 注册→登录→查询当前用户链路端到端通过
- [ ] 删除「Application services are placeholders」注释

---

#### T-036 nginx HTTPS / HSTS / 安全头

**允许修改**：`deploy/nginx/foodmap.conf`、`deploy/nginx/`（证书挂载说明）、`deploy/env.example`

**验收标准**：
- [ ] 80 端口全部 301 跳 443
- [ ] 443 启用 TLS 1.2/1.3，禁用弱套件
- [ ] 响应含 `Strict-Transport-Security`、`X-Content-Type-Options`、`X-Frame-Options`
- [ ] 自签证书下 `curl -k https://localhost/api/...` 正常
- [ ] `location /actuator/` 仍 403

---

#### T-037 GitHub Actions CI 流水线

**依赖**：T-034

**允许修改**：`.github/workflows/backend.yml`（新建）

**交付物**：`mvn verify` + `./harness/scripts/run-all.sh` + 依赖漏洞扫描（Trivy 或 dependency-check）+ 镜像构建（不推送）。

**验收标准**：
- [ ] PR 触发，失败阻断合并
- [ ] 构建缓存生效（`actions/setup-java` + `cache: maven`）
- [ ] 高危依赖漏洞使流水线红灯

---

#### T-038 Testcontainers 集成测试

**依赖**：T-026

**允许修改**：`after/pom.xml`、`after/*/pom.xml`、`after/*/src/test/**`（新增 `*IT.java`）

**交付物**：至少覆盖 auth/user 的 Flyway 迁移执行、Repository 读写、事务回滚、唯一索引冲突。

**验收标准**：
- [ ] `mvn verify` 跑起真实 PostgreSQL 容器
- [ ] 冲突场景断言 `DuplicateKeyException` 或映射后的 409
- [ ] 事务回滚断言数据未落库
- [ ] 测试不依赖外部手工环境

---

#### T-039 mvnw + enforcer + Redisson optional

**允许修改**：`after/pom.xml`、`after/foodmap-common/pom.xml`、`after/mvnw*`（新建）

**验收标准**：
- [ ] `./mvnw -v` 可用
- [ ] enforcer 强制 Java 21 + Maven 3.9+，违规构建失败
- [ ] `foodmap-common` 的 Redisson 标 `optional`，gateway 依赖树不再含 Netty
- [ ] `mvn -q -B test` 通过

---

#### T-040 Prometheus 指标与探针

**允许修改**：`after/pom.xml`、`after/*/pom.xml`、`after/*/src/main/resources/application*.yml`

**验收标准**：
- [ ] 各服务 `/actuator/prometheus` 返回指标
- [ ] `/actuator/health/liveness` 与 `/actuator/health/readiness` 分离
- [ ] HikariCP、Redis pool、HTTP 请求指标可见
- [ ] nginx 层禁止外部访问 `/actuator/prometheus`

---

#### T-041 springdoc OpenAPI 与 /api/v1

**允许修改**：`after/pom.xml`、`after/*/pom.xml`、`after/*/src/main/java/**/controller/**`、`after/*/src/main/resources/application*.yml`、`after/foodmap-gateway-service/src/main/resources/application.yml`

**交付物**：springdoc + Knife4j 配置；`/api/v1/**` 版本前缀策略（网关重写或 controller 路径迁移，二选一并在 CODEX-after.md 记录）。

**验收标准**：
- [ ] `/v3/api-docs` 可访问
- [ ] 文档含 DTO schema 与错误码
- [ ] 版本策略与网关路由一致
- [ ] `docs/api/auth-user.md` 与实现 diff 为 0（含 refresh 不轮换的说明修正）

---

#### T-042 AuthServiceImpl 按用例拆分

**依赖**：T-038（确保拆分时有集成测试兜底）

**允许修改**：`after/foodmap-auth-service/src/main/java/com/foodmap/auth/**`

**交付物**：拆为 `RegisterUseCase` / `LoginUseCase` / `SessionUseCase` / `IdentityBindingUseCase`，单类 ≤ 300 行。

**验收标准**：
- [ ] 原 743 行 `AuthServiceImpl` 拆分完成
- [ ] `@Deprecated` 双写代码有退出计划（TODO + issue 编号）或已移除
- [ ] 全部现有测试通过（不得删除测试来「通过」）
- [ ] T-038 集成测试通过

---

#### T-043 ArchUnit 分层约束

**允许修改**：`after/pom.xml`、`after/foodmap-common/pom.xml`、`after/*/src/test/java/**/architecture/**`

**验收标准**：
- [ ] `domain` 不依赖 `infrastructure`
- [ ] `application` 不依赖 `persistence.entity`
- [ ] `controller` 不依赖 `mapper`
- [ ] 规则失败时构建失败
- [ ] 记录当前违规数为基线，逐步归零

---

#### T-044 全局异常映射补全

**允许修改**：`after/foodmap-common/src/main/java/com/foodmap/common/web/FoodMapGlobalExceptionHandler.java`、对应测试

**交付物**：补 `MissingRequestHeaderException`（→401）、`HttpMediaTypeNotSupportedException`（→415）、`MissingPathVariableException`、`HandlerMethodValidationException`。

**验收标准**：
- [ ] `UserController.me` 缺 `X-FoodMap-User-Id` 返回 401（原为 500）
- [ ] 错误 Content-Type 返回 415
- [ ] MockMvc 覆盖所有错误分支
- [ ] 响应不含异常类名与堆栈

---

#### T-045 数据访问加固

**允许修改**：`after/*/src/main/resources/mapper/**`、`after/*/src/main/java/**/infrastructure/persistence/**`

**交付物**：`UserRepositoryImpl.save` 改 `ON CONFLICT` 或捕获 `DuplicateKeyException` → 409；`selectListByCondition` 强制 limit；LIKE 转义 `%`/`_`；`findByLoginIdentifier` 拆为类型化解析。

**验收标准**：
- [ ] 并发 upsert 不产生 500
- [ ] 列表查询有 limit
- [ ] 用户输入 `%` 不再变成通配符
- [ ] 登录标识按 accountName/phone/email 显式区分

---

#### T-046 clientIp 掩码与审计事件

**依赖**：T-028

**允许修改**：`after/foodmap-common/src/main/java/com/foodmap/common/logging/**`、`after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/messaging/ApiAccessLogEventParser.java`、`after/foodmap-common/src/main/java/com/foodmap/common/logging/AuditLog*.java`（新建）

**验收标准**：
- [ ] `client_ip_masked` 实际为掩码值（保留前两段）
- [ ] 字段名与实际语义一致
- [ ] 提供 `AuditLog` 统一接口，关键动作（登录、登出、管理员操作）产出审计事件
- [ ] `logType=audit` 的日志进入 Kafka `foodmap.logs.audit`

---

#### T-047 注册流程 Outbox 最终一致性

**依赖**：T-038

**背景**：`AuthServiceImpl.register` 在 `@Transactional` 内调用 Feign 开用户，持有数据库连接等待跨服务调用；用户服务已提交而 auth 回滚时产生孤儿用户，无补偿。

**允许修改**：`after/foodmap-auth-service/src/main/java/com/foodmap/auth/**`、`after/foodmap-auth-service/src/main/resources/db/migration/V6__*.sql`（新建）、`after/foodmap-user-service/src/main/java/com/foodmap/user/controller/InternalUserProvisionController.java`

**交付物**：本地事务写 auth 库 + Outbox 记录 → 事务提交后异步投递；用户开通接口支持幂等键；失败状态机 `PENDING→PROVISIONING→ACTIVE→FAILED→COMPENSATING→COMPENSATED`。

**验收标准**：
- [ ] 用户服务不可用时注册返回明确状态，不产生孤儿数据
- [ ] 重复开通返回同一结果（幂等）
- [ ] 补偿任务可重试失败记录
- [ ] 事务内不再有跨服务同步调用

---

### 7. 波次依赖图

```text
W0  T-025 ──┬────────────────────────────────────┐
    T-026   │                                    │
    T-027 ←─┘                                    │
                                                 │
W1  T-028 ──┬──→ T-029 ──→ T-030                 │
            │                                    │
W2  T-031 ──┼──→ T-032                            │
            │                                    │
            └──→ T-033                            │
                                                 │
W3  T-034 ←──────────────────────────────────────┘
    T-036（独立）
    T-034 ──→ T-035
        └───→ T-037

W4  T-026 ──→ T-038 ──┬──→ T-042
                      └──→ T-047
    T-039 / T-040 / T-041 / T-043 / T-044 / T-045（基本独立）
    T-028 ──→ T-046
```

**可并行分组**（互不冲突的文件边界）：

| 组 | 任务 | 说明 |
| --- | --- | --- |
| A | T-026 | 只碰 `common/security` |
| B | T-025 → T-027 | 都碰 auth/gateway，串行 |
| C | T-031 → T-032 | 都碰 gateway，串行 |
| D | T-028 → T-029 → T-030 | 日志链，串行 |
| E | T-034 → T-035 / T-037 | 部署链，串行 |
| F | T-036 / T-039 / T-040 / T-041 / T-043 / T-044 | 互相独立，可并行 |

---

### 8. 立刻可开始的最小动作

如果只想先动一件事，按收益/成本排序：

1. **T-031**（0.2d，1 行配置）— 关掉 discovery locator，攻击面立刻收缩
2. **T-025**（0.5d，约 30 行）— 解开认证密钥死结，是后续所有安全工作的前置
3. **T-026**（0.5d，约 15 行）— 关闭 Token 后缀绕过，配 5 个回归断言

**三项合计约 1.2 人日**，完成后 P0 安全项中最致命的三个洞即关闭，可以安全开始门店/推荐等核心域开发。

---

### 9. 落地方式

按 `AGENTS.md` §4：

- **第 1 步**：主代理手写 `status/data/features/F-024.yml` ~ `F-030.yml`（CLI 无 `feature add`），跑 `node status/tool/cli.mjs check`
- **第 2 步**：用 `node status/tool/cli.mjs task add F-024 --title="..." --priority=P0 --plan=docs/superpowers/plans/2026-11-09-backend-hardening.md` 逐条创建 T-025 ~ T-047
- **第 3 步**：按 §7 的可并行分组分派子代理，每个任务套用 `harness/agent-task-template.md`
- **第 4 步**：每个任务完成后按 §6.3.1 执行 verify 并写 `status/data/runs/`，再 `status check` + 重新生成看板

**不建议并行分派的任务**：T-025/T-027（同碰 GatewayAuthFilter）、T-031/T-032/T-033（同碰 gateway）、T-038/T-042（同碰 auth 测试）。

---

## 附：与原报告整改路线的关系

| 原报告 | 本方案 | 差异 |
| --- | --- | --- |
| 第 0-2 周 P0 七项 | Wave 0 + Wave 1 | 日志提前，部署拆出 |
| 第 2-6 周 P1 七项 | Wave 2 + Wave 3 + Wave 4 部分 | 内部信任并入安全波次 |
| 第 6-12 周 P2 | Wave 4 收口 | 与 W4 合并，不单列时间窗 |

原报告的时间窗假设「一次只做一件事」。本方案按**可并行的文件边界**组织，实际日历时间可压缩约 40%。
