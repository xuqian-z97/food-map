# CODEX 项目迭代文档

## 1. 文档目的

本文档用于定义 FoodMap 项目的生成方式、迭代方式和维护规则。

项目必须根据以下产品和架构文档逐步构建：

- CODEX-product.md
- CODEX-front.md
- CODEX-after.md
- CODEX-gen.md
- AGENTS.md
- `.agents/*.md`
- `skills/*/SKILL.md`
- `harness/*.md`

这些文件是后续代码生成的事实依据。

## 2. 强制迭代规则

后续每次变更都必须遵守：

1. 先阅读相关 CODEX 文档。
2. 如果需求影响产品行为，必须更新 CODEX-product.md。
3. 如果需求影响 iOS 架构、页面、SDK 或 UI 流程，必须更新 CODEX-front.md。
4. 如果需求影响后端服务、API、数据归属或基础设施，必须更新 CODEX-after.md。
5. 如果需求影响里程碑、开发顺序或项目规则，必须更新 CODEX-gen.md。
6. 如果需求影响开发代理职责、前后端验收标准或协作规则，必须更新 AGENTS.md。
7. 如果需求影响项目专属 skills，必须更新 `skills/*/SKILL.md`。
8. 如果需求影响多代理约束、验收流程或自动检查脚本，必须更新 `harness/`。
9. 根据更新后的文档生成或修改代码。
10. 尽可能通过测试或构建命令验证变更。

代码不能偏离这些文档。

## 3. GitHub 同步规则

项目远程仓库：

```text
https://github.com/xuqian-z97/food-map.git
```

后续文档和代码都需要与该 GitHub 仓库保持同步。

同步规则：

1. 本地生成或修改文档、代码后，应先检查 git 状态。
2. 提交前确认变更符合 CODEX 文档约束。
3. 每次重要迭代完成后，应提交到 git。
4. 提交后应推送到远程仓库。
5. 如果远程仓库已有新提交，应先拉取并处理冲突，再继续推送。
6. 不得使用会丢弃用户改动的破坏性 git 操作，除非用户明确要求。

## 4. 当前产品方向

FoodMap 是一款 iOS 优先的美食推荐地图 App。

当前方向：

- 个人美食地图。
- 好友共享。
- 情侣共享。
- 可选择公开推荐。
- 公开推荐进入社区统计。
- 后端使用 Java 微服务。
- 每个后端微服务拥有独立数据库。
- 地图数据使用高德地图。
- MVP 支持当前城市高德离线底图。
- MVP 支持弱网或离线时展示低风险缓存门店点位和门店摘要。
- 好友、情侣、指定用户、群组推荐详情和评论内容不做完整离线浏览。

## 5. 当前仓库阶段

当前阶段：

```text
Stage 1：后端认证用户基础能力与 iOS 认证测试壳已完成
```

已完成交付物：

- 产品文档。
- 前端架构文档。
- 后端架构文档。
- 项目迭代文档。
- AGENTS 协作规范。
- .agents 子代理规范。
- skills 项目专属工作流。
- harness 多代理约束和验收脚手架。
- `after/` Java 微服务 Maven 多模块骨架。
- 本地隔离开发环境配置和 profile 切换约定。
- 认证服务和用户服务已接入 PostgreSQL、Flyway 和 MyBatis Mapper/XML 持久化。
- B1 已完成的认证链路仍基于旧 `accountId + userId` 身份模型；2026-06-29 起目标身份模型调整为 `userId-only`，账号名、手机号、邮箱、微信和 Apple 都作为 `userId` 下的登录身份绑定。
- `foodmap-common` 和 `foodmap-gateway-service` 已完成 `userId-only` 首个重构切片：新 Token 签发、当前用户解析和网关可信身份头以 `userId` 为准，旧 `accountId` 仅保留解析兼容。
- 认证服务业务主键已从内存计数器调整为 Flyway 管理的 PostgreSQL sequence，身份重构后将移除 `accountId` 长期主体语义。
- 认证服务已补齐 Refresh Token 刷新、退出登录撤销和当前会话查询接口。
- 网关已具备 Access Token 校验和可信用户身份请求头透传能力，外部客户端伪造的 FoodMap 身份头会被移除。
- `foodmap-common` 已提供 Token 编解码、内部身份请求头常量和当前用户解析工具。
- `foodmap-admin-service` 已生成首个后台服务代码切片，包含后台管理员创建用例、`admin_users` Flyway 表结构、业务主键 sequence、MyBatis Mapper/XML 和本地/profile 配置。
- `foodmap-log-service` 已生成接口访问摘要消费落库代码切片，负责从 `foodmap.logs.api-access` 消费并幂等写入 `foodmap_log_db.api_access_log`。
- `front/FoodMapApp` iOS SwiftUI 工程已生成。
- iOS 登录页、注册页、认证会话状态、Keychain Token 存储和地图占位页已生成。

