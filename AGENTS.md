# AGENTS.md

## 1. 文档目的

本文档用于指导后续参与 FoodMap 项目的开发代理、工程师和自动化生成流程。

任何前端、后端、文档或基础设施变更，都必须先阅读并遵守：

- CODEX-product.md
- CODEX-front.md
- CODEX-after.md
- CODEX-gen.md
- AGENTS.md
- `.agents/*.md`
- `skills/*/SKILL.md`
- `harness/*.md`

如果本文档与 CODEX 文档存在冲突，以 CODEX 产品和架构文档为准，并在本次迭代中同步修正冲突内容。

## 2. 项目基本方向

FoodMap 是一款 iOS 优先的美食推荐地图 App。

核心能力：

- 用户可以在地图上记录门店。
- 用户可以给门店添加推荐菜单。
- 推荐菜单名称必须是纯文字。
- 用户可以可选上传图片、添加标签、填写推荐理由。
- 推荐内容支持不同可见范围。
- 好友、情侣可以查看彼此授权的推荐内容。
- 只有全部公开的推荐进入全站社区统计。

## 3. 通用开发规则

1. 先读文档，再写代码。
2. 产品行为变化时，先更新 CODEX-product.md。
3. 前端架构、页面、SDK、交互变化时，先更新 CODEX-front.md。
4. 后端服务、API、数据库归属、基础设施变化时，先更新 CODEX-after.md。
5. 迭代顺序、验收标准、协作规则变化时，先更新 CODEX-gen.md 或 AGENTS.md。
6. 代码必须符合文档，不允许代码和文档长期不一致。
7. 每次重要迭代完成后，需要提交并推送到 GitHub 仓库。
8. 不允许使用破坏性 git 操作丢弃用户改动，除非用户明确要求。

## 4. 多代理开发模式

### 4.1 使用原则

FoodMap 支持“主代理 + 子代理”的多代理开发模式。

启用条件：

- 用户明确要求使用多代理、子代理、并行开发或任务分派。
- 当前任务足够大，可以拆成互不冲突的子任务。
- 子任务有清晰的输入、输出和文件写入范围。

不启用条件：

- 任务很小，主代理可以直接完成。
- 子任务之间高度耦合，拆分会增加冲突。
- 下一步关键路径必须依赖某个结果，主代理应优先本地完成该关键任务。

### 4.2 主代理职责

主代理负责整体协调和最终质量。

职责：

- 理解用户目标。
- 阅读并维护 CODEX 文档和 AGENTS.md。
- 拆分任务并定义子代理边界。
- 指定每个子代理的文件写入范围。
- 避免多个子代理修改同一批文件。
- 审核子代理产出。
- 整合代码和文档。
- 运行测试或构建。
- 提交并推送 GitHub。

主代理不能把最终产品判断、架构判断和验收责任完全转交给子代理。

### 4.3 子代理通用规则

所有子代理必须遵守：

- 必须先阅读与自己任务相关的 CODEX 文档和 AGENTS.md。
- 必须只修改被明确授权的文件或目录。
- 必须知道自己不是唯一开发者，不能回滚或覆盖他人改动。
- 必须在完成后说明修改了哪些文件。
- 必须说明已执行或无法执行的验证动作。
- 不能擅自改变产品方向、服务边界、技术栈或数据库归属。

### 4.4 推荐子代理类型

| 子代理 | 主要职责 | 规范文件 |
| --- | --- | --- |
| 前端子代理 | iOS、SwiftUI、页面、状态管理、高德地图集成 | .agents/frontend-agent.md |
| 后端子代理 | Java 微服务、数据库、API、权限、事件 | .agents/backend-agent.md |
| 接口契约子代理 | API、DTO、前后端联调契约 | .agents/api-agent.md |
| 测试验收子代理 | 构建、测试、验收标准、风险检查 | .agents/qa-agent.md |
| 文档子代理 | CODEX 文档、AGENTS.md、迭代规则同步 | .agents/docs-agent.md |

### 4.5 文件边界规则

拆分任务时必须明确写入范围。

示例：

```text
前端子代理：
只允许修改 front/FoodMapApp/Features/Auth 和 front/FoodMapApp/Core/Networking。

后端认证子代理：
只允许修改 after/foodmap-auth-service 和 after/foodmap-common。

后端门店子代理：
只允许修改 after/foodmap-store-service。

接口契约子代理：
只允许修改 docs/api 和相关 DTO 文件。

文档子代理：
只允许修改 CODEX-*.md、AGENTS.md 和 .agents/*.md。
```

如果任务确实需要跨范围修改，必须由主代理重新授权并协调。

### 4.6 多代理交付流程

推荐流程：

