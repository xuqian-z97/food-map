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
| 本地缓存 | MVP 不做离线菜单/地图缓存 |

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
- VisibilityPickerView
- TagPickerView

验收标准：

- 添加推荐时菜名为必填纯文字。
- 推荐理由、图片、标签、价格、推荐程度为可选。
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
| ORM | MyBatis 或 MyBatis-Plus |
| 数据库 | PostgreSQL |
| 地理能力 | PostgreSQL + PostGIS |
| 数据库迁移 | Flyway |
| 缓存 | Redis |
| 消息队列 | RocketMQ 或 RabbitMQ |
| 对象存储 | MinIO 或阿里云 OSS |
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
- tags
- recommendation_tags
- visibility_rules

验收标准：

- 支持创建、编辑、删除推荐菜单。
- 菜名必填，并以纯文字保存。
- 推荐理由、图片、标签、价格、推荐程度为可选。
- 支持 PRIVATE、SPECIFIC_USERS、FRIENDS、COUPLE、GROUP、PUBLIC 可见范围。
- 所有推荐查询都必须校验当前用户可见权限。
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
- 支持推荐服务校验媒体引用。

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

### 8.5 后端通用验收标准

每个后端迭代至少满足：

- 服务边界符合 CODEX-after.md。
- 每个服务使用自己的数据库配置。
- 禁止跨服务直接访问数据库表。
- Controller 使用 DTO，不直接暴露数据库实体。
- 写接口从 Token 获取当前用户身份。
- 权限、归属、可见范围必须由后端校验。
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
8. 公开社区只展示 PUBLIC 推荐。
9. PUBLIC 推荐能进入社区统计。
10. 私密、指定用户、好友、情侣、群组内容不会进入全站统计。

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

当前阶段已完成文档、多代理规则、harness 和 `after/` 后端微服务骨架。

下一步推荐：

1. 实现认证和用户基础能力。
2. 生成 `front/` iOS App 壳。
3. 设计认证/用户 API 契约并与前端模型对齐。
4. 实现门店和推荐主流程。
5. 后端镜像生成后，完善 `deploy/docker-compose.ecs2.yml` 中的应用服务配置。