当前 iOS 工程可通过 Xcode 打开。命令行完整构建依赖本机 Xcode 已安装 iOS 平台组件；若 `xcodebuild` 提示 iOS 平台未安装，需要先在 Xcode Settings 的 Components 中安装对应 iOS 平台。

当前迭代新增约束：

- 数据库结构对应 Java 类作为持久化实体存放在 `infrastructure.persistence.entity` 中。
- 固定字段由 `foodmap-common` 的 `BaseEntity` 承载。
- 数据库表字段、Flyway 字段注释和持久化实体字段 Javadoc 必须保持一致。
- 后端生成或修改业务方法时，方法级 Javadoc 必须说明业务作用，并按入参补齐 `@param`；非 `void` 方法必须补齐 `@return`，`void` 方法不需要 `@return`，JavaBean getter / setter 不纳入强制范围。
- API 响应生成规则统一为 `success`、`status`、`code`、`message`、`data`；异常必须通过统一异常拦截转换，不能由 Controller 手写零散错误响应。
- 后端生成写接口时必须先判断事务和并发控制：单服务多表写使用本地事务，跨服务写使用 Saga/补偿事务 + Outbox + 幂等消费，唯一性或业务主键并发冲突场景评估数据库约束、乐观锁、悲观锁或 Redis 分布式锁。
- 后端生成数据库或 Redis 访问代码时必须同步考虑连接池：PostgreSQL 使用 HikariCP，Redis 需要池化时使用 Lettuce pool；连接池参数通过 profile 和环境变量配置，事务和连接必须短持有，禁止在事务中等待 Redis 锁或调用慢外部依赖。
- 除网关外，后端业务服务生成或调整 profile 时必须同步维护 datasource、Flyway 和 HikariCP 配置，避免同一项目内连接池参数分叉。
- 后端生成分布式锁相关代码时可以使用 Redisson 适配器实现 watchdog，但业务代码只能依赖 `DistributedLockClient`，不能直接调用 Redisson API。
- 持久化实体、DTO、VO 必须分离，Controller 不直接暴露 Entity。
- 业务层统一使用 `XxxService` 接口 + `XxxServiceImpl` 实现类，Controller 只能依赖 Service 接口。
- ServiceImpl 通过仓储端口访问持久化能力，运行时统一使用 MyBatis Mapper + Mapper.xml，内存仓储仅用于测试。
- 每张业务表生成标准 Mapper/XML，复杂业务 SQL 单独放入 DefineMapper/XML。
- Repository 实现类名不再使用 `MyBatis` 等技术前缀，统一采用 `{EntityName}RepositoryImpl` 或业务聚合语义命名。
- 后端 Spring Bean 依赖注入按场景取舍：普通业务类允许优先使用 `@Autowired` 字段注入；强必需依赖、不可变依赖、易测性要求高或需要尽早暴露循环依赖的类优先使用构造器注入。
- 后端日志平台按 `DEBUG / INFO / WARN / ERROR` 四级设计，所有日志必须带 `requestId`、`traceId`、`serviceName`，并支持通过流水号查询一次接口调用的全部日志。
- Kafka 从当前阶段开始纳入日志链路基础设施；Elasticsearch 保存全量热日志 7 天；接口访问摘要写入独立日志 PostgreSQL 并保留 15 天；7 天后的全量日志压缩归档到 OSS。
- SQL 日志语义上统一属于 `DEBUG`，生产环境默认不记录全量 SQL DEBUG，必须通过 Nacos 等动态配置按服务、Mapper、traceId/requestId、慢 SQL 阈值或采样率开启；慢 SQL 和异常 SQL 即使 DEBUG 关闭也必须以 `WARN` 输出摘要。
- 业务接口调用必须记录结构化访问日志，关键业务动作必须记录审计日志；审计日志只保存动作事实、业务主键和脱敏摘要，不保存敏感正文。