1. 主代理阅读文档并拆分任务。
2. 主代理明确每个子代理的目标、输入、输出、验收标准和写入范围。
3. 子代理并行处理互不冲突的任务。
4. 主代理在等待期间处理关键路径或非重叠工作。
5. 子代理完成后，主代理审核文件和验证结果。
6. 主代理整合变更并解决冲突。
7. 主代理执行测试或构建。
8. 主代理提交并推送。

## 5. 项目专属 Skills

FoodMap 使用项目内 skills 固化重复开发流程。

skills 目录：

```text
skills
├── foodmap-doc-sync
├── foodmap-backend-service
├── foodmap-ios-feature
├── foodmap-api-contract
└── foodmap-qa-check
```

使用规则：

- 需求、架构、验收标准变化时，使用 `foodmap-doc-sync`。
- 创建或修改 Java 微服务时，使用 `foodmap-backend-service`。
- 创建或修改 iOS/SwiftUI 功能时，使用 `foodmap-ios-feature`。
- 设计或校验 API、DTO、前后端模型时，使用 `foodmap-api-contract`。
- 构建、测试、验收、GitHub 同步前检查时，使用 `foodmap-qa-check`。

这些 skills 必须与 CODEX 文档、AGENTS.md 和 `.agents/*.md` 保持一致。

## 6. Harness 开发约束

FoodMap 使用 `harness/` 作为多代理开发的约束和验收脚手架。

harness 目录：

```text
harness
├── README.md
├── agent-task-template.md
├── acceptance-checklist.md
├── file-boundary-rules.md
├── api-contract-checklist.md
└── scripts
    ├── run-all.sh
    ├── validate-docs.sh
    ├── validate-backend.sh
    ├── validate-logging.sh
    ├── validate-ios.sh
    ├── validate-api.sh
    └── validate-git.sh
```

使用规则：

- 主代理分派子代理任务时，应参考 `harness/agent-task-template.md`。
- 多代理并行开发时，必须遵守 `harness/file-boundary-rules.md`。
- API 和 DTO 相关任务必须参考 `harness/api-contract-checklist.md`。
- 每次重要迭代结束前，必须参考 `harness/acceptance-checklist.md`。
- 条件允许时，执行 `./harness/scripts/run-all.sh`。

harness 当前是轻量阶段。`after/` 已生成后会执行 Maven 基础校验；`front/` 尚未生成时，脚本会跳过对应 iOS 代码检查；等工程落地后再逐步增强。

### 6.1 前后端联调文档约束

FoodMap 每次进入前后端联调前，必须在 `docs/integration/` 下生成独立联调文件夹。

目录格式：

```text
docs/integration/<iteration>-<feature>/
├── integration-plan.md
└── issue-log.md
```

`integration-plan.md` 必须写明：

- 本次联调的功能和所属迭代。
- 前端职责：页面、交互、状态、网络请求、截图或录屏证据。
- 后端职责：接口、入参、响应、错误码、服务边界、日志要求。
- API 契约、测试数据、联调环境和验收场景。
- 相关提交记录，条件允许时记录前端提交、后端提交和文档提交。
- 验收判定口径：通过、有条件通过、不通过、无法判定。

`issue-log.md` 必须写明：

- 联调问题、严重级别、状态、责任侧和关联场景。
- 复现步骤和测试数据。
- 前端现象、网络请求摘要、后端日志摘要、`requestId`、`traceId`。
- 初步分析、建议修复范围、修复提交和复测结论。

联调阶段默认启用主代理 + 子代理模式：

- 主代理负责流程推进、文件夹创建、问题分派和最终验收。
- 后端观察子代理负责观察后端项目情况、接口契约、日志和服务状态。
- 前端观察子代理负责观察 iOS 页面、交互、状态展示和网络请求。
- 联调验收子代理负责按 `integration-plan.md` 和 `issue-log.md` 判定是否通过。

子代理默认只读观察和反馈。需要修改代码时，必须由主代理按 `harness/agent-task-template.md` 重新明确写入范围。

## 7. 前端 Agent

### 7.1 前端职责

前端 Agent 负责 iOS App 的页面、交互、状态管理、网络请求、地图展示和用户体验。

前端实现必须满足：

- iOS 原生 App。
- 以地图作为核心入口。
- 用户登录后优先进入地图页。
- 推荐添加流程轻量、清晰。
- 可见范围选择必须明确，避免用户误公开。
- 前端只能展示后端返回的可见数据，不能在本地绕过权限。

### 7.2 前端技术栈

| 类别 | 技术 |
| --- | --- |
| 平台 | iOS |
| 语言 | Swift |
| UI | SwiftUI |
| 架构 | MVVM |
| 地图 | 高德 iOS SDK |
| 网络 | URLSession 优先，后续可评估 Alamofire |
| 状态管理 | SwiftUI Observable / ViewModel |
| Token 存储 | Keychain |
| 简单偏好 | UserDefaults |
| 图片加载 | Kingfisher 或 Nuke，后续决策 |
| 本地缓存 | MVP 支持高德离线底图和低风险门店摘要缓存；不离线展示社交推荐详情和评论 |

