# AGENTS.md

## 1. 文档目的

本文档用于指导后续参与 FoodMap 项目的开发代理、工程师和自动化生成流程。

任何前端、后端、文档或基础设施变更，都必须先阅读并遵守：

- CODEX-product.md
- CODEX-front.md
- CODEX-after.md
- CODEX-gen.md
- AGENTS.md

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

## 4. 前端 Agent

### 4.1 前端职责

前端 Agent 负责 iOS App 的页面、交互、状态管理、网络请求、地图展示和用户体验。

前端实现必须满足：

- iOS 原生 App。
- 以地图作为核心入口。
- 用户登录后优先进入地图页。
- 推荐添加流程轻量、清晰。
- 可见范围选择必须明确，避免用户误公开。
- 前端只能展示后端返回的可见数据，不能在本地绕过权限。

### 4.2 前端技术栈

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

### 4.3 前端目录约定

推荐结构：

```text
ios/FoodMapApp
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

### 4.4 前端页面清单

#### 4.4.1 认证页面

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

#### 4.4.2 地图页面

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

#### 4.4.3 门店页面

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

#### 4.4.4 推荐菜单页面

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

#### 4.4.5 好友页面

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

#### 4.4.6 情侣页面

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

#### 4.4.7 社区页面

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

#### 4.4.8 我的页面

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

### 4.5 前端通用验收标准

每个前端迭代至少满足：

- 页面符合 CODEX-front.md 中的模块和导航约定。
- 关键页面具备加载中、空状态、错误、无权限、网络不可用状态。
- 网络请求通过统一 APIClient 发起。
- Token 不出现在日志中。
- 私密推荐内容不出现在日志中。
- ViewModel 逻辑可测试。
- 条件允许时执行 iOS 构建或相关测试。

## 5. 后端 Agent

### 5.1 后端职责

后端 Agent 负责 Java 微服务、API、数据库、权限校验、事件、缓存和基础设施。

后端实现必须满足：

- Java 微服务架构。
- 每个服务拥有独立数据库。
- 服务不能直接访问其他服务的数据表。
- 外部请求通过 API Gateway 进入。
- 推荐内容的可见范围由服务端强制校验。
- 社区统计只统计 PUBLIC 推荐。

### 5.2 后端技术栈

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
| 缓存 | Redis |
| 消息队列 | RocketMQ 或 RabbitMQ |
| 对象存储 | MinIO 或阿里云 OSS |
| API 文档 | OpenAPI + Knife4j |
| 构建 | Maven |
| 容器 | Docker |
| 本地环境 | Docker Compose |
| 监控 | Actuator、Prometheus、Grafana |
| 链路追踪 | OpenTelemetry 或 SkyWalking |

### 5.3 后端目录约定

推荐结构：

```text
backend
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
│   └── mapper
└── pom.xml
```

### 5.4 后端服务清单

#### 5.4.1 网关服务

服务：

- foodmap-gateway-service

数据库：

- 默认无数据库

验收标准：

- 能路由外部 API 到内部服务。
- 支持基础 JWT 前置校验。
- 支持 CORS。
- 支持请求日志。
- 支持基础限流配置占位。
- 不承载业务数据。

#### 5.4.2 认证服务

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

#### 5.4.3 用户服务

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

#### 5.4.4 关系服务

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

#### 5.4.5 门店服务

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

#### 5.4.6 推荐服务

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

#### 5.4.7 社区服务

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

#### 5.4.8 媒体服务

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

#### 5.4.9 通知服务

服务：

- foodmap-notification-service

阶段：

- 后续实现

验收标准：

- 支持好友申请通知。
- 支持情侣绑定通知。
- 支持群组邀请通知。
- 支持系统通知。

#### 5.4.10 管理后台服务

服务：

- foodmap-admin-service

阶段：

- 后续实现

验收标准：

- 支持公开内容审核。
- 支持举报处理。
- 支持门店合并操作。
- 支持用户状态管理。

### 5.5 后端通用验收标准

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

## 6. 联调验收标准

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

## 7. GitHub 同步要求

后续每次重要迭代完成后：

1. 检查工作区状态。
2. 确认文档和代码一致。
3. 提交本地变更。
4. 推送到 `origin/main`。

当前远程仓库：

```text
git@github.com:xuqian-z97/food-map.git
```

## 8. 当前优先级

当前阶段仍处于文档和项目骨架准备期。

下一步推荐：

1. 生成后端微服务骨架。
2. 生成 iOS App 壳。
3. 实现认证和用户基础能力。
4. 实现门店和推荐主流程。