当前 B1 后续目标：

- iOS 认证联调先完成 Gateway 入口收口：前端默认或推荐 Base URL 指向 Gateway，本地为 `http://127.0.0.1:18080`。
- iOS 网络层补齐 B1 必需能力：GET/POST、Bearer Token、`X-Request-Id`、`X-Trace-Id`、统一响应 `success/status/code/message/data` 解析和非 2xx 错误体解析。
- iOS 登录和 Token 恢复后必须调用 Gateway `GET /api/users/me` 校验真实当前用户；禁止用 `accountId/userId = 0` 作为运行时已登录会话。
- B1 完整前后端联调安全点以 `docs/integration/B1-auth-ios-backend/integration-plan.md` 为准；当前旧身份模型主链路已手工确认可用，但身份重构后必须重新执行 userId-only 认证联调。
- 在继续大规模业务能力前，优先完成日志平台基础规划和第一阶段代码骨架，避免后续服务重复补日志能力。
- 生成高德地图首页壳、离线地图城市管理壳、门店查询 API 契约和门店服务基础能力。
- 建立 iOS 低风险业务缓存底座，用于弱网时展示最近加载过的门店点位和门店摘要。
- B1.5-b 已补齐 `foodmap-common` 统一 Elasticsearch SearchClient、日志归档导出适配器、common ObjectStorageClient 上传桥接、MinIO/S3 兼容对象存储实现、管理后台日志查询代理 API 和 `LOG_ACCESS_READ` 权限骨架；真实环境联调验收和阿里云 OSS 生产适配器顺延为后续部署/媒体服务阶段任务，不阻塞 B1.5 交付。

## 6. 计划中的仓库结构

推荐未来结构：

```text
food-map
├── .agents
│   ├── frontend-agent.md
│   ├── backend-agent.md
│   ├── api-agent.md
│   ├── qa-agent.md
│   └── docs-agent.md
├── skills
│   ├── foodmap-doc-sync
│   ├── foodmap-backend-service
│   ├── foodmap-ios-feature
│   ├── foodmap-api-contract
│   ├── foodmap-integration-coordination
│   └── foodmap-qa-check
├── harness
│   ├── README.md
│   ├── agent-task-template.md
│   ├── acceptance-checklist.md
│   ├── file-boundary-rules.md
│   ├── api-contract-checklist.md
│   └── scripts
├── deploy
│   ├── README.md
│   ├── architecture.md
│   ├── env.example
│   ├── docker-compose.ecs2.yml
│   ├── nginx
│   └── checklists
├── AGENTS.md
├── CODEX-product.md
├── CODEX-front.md
├── CODEX-after.md
├── CODEX-gen.md
├── after
│   ├── pom.xml
│   ├── foodmap-common
│   ├── foodmap-gateway-service
│   ├── foodmap-auth-service
│   ├── foodmap-user-service
│   ├── foodmap-relation-service
│   ├── foodmap-store-service
│   ├── foodmap-recommendation-service
│   ├── foodmap-community-service
│   ├── foodmap-media-service
│   ├── foodmap-admin-service
│   ├── foodmap-log-service
│   └── docker-compose.yml
├── front
│   └── FoodMapApp
└── docs
    ├── api
    ├── integration
    │   ├── README.md
    │   ├── templates
    │   │   ├── integration-plan.md
    │   │   └── issue-log.md
    │   └── <iteration>-<feature>
    │       ├── integration-plan.md
    │       └── issue-log.md
    ├── database
    └── design
```