### 7.3 前端目录约定

推荐结构：

```text
front/FoodMapApp
├── App
├── Core
│   ├── Networking
│   ├── Auth
│   ├── Storage
│   ├── DesignSystem
│   ├── Extensions
│   └── Utilities
├── Features
│   ├── Auth
│   ├── Map
│   ├── Store
│   ├── Recommendation
│   ├── Friends
│   ├── Couple
│   ├── Community
│   └── Profile
└── Resources
```

### 7.4 前端页面清单

#### 7.4.1 认证页面

页面：

- LoginView
- RegisterView
- ForgotPasswordView 占位

验收标准：

- 支持手机号、邮箱或账号名登录入口。
- 登录成功后进入地图页。
- Token 存储在 Keychain。
- 登录失败展示明确错误。
- 不在本地保存明文密码。

#### 7.4.2 地图页面

页面：

- MapHomeView
- MapFilterView
- StoreMarkerPreviewView

验收标准：

- 地图页是登录后的主要入口。
- 能展示高德地图。
- 能根据后端返回数据展示门店点位。
- 支持地图范围切换：我的、好友、情侣、公开。
- 支持基础筛选：标签、关键词、推荐程度、价格范围。
- 点击门店点位后展示门店摘要。
- 地图页有清晰的添加推荐入口。

#### 7.4.3 门店页面

页面：

- StoreSearchView
- StoreCreateView
- StoreDetailView

验收标准：

- 支持关键词搜索门店。
- 能展示高德 POI 或后端门店搜索结果。
- 搜不到门店时支持手动创建门店。
- 门店详情展示名称、地址、公开统计和当前用户可见推荐。
- 不展示用户无权限查看的推荐内容。

#### 7.4.4 推荐菜单页面

页面：

- AddRecommendationView
- EditRecommendationView
- RecommendationDetailView
- RecommendationCommentsView
- CommentComposerView
- VisibilityPickerView
- TagPickerView

验收标准：

- 添加推荐时菜名为必填纯文字。
- 推荐理由、图片、标签、价格、推荐程度为可选。
- 推荐详情支持查看评论列表。
- 用户可以发布文字评论，并可选上传最多 3 张评论图片。
- 评论人昵称展示使用后端返回的评论昵称快照。
- 可见范围必须在提交前明确选择。
- 可见范围至少支持：仅自己、指定用户、好友、情侣、群组、全部公开。
- 图片上传失败时保留已填写文本，并允许重试。
- 提交成功后能回到门店详情或地图页并看到更新结果。

#### 7.4.5 好友页面

页面：

- FriendsView
- FriendSearchView
- FriendRequestsView

验收标准：

- 支持搜索用户。
- 支持发送好友申请。
- 支持接受或拒绝好友申请。
- 支持删除好友。
- 好友可见推荐只能展示后端返回的授权内容。

#### 7.4.6 情侣页面

页面：

- CoupleHomeView
- CoupleBindView
- CoupleMapView

验收标准：

- 支持从好友中发起情侣绑定。
- 支持接受或拒绝情侣申请。
- 支持解除情侣关系。
- 情侣地图只展示双方授权为情侣可见的内容。
- 同一时间一个用户只能有一个有效情侣关系。

#### 7.4.7 社区页面

页面：

- CommunityHomeView
- HotStoresView
- HotDishesView
- NearbyPublicView

验收标准：

- 展示公开热门门店。
- 展示公开热门菜品。
- 展示附近公开推荐。
- 社区页面只展示全部公开的内容。
- 不能将好友、情侣、指定用户或群组内容混入公开社区。

#### 7.4.8 我的页面

页面：

- ProfileView
- MyRecommendationsView
- PrivacySettingsView
- AccountSettingsView

验收标准：

- 支持查看和编辑个人资料。
- 支持查看我的推荐。
- 支持管理默认隐私设置。
- 支持退出登录并清理本地认证状态。

### 7.5 前端通用验收标准

每个前端迭代至少满足：

- 页面符合 CODEX-front.md 中的模块和导航约定。
- 关键页面具备加载中、空状态、错误、无权限、网络不可用状态。
- 网络请求通过统一 APIClient 发起。
- Token 不出现在日志中。
- 私密推荐内容不出现在日志中。
- ViewModel 逻辑可测试。
- 条件允许时执行 iOS 构建或相关测试。

## 8. 后端 Agent

### 8.1 后端职责

后端 Agent 负责 Java 微服务、API、数据库、权限校验、事件、缓存和基础设施。

后端实现必须满足：

