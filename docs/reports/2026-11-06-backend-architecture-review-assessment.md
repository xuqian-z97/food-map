# FoodMap 后端架构评审复核评估

- 复核日期：2026-11-06
- 复核对象：[2026-11-06-backend-architecture-review.md](2026-11-06-backend-architecture-review.md)
- 复核方式：对评审报告的每条结论回到 `after/` 源码逐项验证，重新执行构建与测试，并补充评审未覆盖的仓库事实
- 文档口径调整原则：**技术结论从严复核，评分口径按"当前阶段"与"生产就绪"双轨重算**
- 本文不修改业务代码，只做评估

---

## 1. 结论摘要

原报告的技术判断 **绝大多数成立**，我逐条核对 P0/P1 共 12 项，没有发现事实性错误。它的问题不在"说错了"，而在三处：

1. **评分口径混用**："成熟度 1-5 分"表里既有架构质量（针对骨架）又有就绪度（针对生产），最后合成一个 2.8 分，导致读者无法判断"该先补架构还是先补部署"。
2. **漏掉了一块真实资产**：仓库里已存在 `tools/verify/`（3,785 行零依赖黑盒验证工具，7 个适配器）和 `harness/scenarios/`（5 个可执行场景），并有 `status/data/runs/` 下的真实运行记录。这直接驳倒原报告"没有集成测试、没有质量门禁"的绝对化表述——有集成验证，只是**不在 Maven 里**。
3. **规模数字偏差**：原报告"284 个 Java 主代码文件"实测为 **232 个**（14,339 行准确）。这会影响"骨架完成度"的主观印象。

重新计算后的判断：

> 架构蓝图 7 分 → **维持 7 分**（文档体系确实是本项目最强项）  
> 代码骨架 6 分 → **下调至 5 分**（分层倒置与 God Service 比我预期的更严重）  
> 生产就绪度 3 分 → **下调至 2 分**（无 TLS、无镜像、无 CI、无指标，且密钥链路断开）

综合分从 2.8 调整为 **2.9**（分项互有升降，见 §2），但**结论方向不变且更明确：不要现在写核心业务，先修 P0。**

---

## 2. 重新打分（双轨）

原报告的单一 2.8 分信息量不足。拆成两轨：**骨架工程分**（衡量"作为学习/迭代基座的质量"）与**生产就绪分**（衡量"能否承载真实用户"）。同一份代码在两个口径下本来就该有两个分数。

### 2.1 骨架工程分（衡量代码组织与可演进性）

| 维度 | 原分 | 复核分 | 调整理由 |
| --- | --- | --- | --- |
| 架构搭建与边界 | 3.5 | **3.5** | 维持。服务拆分、独立库（9 个服务 9 个库已核实）、未发现跨库访问，判断准确 |
| 编码结构 | 2.5 | **2.5** | 维持。`AuthServiceImpl` 743 行、端口返回持久化实体、`@Deprecated` 11 处，均核实无误 |
| 编码规范 | 3.5 | **3.0** ↓ | 下调。`UserController.me` 的 Javadoc 确实无 `@param`/`@return`，而 `validate-backend.sh` 第 180 行只跑 `mvn -q -DskipTests validate`——**门禁 PASS 与规范执行完全脱钩**，这比"规范执行靠脚本"更严重 |
| 数据与一致性 | 3.0 | **3.0** | 维持。Schema 质量高是真的；log-service 无 Flyway 依赖、迁移脚本在 `deploy/` 下已核实 |
| 文档与治理 | 4.5 | **4.5** | 维持。CODEX 5 份共 5,919 行 + AGENTS + skills + harness，确实是个人项目里的顶配 |
| **骨架综合** | — | **3.3** | 比原报告 2.8 高，因为把"部署/安全"从这一轨剥离了 |

### 2.2 生产就绪分（衡量能否对外上线）

| 维度 | 原分 | 复核分 | 调整理由 |
| --- | --- | --- | --- |
| 安全与合规 | 1.5 | **1.0** ↓ | 下调。原报告列了 6 项，我核实到**更严重的一层**：`HmacTokenIssuer` 的 `@Component` 无参构造器直接写死默认密钥，认证服务**没有任何配置项**能注入 `FOODMAP_TOKEN_SECRET`。这不是"配置没配好"，是**结构上无法配置**。叠加 `X-Admin-*` 头网关完全不处理（已用 grep 确认），后台权限等于没有 |
| 测试与质量门禁 | 2.0 | **2.5** ↑ | 上调。Maven 侧确实 0 个 Testcontainers、0 个 `@SpringBootTest`；但 `tools/verify` 的 5 个场景是**真跑真实网关 + 真实 Redis + 真实 PostgreSQL** 的（见 `b1-logout.yml`，含 denylist 计数断言和 DB 状态断言）。这是货真价实的集成验证能力，只是没进 CI。给 2.5 而非 3.0，因为它需要手工起 Docker 环境，不可自动拦截回归 |
| 部署与运维 | 1.5 | **1.5** | 维持。无 Dockerfile（全仓库 0 个）、compose 无应用服务、nginx 仅 80、无 logback。全部核实 |
| 可观测性（新增独立项） | — | **1.0** | 原报告并进了"部署"。单列更清楚：无 micrometer、无 OTel、actuator 仅 `health,info`、Fluent Bit 用 JSON parser 而 `SafeLog` 输出 `key=value` 文本，**格式根本不匹配**，采集链路即使有数据也解析不出字段 |
| **就绪综合** | — | **1.4** | 明显低于原报告暗示的 3 分 |