## 6.1 前后端联调文档生成规则

每次进入前后端联调前，必须在 `docs/integration/<iteration>-<feature>/` 下生成两个核心文件：

- `integration-plan.md`：联调说明，明确功能范围、前后端职责、接口契约、测试数据、验收标准、环境和提交记录。
- `issue-log.md`：问题记录，记录联调失败、测试数据、前端现象、后端日志摘要、复现步骤、初步归因、修复提交和复测结论。

联调说明必须作为验收标准来源。前端、后端和 QA 子代理在联调时都必须按该文件检查交付代码，不能只凭口头描述判定通过。

联调安全点必须作为正式联调准入门禁。`integration-plan.md` 需要明确联调等级和准入检查，包括 API 契约、后端可调用、前端可触发、环境可运行、测试数据可准备、`requestId/traceId` 可追踪和本次范围已冻结。

联调等级统一使用：

- `L0 契约联调`：确认 API、DTO、字段、枚举和错误码。
- `L1 Mock 联调`：前端使用 mock 或后端占位响应验证页面流程。
- `L2 本地真实联调`：前端调用本地真实后端服务。
- `L3 环境联调`：在 OrbStack、测试环境或 prod-like 环境跑完整服务链路。
- `L4 验收联调`：包含日志证据、测试数据、BUG 复测和最终结论。

正式业务联调原则上至少从 `L2` 开始。权限、隐私、Token、可见范围、PUBLIC 统计、上传和支付类高风险能力必须推进到 `L3` 或 `L4` 才能认为完成。

验收判定统一使用：

- `通过`：必测场景全部执行并有证据，阻塞级和高风险 BUG 已关闭。
- `有条件通过`：主流程可用，仅存在明确记录的非阻塞问题。
- `不通过`：主流程失败，或出现权限、隐私、Token、可见范围、PUBLIC 统计口径等高风险问题。
- `无法判定`：缺少环境、构建、测试、接口调用、截图/录屏、日志等关键证据。

联调失败时，必须在 `issue-log.md` 中新增 BUG 条目。阻塞级和高风险 BUG 修复后必须记录复测结果，才能把本次联调标记为通过。

## 7. 开发策略

项目应尽量按纵向业务切片推进。

默认推进方式是“开发到联调安全点，再决定是否继续下一个功能”。每完成一个前端或后端能力，主代理必须判断：

- 当前能力是否形成可联调业务切片。
- 是否应该暂停继续开发强依赖的下一个功能，先进入联调。
- 前端需要达到的最小程度是什么。
- 后端需要达到的最小程度是什么。
- 如果跳过联调继续开发，可能带来的契约、权限、状态或数据风险是什么。

示例：后端完成登录能力后，应提示前端至少具备 `LoginView + APIClient + Token 保存 + 登录后跳转` 再进入 B1 认证联调；后端完成地图视野查询后，应提示前端至少具备地图首页壳、bbox 获取、真实请求和点位/列表展示再进入门店地图联调。

B1 认证联调当前安全点拆分：