- Java 微服务架构。
- 每个服务拥有独立数据库。
- 服务不能直接访问其他服务的数据表。
- 库表结构必须通过 Flyway 迁移脚本管理。
- 每张业务表必须包含 `id / created_time / updated_time / is_delete` 固定字段。
- 主业务表必须包含 `bigint` 类型业务主键，跨服务引用使用业务主键而不是自增 `id`。
- 持久化业务主键不能使用服务内存计数器生成；当前 PostgreSQL 服务优先使用 Flyway 管理的数据库 sequence。
- 数据库结构对应 Java 类必须放在服务内 `infrastructure.persistence.entity` 包中，并与 DTO、VO 明确区分。
- `foodmap-common` 的 `BaseEntity` 只承载 `id / created_time / updated_time / is_delete` 固定字段，不承载业务主键。
- Controller 只能使用 DTO 作为请求和响应契约，不能直接暴露数据库持久化实体。
- service 层必须使用 `XxxService` 接口 + `XxxServiceImpl` 实现类，Controller 只能依赖 `XxxService` 接口。
- ServiceImpl 只能依赖仓储端口接口，不能直接依赖内存仓储、MyBatis Mapper 等基础设施实现。
- 内存仓储只允许作为单元测试或本地替身，不作为生产 profile 默认持久化实现。
- 外部请求通过 API Gateway 进入。
- 推荐内容的可见范围由服务端强制校验。
- 社区统计只统计 PUBLIC 推荐。

### 8.2 后端技术栈

| 类别 | 技术 |
| --- | --- |
| 语言 | Java 21 |
| 框架 | Spring Boot |
| 微服务 | Spring Cloud / Spring Cloud Alibaba |
| 网关 | Spring Cloud Gateway |
| 注册中心 | Nacos |
| 配置中心 | Nacos |
| 鉴权 | Spring Security + JWT |
| ORM | MyBatis + Mapper.xml |
| 数据库 | PostgreSQL |
| 地理能力 | PostgreSQL + PostGIS |
| 数据库连接池 | HikariCP |
| 数据库迁移 | Flyway |
| 缓存 | Redis |
| 分布式锁适配器 | Redisson，限定在 `DistributedLockClient` 基础设施实现内使用 |
| 消息队列 | RocketMQ 或 RabbitMQ |
| 对象存储 | MinIO 或阿里云 OSS |
| 日志热查询 | Elasticsearch |
| 日志缓冲管道 | Kafka |
| 日志采集 | Fluent Bit 本地采集器；生产可按部署形态评估 OpenTelemetry Collector / Filebeat |
| 日志热写入 | Logstash 本地消费器；生产可按规模评估自研消费者或 Kafka Connect |
| 日志摘要写入 | foodmap-log-service，消费 api-access topic 并写入独立日志库 |
| 日志归档 | OSS + 独立日志 PostgreSQL |
| 日志摘要库 | foodmap_log_db，保存 api_access_log 等结构化摘要 |
| API 文档 | OpenAPI + Knife4j |
| 构建 | Maven |
| 容器 | Docker |
| 本地环境 | Docker Compose / OrbStack / Spring Profiles |
| 监控 | Actuator、Prometheus、Grafana |
| 链路追踪 | OpenTelemetry 或 SkyWalking |

部署约束：

- MVP 使用 Docker Compose，不使用 Kubernetes。
- ECS2 作为主应用服务器。
- ECS1 作为辅助服务器。
- 两台服务器不在同一云厂商/VPC 时，不通过裸公网访问 Redis、PostgreSQL、Nacos、RabbitMQ。
- 如需跨服务器访问内部组件，必须先建立 WireGuard/VPN。
- 阿里云 OSS 用于图片和头像。
- 本地服务 profile 使用 `SPRING_PROFILES_ACTIVE > FOODMAP_PROFILE > local` 的优先级。
- IDEA/Maven 本机启动默认使用 `local`；Docker/OrbStack 容器网络启动使用 `orbstack`；生产使用 `prod`。

数据库设计约束：

- `id` 使用 `bigint generated by default as identity`，只作为服务内部数据库主键。
- 固定字段统一为 `created_time timestamptz`、`updated_time timestamptz`、`is_delete smallint default 0`。
- 数据库表结构设计必须先在 `CODEX-after.md` 中补充表中文名、表用途、字段类型和字段中文注释。
- 后续生成 Flyway SQL 时，表和字段必须使用 PostgreSQL `comment on table` / `comment on column` 固化中文注释。
- 持久化实体字段必须提供字段级 Javadoc；如果字段对应数据库字段，注释必须与表字段中文注释一一对应。
- 展示型金额字段使用 PostgreSQL `numeric(10,2)`，Java 使用 `BigDecimal`。
- 真实交易金额如后续出现订单或钱包，再使用 `bigint` 保存最小货币单位。
- 门店地理位置由门店服务负责，地理查询使用 PostGIS `geography(Point, 4326)`。

### 8.3 后端目录约定

推荐结构：