### 2.3 综合判断

| 口径 | 分数 | 一句话 |
| --- | --- | --- |
| 骨架工程分 | **3.3 / 5** | 结构清晰、文档扎实，是个好基座 |
| 生产就绪分 | **1.4 / 5** | 现在上线等于把用户凭证放在明文 HTTP 上 |
| 原报告综合分 | 2.8 | 复核后 **2.9**，差异不大，但含义变了 |

**这个 2.9 不是"中等偏下"，而是"两极分化"**——文档和 Schema 是 4.5 级，安全与部署是 1.0 级。平均值掩盖了真正的风险形状。原报告用平均分表达，容易让读者以为"各方面都差一点，慢慢补"，实际是"少数几个洞足以致命，其余都还不错"。

---

## 3. 对原报告结论的核实结果

### 3.1 成立（无需调整）

| 编号 | 结论 | 复核证据 |
| --- | --- | --- |
| P0-1 | Token 密钥未闭环 | `HmacTokenIssuer.java:23-24` 无参构造器硬编码；auth 模块**无任何** `@Value` 读取密钥；`deploy/env.example:145` 写 `JWT_SECRET`，全仓库无代码读取 |
| P0-2 | Token 后缀绕过拒绝名单 | `HmacTokenCodec.java:161-167` 确认 `parts.length < 3` 判断，仅校验 `parts[0]+"."+parts[1]`；`tokenHash()` 对整串签名，故追加后缀后哈希改变、denylist miss |
| P0-3 | 内部接口/后台权限可伪造 | `InternalUserProvisionController` 无任何鉴权注解；`AdminPermissionGuard.java:22-32` 直接读 `X-Admin-Permissions` 头；网关 grep 确认**未移除** `X-Admin-*`；`discovery.locator.enabled: true` 已确认 |
| P0-4 | 无 TLS / 无镜像 / 未编排 | `foodmap.conf` 仅 `listen 80`；全仓库 0 个 Dockerfile；`docker-compose.ecs2.yml:96` 注释确认占位；`env.example` 无 `ADMIN_DB_USERNAME` |
| P0-5 | 日志平台无数据源 | 全仓库 0 个 `logback*`；应用服务未挂载日志卷；`parsers.conf` 为 `Format json` 而 `SafeLog` 输出文本 |
| P1-1 | 分层倒置 | `application/port/*Repository` 返回 `infrastructure.persistence.entity.*`；domain 层仅枚举 + 2 个工具类且带 `@Component` |
| P1-2 | 事务内跨服务调用 | `AuthServiceImpl.java:178 @Transactional` → `:210 userProfileProvisionClient.provision(...)`，在事务提交前调用 Feign |
| P1-3 | 无可观测性 | auth pom 无 micrometer/OTel；所有服务 actuator 仅 `health,info` |
| P1-4 | 契约漂移 | `AuthController.refresh` Javadoc 写"必须保存轮换后的 Refresh Token"，`AuthServiceImpl.refresh` 注释明确"MVP 阶段不轮换"并原样返回；无 springdoc/knife4j；无 `/api/v1` |
| P1-5 | 安全基线缺口 | auth pom 无 `spring-boot-starter-security`；全仓库无 CORS/RateLimit 配置；`trusted.packages: "*"` 已确认；`FoodMapAccessTokenDenylistAutoConfiguration` 确为 `@ConditionalOnMissingBean` 静默回退 Noop |
| P1-6 | 无 CI | 无 `.github/`；父 POM 无 JaCoCo/Surefire 固定版本/Enforcer/SpotBugs/Checkstyle |
| P1-7 | 供应链薄弱 | 无 `mvnw`；`foodmap-common/pom.xml:45` Redisson 非 optional，而 mybatis/feign/web 都标了 optional——这个对比说明作者知道怎么做却没做 |

### 3.2 需要修正或补强

#### 修正 1：规模数字

> 原报告：「Java 主代码 284 个文件，约 14,339 行」