- 旧身份模型安全点已到达：Gateway/Auth/User 已完成注册、登录、当前用户、内部接口拦截、accountId 归属校验和注册失败回滚的本地 L2 验证。
- 新身份模型 common/gateway 安全点已到达：Token 编解码、CurrentUser 解析和 Gateway 可信身份头已支持 `userId-only`，并兼容解析旧 `accountId + userId` Token。
- 新身份模型完整 L2 安全点未到达：auth-service 签发链路、user-service 当前用户接口、认证身份绑定、认证会话和 iOS 模型仍需继续收口；当前 `foodmap-user-service` 仍存在旧 `X-FoodMap-Account-Id` 必填依赖，不能直接进入完整 userId-only 认证联调。
- 下一步应先完成 auth-service + user-service userId-only 重构，再执行 userId-only 完整 iOS L2 联调。

推荐顺序：

1. 文档基础建设。
2. 后端基础设施骨架。
3. 认证服务和用户服务。
4. iOS App 壳和认证流程。
5. 门店服务和高德集成占位。
6. iOS 地图壳。
7. iOS 当前城市离线底图和缓存摘要降级。
8. 推荐服务。
9. iOS 添加推荐流程。
10. 关系服务。
11. iOS 好友和情侣流程。
12. 社区服务和公开统计。
13. 媒体服务和图片上传。

这个顺序可以让项目较早变得可用，同时保持微服务边界清晰。

## 8. 后端生成计划

### B0：后端骨架

交付物：

- after/pom.xml
- foodmap-common
- 各服务模块
- 基础 Spring Boot 应用
- Docker Compose 基础配置
- `application-{profile}.yml` 分文件环境配置
- 标准包结构

服务：

- gateway
- auth
- user
- relation
- store
- recommendation
- community
- media

### B1：认证和用户

交付物：

- 注册接口。
- 登录接口。
- JWT 工具。
- Refresh Token / auth session 持久化，DB 保存会话事实，Redis 用于热缓存和撤销加速。
- 用户资料接口。
- 注册成功后通过用户服务内部接口开通用户主体、用户资料和默认设置。
- 服务独立数据库。
- 身份重构目标：移除 `accountId` 长期主体，新增认证身份绑定、长期凭证和认证会话模型。

### B1.5：日志平台基础能力

交付物：

B1.5-a 已作为当前小阶段执行，目标是先让所有后端服务具备统一链路上下文和访问日志基础能力：

- `requestId`、`traceId`、`spanId` 生成、校验和跨服务透传规则。
- 网关 `GatewayTraceFilter`，在认证过滤器前生成或透传链路头，并写回响应头。
- Servlet 业务服务 `LogMdcFilter`，把链路头、服务名和可信用户身份写入 MDC。
- `ApiAccessLogFilter` 服务内接口访问摘要，记录 `api.access.completed` 和 `api.access.slow`。
- `SafeLog` 增强，自动附带 MDC 中的 `requestId`、`traceId`、`spanId`、`serviceName`、`userId`；`accountId` 仅作为旧身份模型兼容字段。
- 日志架构图、实现原理和阶段边界同步到 `CODEX-after.md`。

B1.5-b 后续依次展开，目标是把 B1.5-a 的日志上下文接入完整日志平台：