```text
after
├── pom.xml
├── foodmap-common
├── foodmap-gateway-service
├── foodmap-auth-service
├── foodmap-user-service
├── foodmap-relation-service
├── foodmap-store-service
├── foodmap-recommendation-service
├── foodmap-community-service
├── foodmap-media-service
├── foodmap-admin-service
├── foodmap-log-service
└── docker-compose.yml
```

单个服务推荐结构：

```text
service-name
├── src/main/java
│   └── com/foodmap/{service}
│       ├── controller
│       ├── application
│       ├── domain
│       ├── infrastructure
│       ├── mapper
│       ├── dto
│       ├── config
│       └── ServiceApplication.java
├── src/main/resources
│   ├── application.yml
│   ├── application-local.yml
│   ├── application-orbstack.yml
│   ├── application-prod.yml
│   └── mapper
└── pom.xml
```

### 8.4 后端服务清单

#### 8.4.1 网关服务

服务：

- foodmap-gateway-service

数据库：

- 默认无数据库

验收标准：

- 能路由外部 API 到内部服务。
- 能通过 Nacos + Spring Cloud LoadBalancer 访问已注册服务。
- 骨架阶段可通过 `/internal/**/health` 验证网关到服务的内部路由。
- 支持基础 JWT 前置校验。
- 支持 CORS。
- 支持请求日志。
- 支持基础限流配置占位。
- 不承载业务数据。

#### 8.4.2 认证服务

服务：

- foodmap-auth-service

数据库：

- foodmap_auth_db

核心表：

- auth_accounts
- auth_credentials
- refresh_tokens
- login_logs

验收标准：

- 支持注册。
- 支持手机号、邮箱、账号名登录。
- 密码使用强哈希保存。
- 支持 Access Token 和 Refresh Token。
- Refresh Token 可撤销。
- 不管理用户详细资料。

#### 8.4.3 用户服务

服务：

- foodmap-user-service

数据库：

- foodmap_user_db

核心表：

- users
- user_profiles
- user_settings

验收标准：

- 支持查询当前用户资料。
- 支持更新昵称、头像、城市、简介。
- 支持按允许字段搜索用户。
- 不保存密码和登录凭证。

#### 8.4.4 关系服务

服务：

- foodmap-relation-service

数据库：

- foodmap_relation_db

核心表：

- friend_requests
- friend_relations
- couple_requests
- couple_relations
- groups
- group_members
- blocked_users

验收标准：

- 支持好友申请、接受、拒绝、删除。
- 支持情侣申请、接受、拒绝、解除。
- 同一用户同一时间只能有一个有效情侣关系。
- 提供内部接口校验好友、情侣、群组成员关系。
- 不向其他服务暴露关系表直接访问权限。

#### 8.4.5 门店服务

服务：

- foodmap-store-service

数据库：

- foodmap_store_db

数据库能力：

- PostgreSQL + PostGIS

核心表：

- stores
- store_pois
- store_aliases
- store_merge_records

验收标准：

- 支持高德 POI 搜索集成或占位实现。
- 支持手动创建门店。
- 支持查询门店详情。
- 支持地图视野范围内查询门店。
- 保存经纬度、城市编码、高德 POI ID。
- 推荐服务只能通过 storeId 引用门店。

#### 8.4.6 推荐服务

服务：

- foodmap-recommendation-service

数据库：

- foodmap_recommendation_db

核心表：

- recommendations
- recommendation_images
- recommendation_comments
- recommendation_comment_images
- tags
- recommendation_tags
- visibility_rules

验收标准：

- 支持创建、编辑、删除推荐菜单。
- 菜名必填，并以纯文字保存。
- 推荐理由、图片、标签、价格、推荐程度为可选。
- 支持推荐菜单评论。
- 评论表冗余评论人昵称快照。
- 单条评论最多支持 3 张图片。
- 支持 PRIVATE、SPECIFIC_USERS、FRIENDS、COUPLE、GROUP、PUBLIC 可见范围。
- 所有推荐查询都必须校验当前用户可见权限。
- 评论查询和评论发布也必须校验推荐内容可见权限。
- 创建或变更 PUBLIC 推荐时发送领域事件。
- 不直接访问关系服务数据库。

#### 8.4.7 社区服务

服务：

- foodmap-community-service

数据库：

- foodmap_community_db

核心表：

- public_store_stats
- public_dish_stats
- public_recommendation_index
- community_feed_items

验收标准：

- 消费推荐服务发送的领域事件。
- 只统计 visibility_type = PUBLIC 且 status = NORMAL 的推荐。
- 支持公开热门门店。
- 支持公开热门菜品。
- 支持附近公开推荐。
- 不统计 PRIVATE、SPECIFIC_USERS、FRIENDS、COUPLE、GROUP 内容。

#### 8.4.8 媒体服务

服务：

- foodmap-media-service

数据库：

- foodmap_media_db