实测：**232 个文件**，14,339 行（行数准确，文件数偏差 18%）。测试 52 个文件 / 118 个 `@Test` 准确。Flyway 9 个准确（auth 5 + user 3 + admin 1，log 的 2 个在 `deploy/logging/postgres/migration`）。Mapper XML 34 个准确。

影响：284 vs 232 会让"骨架完成度"显得比实际高。配合 common 占 73 个（31.4%），实际业务代码只有约 160 个文件。

#### 修正 2：测试能力的定性

> 原报告：「52 个测试文件、118 个 @Test，全部是普通 JUnit 单元测试」「没有 CI、无覆盖率门禁」

前半句不准确。`UserControllerTest` 使用了 `MockMvcBuilders.standaloneSetup`，是**切片级 MVC 测试**而非纯单元测试。

更重要的是整段遗漏了 `tools/verify/`：

| 事实 | 数值 |
| --- | --- |
| `tools/verify/` 代码 | 3,785 行（`cli.mjs` 283 行 + 9 个 core 模块 + 7 个适配器） |
| 适配器 | `app` `command` `http` `logfile` `redis` `sql` `xcode`——覆盖 HTTP、Redis、SQL、日志文件、iOS 模拟器 |
| 场景 | 5 个：`b1-logout` `b1-identity-dual-write` `b1-identity-consistency` `b1-ios-logout` `b1-ui-login` |
| 运行记录 | `status/data/runs/` 下 10+ 份 JSON，含通过数与覆盖功能点 |

`b1-logout.yml` 的断言质量值得单独说：它不只断言 HTTP 200/401，还断言 Redis denylist key **数量 +1**、PostgreSQL 里 `refresh_tokens.token_status='REVOKED'` 计数为 1。这是**状态级断言**，比大多数团队的集成测试更硬。

**但仍要下调其价值上限**：这套工具需要手工 `docker compose up` + `mvn spring-boot:run`，不能进 CI 自动拦截，且没有覆盖率。所以是"有集成验证能力"，不是"有集成测试门禁"。评分从 2.0 → 2.5。

#### 修正 3：安全问题的严重度应升级

原报告把 P0-1 描述为"密钥没有真正接入配置"。核实后应更重：

- `HmacTokenIssuer` 标注 `@Component`，Spring 会选**无参构造器**，因此密钥恒为 `foodmap-local-token-secret-please-change`。
- auth 服务全部源码中 **0 处** `foodmap.security.token-secret` 引用（网关有）。
- 结论不是"运维忘记配"，而是"**认证服务在此代码版本下无论怎么配都用默认密钥**"。

这意味着即使运维按 `env.example` 设了 `JWT_SECRET`，登录签发的 Token 也必然被网关拒绝——**当前登录链路在生产配置下是断的**。原报告 §4 P0-1 的"影响"第 2 条说"如果只在网关设置…登录链路直接不可用"，这个描述是对的，但可以更直白：**这不是"可能不一致"，而是"只要网关配了密钥就必然不一致"**。

#### 修正 4：补充原报告未提的 3 个发现

**(a) `AuthServiceImpl.login` 的 `noRollbackFor` 语义可疑**

```java
@Transactional(rollbackFor = Exception.class, noRollbackFor = FoodMapException.class)
public LoginResponse login(LoginRequest request) {
```

`noRollbackFor = FoodMapException.class` 意味着**任何业务异常都不回滚**。登录失败时若已写入 `login_logs` 或更新了失败计数，这些写入会被提交——这可能是"有意为之"（记录失败尝试），但没有注释说明意图。在 `rollbackFor = Exception.class` 同时存在时，这两个注解的组合极易被后续维护者误读。建议显式拆分：失败记录用独立事务或 `REQUIRES_NEW`，主流程保持标准回滚语义。

**(b) `Check.notBlank` 对密码做 trim**

原报告在 P2-4 提了，但低估了影响：密码 `"  abc123  "` 和 `"abc123"` 在系统内等价。这不仅是"不符合常见实践"，而是**缩小了有效密码空间**并在用户换端登录（如 iOS 输入法自动补空格）时产生难以排查的登录失败。应单独列为 P1。

**(c) 网关 `PUBLIC_API_PATHS` 包含 `/api/auth/logout`，但 logout 需要身份**

`GatewayAuthFilter` 把 `/api/auth/logout` 列入公开路径，即不校验 Access Token 就放行。`AuthController.logout` 的 `Authorization` 头是 `required=false`，所以登出不写 denylist 也返回 200。攻击者拿到 Refresh Token 可以登出（无实际危害），但更重要的是：**被窃取的 Access Token 在受害者"登出"后若客户端没正确带 Authorization 头，就不会进 denylist**。这与 P0-2 叠加，使"登出即失效"的保证更弱。