- MyBatis SQL 日志拦截器，支持实际参数替换、脱敏、慢 SQL `WARN` 和 DEBUG 配置模型。
- SQL 日志配置已具备 Environment 动态重读层，并接入 Nacos Config 可选导入；支持按服务、Mapper、traceId/requestId 和采样率临时打开。
- Kafka 日志 topic 规划和本地 Docker Compose `logging` profile 已落地，当前阶段作为日志缓冲管道，不要求业务服务直接写 Kafka。
- Fluent Bit 本地采集器已纳入 Docker Compose `logging` profile，通过共享日志卷读取应用日志，并按 `api.access.*`、`sql.execute.*`、`audit.*`、`security.*` 前缀分流到 Kafka topic。
- Elasticsearch 本地热查询基础已纳入 Docker Compose `logging` profile，并通过 `elasticsearch-init` 写入 7 天 ILM 策略和 `foodmap-logs-*` 索引模板；Logstash 本地消费器已消费五类 Kafka topic 并写入每日 `foodmap-logs-*` 索引。
- 独立日志 PostgreSQL `foodmap_log_db`、`api_access_log` Flyway 迁移、`foodmap-log-service` 接口访问摘要消费落库、内部查询 API、15 天保留清理任务、`log_archive_records` 归档计划表、归档执行器状态机骨架、`foodmap-common` 统一 Elasticsearch SearchClient、真实日志归档导出适配器、common ObjectStorageClient 上传桥接和 MinIO/S3 兼容对象存储实现已落地。
- 业务审计日志定义和首批覆盖动作清单。
- 管理后台 `/api/admin/logs/**` 查询代理接口和 `LOG_ACCESS_READ` 权限骨架。
- B1.5-b 收尾结论：日志平台代码骨架、后台查询代理和权限骨架已完成；真实环境联调验收需要依赖本地/测试环境 Elasticsearch、Kafka、PostgreSQL、MinIO 联合启动，顺延为部署验收任务；阿里云 OSS 具体适配器按生产部署和媒体服务节奏后移。

B1.5 后续每个小阶段交付时，必须同步补齐：

- 关键技术说明。
- 实现原理说明。
- Mermaid 架构图或流程图。
- 运维风险、默认开关和排查入口。

### B2：门店

交付物：

- 门店表结构。
- 门店创建接口。
- 门店搜索接口。
- 高德 POI 集成占位。
- 门店详情接口。
- 地图视野查询接口。

### B3：推荐

交付物：

- 推荐表结构。
- 推荐创建/编辑/删除接口。
- 标签。
- 图片引用。
- 评论表结构。
- 评论列表、发布、删除接口。
- 评论图片引用，单条评论最多 3 张图片。
- 可见范围。
- 可见规则。

### B4：关系

交付物：

- 好友申请。
- 好友关系。
- 情侣申请。
- 情侣关系。
- 内部关系校验接口。

### B5：社区

交付物：

- 推荐事件。
- 事件消费者。
- 公开门店统计。
- 公开菜品统计。
- 热门门店接口。
- 附近公开接口。

### B6：媒体

交付物：

- 上传接口。
- 图片元数据。
- MinIO/OSS 集成占位。
- 媒体引用校验。

## 9. 前端生成计划

### F0：iOS App 壳

交付物：

- SwiftUI App 项目。
- 根路由。
- 基础 TabView。
- 设计系统基础。

### F1：认证

交付物：

- 登录页。
- 注册页。
- API Client。
- Token 存储。
- 认证会话路由。

### F2：地图

交付物：

- 高德地图集成。
- 地图首页。
- 地图范围选择器。
- 点位渲染。
- 门店预览。

### F3：添加推荐

交付物：

- 门店搜索页。
- 添加推荐表单。
- 可见范围选择器。
- 标签选择器。
- 图片选择占位。

### F4：门店详情

交付物：

- 门店详情页。
- 可见推荐列表。
- 公开统计展示。

### F5：社交

交付物：

- 好友列表。
- 好友搜索。
- 好友申请处理。
- 情侣绑定。
- 情侣地图入口。

### F6：社区

交付物：

- 热门门店。
- 热门菜品。
- 附近公开列表。

## 10. 数据和 API 契约规则

在生成会调用后端 API 的前端页面之前：

1. 后端 API 契约必须先文档化。
2. 请求和响应 DTO 在当前迭代内应足够稳定。
3. 前端模型必须与 API DTO 匹配。
4. 当后端服务尚未实现时，可以临时使用 Mock 数据。
5. 后端 API 实现后，应替换 Mock 数据。

## 11. 测试策略

### 10.1 后端测试

预期测试：

- 领域规则单元测试。
- 用例服务测试。
- 关键 SQL 的 Repository 测试。
- Controller API 测试。
- 可见范围集成测试。

重点测试区域：