核心表：

- media_files
- media_usage_refs

验收标准：

- 支持生成上传凭证。
- 支持 MVP 后端直传占位或对象存储直传。
- 校验图片类型和大小。
- 保存媒体元数据。
- 返回可访问 URL 或签名 URL。
- 支持推荐图片和评论图片的媒体引用校验。

#### 8.4.9 通知服务

服务：

- foodmap-notification-service

阶段：

- 后续实现

验收标准：

- 支持好友申请通知。
- 支持情侣绑定通知。
- 支持群组邀请通知。
- 支持系统通知。

#### 8.4.10 管理后台服务

服务：

- foodmap-admin-service

阶段：

- 后续实现

验收标准：

- 支持公开内容审核。
- 支持举报处理。
- 支持门店合并操作。
- 支持用户状态管理。

#### 8.4.11 日志平台服务

服务：

- foodmap-log-service

数据库：

- foodmap_log_db

核心表：

- api_access_log

验收标准：

- 支持从 `foodmap.logs.api-access` 消费接口访问摘要。
- 支持按 Kafka `topic + partition + offset` 幂等写入。
- 支持 `api_access_log` 默认 15 天保留清理，清理任务默认关闭并需要显式开启。
- 支持生成全量日志 OSS 归档计划，记录归档窗口、对象存储 Key 和处理状态。
- 支持归档执行器状态机骨架，真实 Elasticsearch 导出适配器已接入并复用 `foodmap-common` 统一 SearchClient，日志归档上传已桥接到 `foodmap-common` 统一 ObjectStorageClient，本地 MinIO/S3 兼容实现已接入，生产阿里云 OSS 适配器按部署和媒体服务阶段后续接入。
- 默认关闭 Kafka 消费，必须通过 `LOG_SERVICE_API_ACCESS_CONSUMER_ENABLED=true` 显式开启。
- 不保存 Token、密码、请求体、私密推荐正文、评论正文或完整敏感信息。
- 不访问认证、用户、推荐、门店、社区、媒体、关系等业务数据库。

### 8.5 后端通用验收标准

每个后端迭代至少满足：