---

## 4. 建议调整的修复优先级

原报告的 P0 五条我认为都对，但顺序应调整——**按"修完之后能否验证"排序**，而不是按"看起来多严重"排序。

| 原序 | 建议序 | 事项 | 理由 |
| --- | --- | --- | --- |
| P0-1 | **1** | Token 密钥注入 + 生产缺失即启动失败 | 唯一的"结构上无法配置"问题，且改动最小（加一个 `@Value` + fail-fast）。修完后其他安全修复才可验证 |
| P0-2 | **2** | 严格 `parts.length` + 回归测试 | 改动 3 行，配 `tools/verify` 场景立即可验证 |
| P0-5 | **3** | 日志落盘 + JSON 对齐 | **提前**。因为后面所有修复的排障都依赖它；现在出问题只能看控制台 |
| P0-3 | **4** | 关 discovery locator → 内部服务身份 → 后台 RBAC 服务端化 | 分三步走，先关 locator（1 行），再做服务身份（有设计量） |
| P0-4 | **5** | Dockerfile + 应用编排 + HTTPS | 工作量最大，但**前置依赖 P0-1**（否则编排出来的服务互相签验失败） |

关键调整：**把 P0-5（日志）提到 P0-3（内部认证）之前**。原报告把它放最后，但它其实是"可验证性"的基础设施——没有日志落盘，后面三层安全改造出问题都只能靠猜。

---

## 5. 对"根因分析"的补充

原报告 §5 三条根因（文档跑太快、common 过大、网关单点信任）我认同。补一条更底层的：

### 5.4 缺少"完成"的定义，导致骨架被当作成品

`AGENTS.md` 里写得很清楚："功能点只有通过验收单并具备测试运行证据后，才能标为 `accepted`。'代码已写完'只能标 `implemented`"。这条规则是对的。

但实际执行中，P0-5 的日志平台在 `AGENTS.md §11` 被描述为"已完成…接口访问摘要消费落库、内部查询 API、15 天保留清理任务、OSS 归档计划记录、归档执行器状态机骨架"。**每一句都真实**——代码确实写了、单测确实过了。但从"平台是否有数据流动"看，它是 0。

这是"实现完成"与"能力完成"的混淆。建议在 `status/data/` 里为每个能力加一列 **`evidence_kind`**：

- `unit`（有单测）
- `integration`（有 verify 场景通过）
- `e2e`（端到端真实数据流验证）

日志平台目前是 `unit`，不是 `integration`。标注清楚后，"看起来什么都有"的错觉会自动消失，也不需要靠外部评审来发现。

---

## 6. 最终评估

| 判断 | 结论 |
| --- | --- |
| 原报告是否可信 | **可信**。12 项 P0/P1 全部核实成立，无事实错误，可直接作为整改依据 |
| 主要缺陷 | 评分口径混用；遗漏 `tools/verify` 资产；文件数偏差 18%；安全严重度估计偏轻 |
| 骨架质量 | **3.3 / 5** — 文档 4.5 级，代码结构 2.5 级，Schema 4.5 级。作为迭代基座是合格的 |
| 生产就绪 | **1.4 / 5** — 差在安全与部署，不在业务逻辑 |
| 是否该立刻写核心业务 | **不该**。修完密钥（1 天）+ Token 严格校验（半天）+ 日志落盘（2 天）后即可开始，不必等 P0 全清 |
| 最该做的一件事 | 给 `HmacTokenIssuer` 加 `@Value` 注入 + 生产 profile 缺失 fail-fast。**这是整份报告里性价比最高的一处改动** |

---

## 7. 复核执行的验证动作

| 动作 | 结果 |
| --- | --- |
| `mvn -q -B test`（after 全模块） | 通过，exit 0，118 个 `@Test` |
| 文件/行数统计 | 232 主文件 / 14,339 行；52 测试文件 / 3,978 行 |
| `grep` 核验 Dockerfile / .github / logback / micrometer / springdoc | 全部 0 命中 |
| `grep` 核验 `jdbc:postgresql` | 9 个服务 9 个独立库，无跨库 |
| `grep` 核验网关对 `X-Admin-*` 处理 | 0 命中，确认未移除 |
| `grep` 核验 auth 模块 `token-secret` | 0 命中，确认无法配置 |
| 核心文件逐行阅读 | `HmacTokenCodec` `GatewayAuthFilter` `AdminPermissionGuard` `AuthServiceImpl` `FoodMapGlobalExceptionHandler` `HmacTokenIssuer` `Fluid-bit conf/parsers` |

复核限度同原报告：静态审阅 + 本地构建，未做渗透测试与压力测试；`tools/verify` 场景未在本轮重新执行（需起 Docker 全栈）。