- 认证。
- 可见权限。
- 好友/情侣关系校验。
- 社区统计。

### 10.2 前端测试

预期测试：

- ViewModel 测试。
- API Client 测试。
- 重要页面的快照或 UI 测试，条件允许时执行。

重点测试区域：

- 认证会话状态。
- 推荐草稿状态。
- 可见范围选择器。
- 地图筛选。

## 12. 文档更新示例

### 示例一：提前加入群组共享

需要更新：

- CODEX-product.md：MVP 范围和群组用户流程。
- CODEX-front.md：群组页面和导航。
- CODEX-after.md：关系服务群组接口和表结构。
- CODEX-gen.md：里程碑顺序。

### 示例二：更换消息队列

需要更新：

- CODEX-after.md：技术栈和事件流。
- CODEX-gen.md：后端生成计划。

### 示例三：增加评论功能

需要更新：

- CODEX-product.md：评论行为和可见范围。
- CODEX-front.md：评论 UI。
- CODEX-after.md：新增服务或扩展推荐服务。
- CODEX-gen.md：里程碑位置。

## 13. 初始实现建议

完成文档基础建设后，下一步推荐：

```text
优先实现认证服务和用户服务基础能力。
```

原因：

- 认证和用户能力是登录、好友、情侣、推荐归属的基础。
- API 契约会指导后续 `front/` iOS App 开发。
- 认证、用户、门店、推荐服务构成项目主干。

认证和用户基础能力完成后：

```text
生成 iOS App 壳。
```

部署准备：

```text
MVP 阶段使用 Docker Compose 部署到 ECS2，ECS1 作为辅助节点。
两台服务器不在同一云厂商或 VPC 时，不通过裸公网访问 Redis、数据库、Nacos、RabbitMQ。
```

## 14. 当前里程碑状态

| 里程碑 | 状态 |
| --- | --- |
| 文档基础建设 | 已完成 |
| AGENTS 协作规范 | 已完成 |
| 多代理子规范 | 已完成 |
| 项目专属 Skills | 已完成 |
| Harness 约束和验收脚手架 | 已完成 |
| 部署方案和模板 | 已完成 |
| 后端骨架 | 已完成 |
| 本地隔离开发环境配置 | 已完成 |
| 数据库设计规则 | 已完成 |
| MVP 首批表结构草案 | 已完成 |
| 推荐评论体系设计 | 已完成 |
| 后端微服务企业级开发基线 | 已完成 |
| 阶段一中间件封装和统一日志规则 | 已完成 |
| 阶段一 foodmap-common 首批基线代码 | 已完成 |
| 后端代码注释规则和首批注释补齐 | 已完成 |
| 阶段一 common.validation.Check 校验工具 | 已完成 |
| 后端日志平台设计规划 | 已完成 |
| 后端日志平台基础能力 B1.5-a | 已完成 |
| 后端日志平台基础能力 B1.5-b | 已完成 |
| iOS App 壳 | 未开始 |
| 认证/用户接口 | 未开始 |
| 地图壳 | 未开始 |
| 推荐流程 | 未开始 |
| 关系流程 | 未开始 |
| 社区统计 | 未开始 |
| 媒体上传 | 未开始 |

## 15. 项目待决策问题

1. 使用 Maven 还是 Gradle。当前建议：Maven。
2. 业务事件使用 RocketMQ 还是 RabbitMQ。当前建议：如果采用 Spring Cloud Alibaba，优先 RocketMQ；如果优先简化本地环境，使用 RabbitMQ。日志链路已决策引入 Kafka，不与业务事件队列混用。
3. 使用 MyBatis 还是 MyBatis-Plus。已决策：统一使用 MyBatis + Mapper.xml，项目内建设标准 CRUD 模板能力，复杂 SQL 使用 DefineMapper/XML 控制。
4. 最低支持 iOS 版本。
5. MVP 是否展示社区 Tab。