- 服务边界符合 CODEX-after.md。
- 后端代码必须遵守 CODEX-after.md 中的“后端微服务企业级开发基线”。
- 每个服务使用自己的数据库配置。
- 禁止跨服务直接访问数据库表。
- Controller 使用 DTO，不直接暴露数据库实体。
- 数据库持久化实体、DTO、VO 必须分层存放并显式转换，不能互相替代。
- 数据库表字段、Flyway 字段注释和持久化实体字段 Javadoc 必须保持一致。
- 数据库访问统一使用 MyBatis Mapper + Mapper.xml。
- 每张业务表必须生成标准 `{EntityName}Mapper.java` / `{EntityName}Mapper.xml`，只放单表模板 SQL。
- 每张业务表需要复杂 SQL 时，必须新增 `{EntityName}DefineMapper.java` / `{EntityName}DefineMapper.xml`，标准 Mapper 不写复杂业务 SQL。
- 标准 Mapper 至少覆盖有限动态查询、单条新增、批量新增、单条编辑、批量编辑和批量逻辑删除。
- Repository 实现类名不使用 `MyBatis`、`Jdbc`、`Redis` 等技术前缀，统一采用 `{EntityName}RepositoryImpl` 或业务聚合语义命名；技术实现差异通过包名和注释表达。
- Controller 不能直接调用 Mapper，事务边界放在 ServiceImpl 层。
- 单服务内出现多张表新增、编辑、删除时，必须在 ServiceImpl 用例方法上使用本地事务，优先 `@Transactional(rollbackFor = Exception.class)`。
- 跨服务写流程不默认使用强分布式事务，必须采用 Saga/补偿事务 + Outbox + 幂等消费实现最终一致性。
- 跨服务数据交互失败时，必须有失败状态、补偿任务、重试或人工处理入口，不能静默失败。
- 可能并发冲突的业务主键或唯一性数据操作，必须优先判断数据库唯一约束、乐观锁、悲观锁或 Redis 分布式锁是否需要引入。
- Redis 分布式锁只能通过统一封装访问，锁必须有 owner token、lease time，并通过原子脚本释放。
- Redis 锁看门狗只用于耗时不稳定但必须串行的临界区，必须设置续期间隔、续期租约和最大续期次数，禁止无限续期。
- 业务代码使用分布式锁时优先使用 `DistributedLockClient` 的 `executeWithLock`、`tryExecuteWithLock` 或 `executeWithWatchdog` 公共方法。
- Redisson 只允许作为 `DistributedLockClient` 的基础设施适配器使用；业务代码、ServiceImpl、Controller、Repository 和领域对象都不能直接依赖 `RedissonClient`、`RLock` 或其他 Redisson API。
- Redisson 适配器必须保留 FoodMap owner token、lease time、最大续期次数和原子释放语义。
- ServiceImpl 层依赖仓储端口接口，MyBatis/内存仓储等实现不能向上穿透。
- 所有入参必须使用 Bean Validation 或等价方式校验。
- 写接口从 Token 获取当前用户身份。
- 权限、归属、可见范围必须由后端校验。
- API 正常和异常响应使用统一结构：`success`、`status`、`code`、`message`、`data`。
- `status` 必须使用 HTTP 数字状态码语义，`code` 必须使用稳定可枚举业务码。
- 后端必须通过统一异常拦截机制处理业务异常、参数校验异常、JSON 解析异常、请求方法错误和未预期异常。
- 异常响应不能暴露异常类名、堆栈、SQL、Token、密码或内部依赖地址。
- `401` 用于未认证或登录状态失效，`403` 用于已认证但权限不足，不能混用。
- 服务间同步调用必须设置超时，并有明确失败处理。
- 所有数据库结构变更必须通过 Flyway 脚本管理。
- 关键事件消费者必须支持幂等。
- 重复出现 2 次以上的基础校验、值判断、脱敏、时间处理逻辑，应优先沉淀到 `foodmap-common` 中有明确边界的项目级工具类。
- 禁止创建无明确边界的万能工具类，如 `CommonUtils`、`StringUtils`、`DateUtils`。
- record、Command、事件信封和中间件命令中的基础参数校验应优先复用 `common.validation.Check`。
- Spring Bean 依赖注入默认允许使用 `@Autowired` 字段注入，便于人工接手开发；字段必须保持 `private`。
- 强必需依赖、需要 `final` 不可变、需要脱离 Spring 容器测试、或需要尽早暴露循环依赖的类，优先使用构造器注入。
- 多个同类型 Bean 或需要按名称选择实现时，可以使用 `@Resource`、`@Qualifier` 或等价方式明确注入目标。
- 同一个类中不要混用多种注入方式，确有框架原因时必须补充说明。
- 公共类、跨模块复用类、接口、枚举、异常、事件、配置类和中间件封装类必须提供类级 Javadoc 注释。
- 常量类和枚举类中的每一个常量、每一个枚举项都必须提供 Javadoc 注释，说明业务含义、使用场景和排查价值。
- 业务方法必须提供方法级 Javadoc 注释，注释必须说明方法业务作用，并为每个入参提供 `@param`；非 `void` 方法必须为返回结果提供 `@return`，`void` 方法不需要增加 `@return`。
- Controller 层接口方法必须使用接口注释，说明接口用途、调用方关注的入参、返回结果、权限或登录态来源；如方法有入参和返回值，必须同步补齐 `@param` / `@return`。
- Service、ServiceImpl、domain、repository、mapper、通用工具和中间件适配器中的业务方法必须按业务方法注释规则执行。
- JavaBean getter / setter、Spring Boot 启动类 `main` 方法不强制提供方法级 Javadoc；实体字段说明以字段级 Javadoc 为准。
- 构造方法不是普通业务方法，不要求 `@return`，但有入参且承担依赖边界说明时，应在构造方法 Javadoc 中提供对应 `@param`。
- `private` 方法如果承载复杂业务判断、脱敏规则、幂等规则、权限规则、状态流转或非显而易见的转换逻辑，也必须提供方法级 Javadoc，并按参数和返回值补齐 `@param` / `@return`。
- 注释必须说明职责、边界和排查关注点，不能只重复代码字面含义。
- Redis、MQ、对象存储、内部服务调用必须通过项目统一封装或服务内基础设施适配器访问，业务代码不能散落直接调用中间件 SDK。
- Redis Key 必须遵守 `foodmap:{service}:{biz}:{version}:{key}` 格式，业务缓存必须设置 TTL。
- PostgreSQL 连接池统一使用 HikariCP，Redis 需要池化时统一使用 Spring Data Redis + Lettuce pool；池化参数必须通过 profile 和环境变量配置，不能在业务代码中硬编码。
- 除网关外，所有后端业务服务都必须在 `application-local.yml`、`application-orbstack.yml`、`application-prod.yml` 中显式配置 datasource、Flyway 和 HikariCP 参数。
- 数据库连接、Redis 连接、分布式锁和本地事务都必须短持有；请求线程不能在持有数据库连接或事务时等待 Redis 锁、调用高德、OSS、MQ 阻塞确认或跨服务慢接口。
- 分布式锁必须先于本地数据库事务获取，事务方法只包含必要数据库读写、短计算和 Outbox 落库。
- 每个服务必须暴露并关注 HikariCP、Redis pool 和请求线程池指标；连接等待、连接获取超时、Redis pool exhausted、看门狗续期失败都必须作为排查信号。
- 业务事件发布必须通过统一事件发布接口，事件消费者必须具备幂等处理。
- 对象存储访问必须通过统一对象存储接口，本地优先 MinIO，生产适配阿里云 OSS。
- Elasticsearch/OpenSearch 等搜索引擎访问必须通过统一搜索接口，例如 `SearchClient`，不能在各服务里散落拼接 `_search` HTTP 请求、认证头和分页游标处理。
- 业务代码不得随意字符串拼接打印关键业务日志。
- 涉及用户、认证、推荐、评论、文件和中间件调用的日志必须使用通用日志方法或统一日志封装。
- 日志不能输出 Token、密码、密钥、完整手机号、完整邮箱或私密推荐内容。
- 后端日志等级统一为 `DEBUG`、`INFO`、`WARN`、`ERROR`，所有结构化日志必须包含 `requestId`、`traceId`、`serviceName`，具备通过流水号查询一次接口调用全部日志的能力。
- 网关和业务服务必须生成、校验并透传 `requestId`、`traceId`；服务间同步调用和 MQ 事件必须继续透传 `traceId`。
- Kafka 从当前阶段开始作为日志缓冲管道引入，日志 topic 至少规划 `application`、`api-access`、`sql`、`audit`、`security` 五类。
- Elasticsearch 保存全量热日志 7 天；7 天内后台按 `requestId` 或 `traceId` 查询完整日志优先查 Elasticsearch。
- 接口访问摘要必须写入独立日志 PostgreSQL，保留 15 天，用于后台统计和最近调用查询；7 天后的全量日志压缩归档到 OSS。
- SQL 日志统一按 `DEBUG` 语义设计，生产环境默认不全量记录，必须通过动态配置按服务、Mapper、traceId/requestId、慢 SQL 阈值或采样率开启。
- 慢 SQL 和异常 SQL 即使 SQL DEBUG 关闭，也必须以 `WARN` 输出脱敏摘要。
- 每次业务接口调用必须记录结构化访问日志，关键业务动作必须记录审计日志；审计日志只保存动作事实、业务主键和脱敏摘要，不保存敏感正文。
- 列表接口支持分页。
- 地图接口支持边界框查询。
- 关键业务规则有单元测试或集成测试。
- 条件允许时执行 Maven 构建或对应服务测试。

## 9. 联调验收标准

前后端联调时至少满足：

1. 登录后能进入地图页。
2. 地图页能请求当前视野内的可见门店。
3. 用户能搜索或创建门店。
4. 用户能为门店创建推荐菜单。
5. 菜名必填，图片和标签可选。
6. 用户能选择推荐可见范围。
7. 好友和情侣只能看到被授权内容。
8. 用户只能查看和发布自己有权访问的推荐评论。
9. 单条评论最多支持 3 张图片。
10. 公开社区只展示 PUBLIC 推荐。
11. PUBLIC 推荐能进入社区统计。
12. 私密、指定用户、好友、情侣、群组内容不会进入全站统计。

## 10. GitHub 同步要求

后续每次重要迭代完成后：

1. 检查工作区状态。
2. 确认文档和代码一致。
3. 提交本地变更。
4. 推送到 `origin/main`。

当前远程仓库：

```text
git@github.com:xuqian-z97/food-map.git
```

## 11. 当前优先级

当前阶段已完成文档、多代理规则、harness、`after/` 后端微服务骨架、认证/用户持久化基础能力、Refresh Token 刷新与退出登录、网关 Access Token 校验和可信用户身份透传、`foodmap-admin-service` 后台管理员首个代码切片、B1.5-a 日志平台基础能力首批代码；B1.5-b 已完成 MyBatis SQL 日志基础能力、Kafka 本地缓冲管道、Fluent Bit 本地采集器、Elasticsearch 本地热查询基础、Logstash 本地消费写入器、独立日志 PostgreSQL 基础库表、`foodmap-log-service` 接口访问摘要消费落库、内部查询 API、15 天保留清理任务、OSS 归档计划记录、归档执行器状态机骨架、`foodmap-common` 统一 Elasticsearch SearchClient、真实日志归档导出适配器、common ObjectStorageClient 上传桥接、MinIO/S3 兼容对象存储实现、管理后台日志查询代理入口和 `LOG_ACCESS_READ` 权限骨架；同时已生成 `front/FoodMapApp` iOS SwiftUI 认证测试壳。

下一步推荐：

1. 进入 B1 后续业务能力开发前，可按部署验收任务补充日志查询真实联调；生产阿里云 OSS 适配器顺延到媒体服务或生产部署阶段接入。
2. 安装 Xcode iOS 平台组件后，使用模拟器验证登录和注册页面。
3. 将 iOS 登录页联调本地认证服务。
4. 生成高德地图首页壳、门店查询 API 契约和门店服务基础能力。
5. 后端镜像生成后，完善 `deploy/docker-compose.ecs2.yml` 中的应用服务配置。
