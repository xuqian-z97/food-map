# CODEX 后端架构文档

## 1. 文档目的

本文档用于定义 FoodMap 的后端架构。

后端必须采用 Java 微服务模式开发。每个微服务拥有独立数据库。任何服务都不能直接读取或写入其他服务的数据库。

后续所有后端代码都必须根据本文档生成。如果服务边界、数据库归属、API 契约或基础设施选型发生变化，必须在修改代码之前或同时更新本文档。

## 2. 后端架构方向

架构形式：

- 微服务
- Java 后端
- 每个服务独立数据库
- API 网关作为外部统一入口
- 同步调用用于实时查询
- 异步事件用于统计、通知和最终一致性

核心规则：

1. 一个服务负责一个清晰的业务边界。
2. 一个服务拥有自己的数据库。
3. 服务之间不能共享数据表。
4. 服务之间通过 API 或事件通信。
5. 后端必须强制校验所有可见权限和资源归属。
6. 只有 PUBLIC 推荐内容进入全站社区统计。

## 3. 推荐技术栈

| 层级 | 技术 |
| --- | --- |
| 开发语言 | Java 21 |
| 基础框架 | Spring Boot |
| 微服务体系 | Spring Cloud / Spring Cloud Alibaba |
| API 网关 | Spring Cloud Gateway |
| 注册中心 | Nacos |
| 配置中心 | Nacos |
| 认证鉴权 | Spring Security + JWT |
| ORM | MyBatis 或 MyBatis-Plus |
| 数据库 | PostgreSQL |
| 地理数据库 | PostgreSQL + PostGIS |
| 缓存 | Redis |
| 消息队列 | RocketMQ 或 RabbitMQ |
| 对象存储 | MinIO 或阿里云 OSS |
| API 文档 | OpenAPI + Knife4j |
| 构建工具 | Maven |
| 容器化 | Docker |
| 本地开发 | Docker Compose / OrbStack / Spring Profiles |
| 监控 | Spring Boot Actuator、Prometheus、Grafana |
| 链路追踪 | OpenTelemetry 或 SkyWalking |

优先推荐的国内生态组合：

- Spring Boot
- Spring Cloud Alibaba
- Nacos
- RocketMQ
- Redis
- PostgreSQL/PostGIS
- MinIO 或阿里云 OSS

## 4. 微服务列表

```text
foodmap-platform
├── foodmap-gateway-service
├── foodmap-auth-service
├── foodmap-user-service
├── foodmap-relation-service
├── foodmap-store-service
├── foodmap-recommendation-service
├── foodmap-community-service
├── foodmap-media-service
├── foodmap-notification-service
└── foodmap-admin-service
```

MVP 阶段服务：

```text
foodmap-gateway-service
foodmap-auth-service
foodmap-user-service
foodmap-relation-service
foodmap-store-service
foodmap-recommendation-service
foodmap-community-service
foodmap-media-service
```

后续服务：

```text
foodmap-notification-service
foodmap-admin-service
```

## 5. 服务职责

### 5.1 网关服务

服务名：

```text
foodmap-gateway-service
```

职责：

- 外部 API 统一入口。
- 将请求路由到内部微服务。
- 通过 Spring Cloud LoadBalancer 解析 `lb://service-name` 路由。
- 在合适场景下进行 JWT 前置校验。
- 限流。
- 请求日志。
- CORS。
- API 版本路由。

数据库：

- 默认无数据库。

依赖：

- 如果后续使用不透明 Token，可依赖认证服务校验 Token。
- 依赖 Nacos 进行服务发现。
- 依赖 Spring Cloud LoadBalancer 完成服务实例选择。

### 5.2 认证服务

服务名：

```text
foodmap-auth-service
```

职责：

- 注册账号。
- 支持手机号、邮箱、账号名登录。
- 密码校验。
- 密码哈希。
- 签发 Access Token。
- 签发和撤销 Refresh Token。
- 登录日志。

数据库：

```text
foodmap_auth_db
```

核心表：

```text
auth_accounts
auth_credentials
refresh_tokens
login_logs
```

不负责：

- 用户资料。
- 好友关系。
- 推荐内容。

### 5.3 用户服务

服务名：

```text
foodmap-user-service
```

职责：

- 用户资料。
- 昵称。
- 头像。
- 城市。
- 简介。
- 用户设置。
- 用户状态。
- 根据对外可搜索字段搜索用户。

数据库：

```text
foodmap_user_db
```

核心表：

```text
users
user_profiles
user_settings
```

### 5.4 关系服务

服务名：

```text
foodmap-relation-service
```

职责：

- 好友申请。
- 好友关系。
- 情侣绑定申请。
- 当前有效情侣关系。
- 后续支持群组创建和成员管理。
- 拉黑用户。
- 关系权限校验。

数据库：

```text
foodmap_relation_db
```

核心表：

```text
friend_requests
friend_relations
couple_requests
couple_relations
groups
group_members
blocked_users
```

关键内部接口：

```text
GET /internal/relations/friends/check
GET /internal/relations/couple/check
GET /internal/relations/groups/{groupId}/members/check
```

### 5.5 门店服务

服务名：

```text
foodmap-store-service
```

职责：

- 集成高德 POI 搜索。
- 创建门店。
- 手动创建门店。
- 查询门店详情。
- 处理门店别名。
- 记录门店合并。
- 地图视野内门店查询。
- 地理索引。

数据库：

```text
foodmap_store_db
```

数据库类型：

```text
PostgreSQL + PostGIS
```

核心表：

```text
stores
store_pois
store_aliases
store_merge_records
```

重要原则：

门店服务拥有门店位置和 POI 标识。推荐服务只通过 storeId 引用门店，不存储完整门店详情。

### 5.6 推荐服务

服务名：

```text
foodmap-recommendation-service
```

职责：

- 创建推荐菜单。
- 编辑推荐菜单。
- 删除推荐菜单。
- 菜名必须以纯文字保存。
- 保存可选推荐理由、价格、推荐程度。
- 管理推荐标签。
- 管理推荐图片引用。
- 管理可见规则。
- 查询当前用户可见的推荐内容。
- 发送推荐相关领域事件。

数据库：

```text
foodmap_recommendation_db
```

核心表：

```text
recommendations
recommendation_images
tags
recommendation_tags
visibility_rules
```

可见范围：

```text
PRIVATE
SPECIFIC_USERS
FRIENDS
COUPLE
GROUP
PUBLIC
```

外部依赖：

- 调用关系服务进行关系校验。
- 调用门店服务校验门店是否存在。
- 调用媒体服务校验图片。
- 通过消息队列发送领域事件。

重要原则：

推荐服务是推荐内容可见范围的事实来源。

### 5.7 社区服务

服务名：

```text
foodmap-community-service
```

职责：

- 消费推荐事件。
- 维护公开门店统计。
- 维护公开菜品统计。
- 维护附近公开推荐索引。
- 提供热门门店接口。
- 提供热门菜品接口。
- 提供公开社区列表。

数据库：

```text
foodmap_community_db
```

核心表：

```text
public_store_stats
public_dish_stats
public_recommendation_index
community_feed_items
```

缓存：

- Redis 用于热门列表和附近公开结果缓存。

重要原则：

社区服务只能统计可见范围为 PUBLIC 的推荐内容。

### 5.8 媒体服务

服务名：

```text
foodmap-media-service
```

职责：

- 生成上传凭证。
- MVP 如有需要可接收后端直传。
- 校验图片类型和大小。
- 保存媒体元数据。
- 管理媒体使用引用。
- 返回公开 URL 或签名 URL。

数据库：

```text
foodmap_media_db
```

核心表：

```text
media_files
media_usage_refs
```

对象存储：

```text
MinIO 或阿里云 OSS
```

### 5.9 通知服务

服务名：

```text
foodmap-notification-service
```

阶段：

- 后续实现

职责：

- 好友申请通知。
- 情侣绑定通知。
- 群组邀请通知。
- 系统通知。

数据库：

```text
foodmap_notification_db
```

### 5.10 管理后台服务

服务名：

```text
foodmap-admin-service
```

阶段：

- 后续实现

职责：

- 公开内容审核。
- 举报处理。
- 门店合并操作。
- 用户状态管理。

数据库：

```text
foodmap_admin_db
```

## 6. 数据库归属

| 服务 | 数据库 | 主要数据 |
| --- | --- | --- |
| 认证服务 | foodmap_auth_db | 凭证、Token、登录日志 |
| 用户服务 | foodmap_user_db | 用户资料和设置 |
| 关系服务 | foodmap_relation_db | 好友、情侣、群组、拉黑 |
| 门店服务 | foodmap_store_db | 门店、POI、地理数据 |
| 推荐服务 | foodmap_recommendation_db | 推荐、标签、可见范围 |
| 社区服务 | foodmap_community_db | 公开统计和索引 |
| 媒体服务 | foodmap_media_db | 媒体元数据 |
| 通知服务 | foodmap_notification_db | 通知 |
| 管理服务 | foodmap_admin_db | 举报和审核 |

数据库规则：

任何服务都不能跨服务访问数据表。

示例：

- 推荐服务不能直接查询 friend_relations 表。
- 推荐服务必须调用关系服务。

### 6.1 库表设计通用规则

后续所有数据库表、Flyway 脚本、Entity、Mapper 和 DTO 必须遵守以下规则。

#### 6.1.1 数据库边界

- 每个微服务拥有独立数据库。
- 服务之间不能跨库访问数据表。
- 服务之间的数据交互只能通过内部 API 或 MQ 事件完成。
- 本地开发可以使用一个 PostgreSQL/PostGIS 容器创建多个逻辑数据库，但仍必须保持服务数据库边界。

#### 6.1.2 固定字段

每张业务表必须包含以下固定字段：

```text
id
created_time
updated_time
is_delete
```

字段定义：

```sql
id bigint generated by default as identity primary key,
created_time timestamptz not null,
updated_time timestamptz not null,
is_delete smallint not null default 0
```

字段含义：

- `id`：数据库内部自增主键，只用于本服务数据库内部。
- `created_time`：创建时间，暂定由业务层或 ORM 统一传入。
- `updated_time`：更新时间，暂定由业务层或 ORM 统一传入。
- `is_delete`：逻辑删除标记，`0` 表示未删除，`1` 表示已删除，默认 `0`。

查询业务数据时默认必须过滤：

```sql
is_delete = 0
```

#### 6.1.3 业务主键

主业务表必须保留 `bigint` 类型业务主键，并设置唯一约束。

示例：

```text
auth_accounts.account_id
users.user_id
friend_relations.relation_id
groups.group_id
stores.store_id
recommendations.recommendation_id
tags.tag_id
media_files.media_id
```

业务主键规则：

- 业务主键用于 API、跨服务引用、日志、事件和前端展示。
- 自增主键 `id` 不对外暴露，不用于跨服务引用。
- 跨服务引用只能保存对方服务的业务主键，例如推荐服务保存 `user_id` 和 `store_id`。
- 后续业务主键建议由统一 ID 生成器生成，优先考虑 Snowflake 风格的 `bigint` ID。

中间表不强制要求独立业务主键，但必须具备必要的唯一约束。

示例：

```sql
unique (recommendation_id, tag_id)
unique (group_id, user_id)
```

#### 6.1.4 唯一索引与逻辑删除

如果某个唯一字段在逻辑删除后允许再次创建，应优先使用 PostgreSQL 部分唯一索引。

示例：

```sql
create unique index uk_users_user_id_active
on users (user_id)
where is_delete = 0;
```

如果业务要求历史数据中该字段永久不可重复，则可以使用普通唯一约束。

#### 6.1.5 金额字段

FoodMap 当前阶段的价格字段是推荐展示信息，不是支付结算金额。

推荐菜品价格、人均价格等展示型金额使用：

```sql
numeric(10,2)
```

Java 类型使用：

```text
BigDecimal
```

示例：

```text
price_amount numeric(10,2)
avg_price_amount numeric(10,2)
```

如果后续出现订单、钱包、支付、退款、余额等真实交易金额，再使用 `bigint` 保存最小货币单位。

示例：

```text
amount_cent bigint
```

#### 6.1.6 地理字段

门店服务是地理位置事实来源。门店位置字段建议同时保留经纬度和 PostGIS 字段：

```sql
longitude numeric(10,7),
latitude numeric(10,7),
location geography(Point, 4326)
```

地图范围查询和附近查询必须优先使用 `location` 的地理索引。

#### 6.1.7 枚举、命名和敏感字段

- 表名和字段名统一使用 `snake_case`。
- 枚举字段早期优先使用 `varchar(32)` 保存明确字符串，如 `PUBLIC`、`PRIVATE`、`ACTIVE`。
- 密码、Token、密钥等敏感数据不能明文保存。
- 密码必须保存为 `password_hash`，必要时保存 `hash_algorithm`。

#### 6.1.8 数据库迁移

数据库结构必须通过 Flyway 管理。

每个服务拥有自己的迁移目录：

```text
src/main/resources/db/migration
```

迁移文件命名示例：

```text
V1__create_auth_tables.sql
V2__add_login_logs.sql
```

禁止通过手工 SQL 变更数据库后不提交迁移脚本。

### 6.2 MVP 首批表结构草案

本节定义 MVP 阶段优先落地的数据库表结构草案。后续生成 Flyway 脚本、Entity、Mapper、DTO 和 API 契约时，必须以本节为基础。

本节暂不直接等同于最终 SQL。正式建表前仍需要根据接口实现、索引策略和测试结果补充约束、索引、外键策略或字段长度。

#### 6.2.1 通用固定字段

下列所有业务表都必须包含固定字段。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| id | bigint generated by default as identity primary key | 数据库内部自增主键，仅用于本服务库内关联和 ORM 标识，不对外暴露 |
| created_time | timestamptz not null | 数据创建时间，暂定由业务层或 ORM 统一写入 |
| updated_time | timestamptz not null | 数据最后更新时间，暂定由业务层或 ORM 统一写入 |
| is_delete | smallint not null default 0 | 逻辑删除标记，0 表示未删除，1 表示已删除 |

#### 6.2.2 认证服务表结构

数据库：

```text
foodmap_auth_db
```

表：`auth_accounts`

中文名：认证账号表。

用途：保存登录账号的基础认证身份，负责账号状态、登录标识和与用户服务的 `user_id` 关联。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| account_id | bigint not null | 账号业务主键，用于认证服务对外引用账号 |
| user_id | bigint not null | 用户业务主键，关联用户服务的用户身份 |
| account_name | varchar(64) | 账号名，可用于账号名登录 |
| phone | varchar(32) | 手机号，可用于手机号登录 |
| email | varchar(128) | 邮箱，可用于邮箱登录 |
| account_status | varchar(32) not null | 账号状态，如 NORMAL、DISABLED、LOCKED |
| registered_channel | varchar(32) | 注册来源，如 IOS、WEB、ADMIN |
| last_login_time | timestamptz | 最近一次登录成功时间 |

表：`auth_credentials`

中文名：认证凭证表。

用途：保存账号的登录凭证。MVP 阶段主要保存密码哈希，后续可扩展验证码、第三方登录等凭证类型。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| credential_id | bigint not null | 凭证业务主键 |
| account_id | bigint not null | 账号业务主键，关联 auth_accounts.account_id |
| credential_type | varchar(32) not null | 凭证类型，如 PASSWORD |
| password_hash | varchar(255) | 密码哈希值，禁止保存明文密码 |
| hash_algorithm | varchar(64) | 密码哈希算法标识，如 BCrypt、Argon2 |

表：`refresh_tokens`

中文名：刷新令牌表。

用途：保存 Refresh Token 的哈希、过期和撤销状态，用于刷新 Access Token 和主动退出登录。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| token_id | bigint not null | 刷新令牌业务主键 |
| account_id | bigint not null | 账号业务主键，关联 auth_accounts.account_id |
| token_hash | varchar(255) not null | Refresh Token 哈希值，禁止保存明文 Token |
| expires_time | timestamptz not null | Refresh Token 过期时间 |
| revoked_time | timestamptz | Refresh Token 被撤销时间 |
| token_status | varchar(32) not null | Token 状态，如 ACTIVE、REVOKED、EXPIRED |

表：`login_logs`

中文名：登录日志表。

用途：记录登录行为，用于安全审计、异常登录分析和用户最近登录记录。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| login_log_id | bigint not null | 登录日志业务主键 |
| account_id | bigint | 账号业务主键，登录失败且账号不存在时可为空 |
| login_type | varchar(32) not null | 登录方式，如 PHONE、EMAIL、ACCOUNT_NAME |
| login_result | varchar(32) not null | 登录结果，如 SUCCESS、FAILED |
| ip_address | varchar(64) | 登录请求 IP 地址 |
| user_agent | varchar(512) | 登录设备或浏览器 User-Agent |

#### 6.2.3 用户服务表结构

数据库：

```text
foodmap_user_db
```

表：`users`

中文名：用户主表。

用途：保存用户核心身份、昵称、头像和账号状态，是用户服务的主事实表。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| user_id | bigint not null | 用户业务主键，用于跨服务引用用户 |
| account_id | bigint not null | 认证账号业务主键，来源于认证服务 |
| nickname | varchar(64) not null | 用户昵称 |
| avatar_media_id | bigint | 头像媒体业务主键，关联媒体服务 media_id |
| user_status | varchar(32) not null | 用户状态，如 NORMAL、DISABLED |
| searchable | smallint not null default 1 | 是否允许被搜索，1 表示允许，0 表示不允许 |

表：`user_profiles`

中文名：用户资料表。

用途：保存用户展示资料，避免用户主表承载过多非核心字段。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| profile_id | bigint not null | 用户资料业务主键 |
| user_id | bigint not null | 用户业务主键，关联 users.user_id |
| city_code | varchar(32) | 用户所在城市编码 |
| city_name | varchar(64) | 用户所在城市名称 |
| bio | varchar(255) | 用户个人简介 |
| gender | varchar(32) | 性别，可选字段 |
| birthday | date | 生日，可选字段 |

表：`user_settings`

中文名：用户设置表。

用途：保存隐私偏好和默认推荐可见范围。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| setting_id | bigint not null | 用户设置业务主键 |
| user_id | bigint not null | 用户业务主键，关联 users.user_id |
| default_visibility_type | varchar(32) not null | 默认推荐可见范围，如 PRIVATE、FRIENDS、PUBLIC |
| allow_friend_request | smallint not null default 1 | 是否允许收到好友申请，1 表示允许，0 表示不允许 |
| allow_search_by_phone | smallint not null default 0 | 是否允许通过手机号搜索到本人 |
| allow_search_by_email | smallint not null default 0 | 是否允许通过邮箱搜索到本人 |

#### 6.2.4 关系服务表结构

数据库：

```text
foodmap_relation_db
```

表：`friend_requests`

中文名：好友申请表。

用途：保存好友申请流程和处理结果。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| request_id | bigint not null | 好友申请业务主键 |
| from_user_id | bigint not null | 发起申请的用户业务主键 |
| to_user_id | bigint not null | 接收申请的用户业务主键 |
| request_message | varchar(255) | 好友申请附言 |
| request_status | varchar(32) not null | 申请状态，如 PENDING、ACCEPTED、REJECTED、CANCELED |
| handled_time | timestamptz | 申请被处理的时间 |

表：`friend_relations`

中文名：好友关系表。

用途：保存已建立的好友关系。建议按双向关系保存两行，方便按 user_id 查询好友列表。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| relation_id | bigint not null | 好友关系业务主键 |
| user_id | bigint not null | 当前用户业务主键 |
| friend_user_id | bigint not null | 好友用户业务主键 |
| relation_status | varchar(32) not null | 关系状态，如 ACTIVE、DELETED、BLOCKED |

表：`couple_requests`

中文名：情侣绑定申请表。

用途：保存情侣关系绑定申请和处理结果。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| request_id | bigint not null | 情侣绑定申请业务主键 |
| from_user_id | bigint not null | 发起绑定申请的用户业务主键 |
| to_user_id | bigint not null | 接收绑定申请的用户业务主键 |
| request_status | varchar(32) not null | 申请状态，如 PENDING、ACCEPTED、REJECTED、CANCELED |
| handled_time | timestamptz | 申请被处理的时间 |

表：`couple_relations`

中文名：情侣关系表。

用途：保存当前或历史情侣关系，同一用户同一时间只能有一个 ACTIVE 情侣关系。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| relation_id | bigint not null | 情侣关系业务主键 |
| user_a_id | bigint not null | 情侣关系一方用户业务主键 |
| user_b_id | bigint not null | 情侣关系另一方用户业务主键 |
| relation_status | varchar(32) not null | 关系状态，如 ACTIVE、UNBOUND |
| bound_time | timestamptz not null | 绑定成功时间 |
| unbound_time | timestamptz | 解除绑定时间 |

表：`groups`

中文名：用户群组表。

用途：保存用户自定义群组，用于指定用户群组可见的推荐授权。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| group_id | bigint not null | 群组业务主键 |
| owner_user_id | bigint not null | 群主用户业务主键 |
| group_name | varchar(64) not null | 群组名称 |
| group_status | varchar(32) not null | 群组状态，如 ACTIVE、DISBANDED |

表：`group_members`

中文名：群组成员表。

用途：保存群组成员身份，用于群组可见权限校验。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| member_id | bigint not null | 群组成员业务主键 |
| group_id | bigint not null | 群组业务主键，关联 groups.group_id |
| user_id | bigint not null | 成员用户业务主键 |
| member_role | varchar(32) not null | 成员角色，如 OWNER、MEMBER |
| member_status | varchar(32) not null | 成员状态，如 ACTIVE、REMOVED |

表：`blocked_users`

中文名：用户拉黑表。

用途：保存用户拉黑关系，用于搜索、关系申请和内容可见性限制。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| block_id | bigint not null | 拉黑记录业务主键 |
| user_id | bigint not null | 执行拉黑的用户业务主键 |
| blocked_user_id | bigint not null | 被拉黑的用户业务主键 |

#### 6.2.5 门店服务表结构

数据库：

```text
foodmap_store_db
```

表：`stores`

中文名：门店主表。

用途：保存系统统一门店信息，是地图点位、推荐归属和社区统计的门店事实来源。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| store_id | bigint not null | 门店业务主键，用于跨服务引用门店 |
| store_name | varchar(128) not null | 门店名称 |
| address | varchar(255) | 门店地址 |
| city_code | varchar(32) | 城市编码 |
| city_name | varchar(64) | 城市名称 |
| longitude | numeric(10,7) not null | 门店经度 |
| latitude | numeric(10,7) not null | 门店纬度 |
| location | geography(Point, 4326) not null | PostGIS 地理位置点，用于地图范围和附近查询 |
| amap_poi_id | varchar(128) | 高德 POI ID，手动创建门店可为空 |
| store_source | varchar(32) not null | 门店来源，如 AMAP、MANUAL |
| store_status | varchar(32) not null | 门店状态，如 NORMAL、MERGED、DISABLED |

表：`store_pois`

中文名：门店 POI 原始信息表。

用途：保存高德 POI 返回信息，便于门店匹配、更新和问题排查。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| poi_id | bigint not null | POI 记录业务主键 |
| store_id | bigint not null | 门店业务主键，关联 stores.store_id |
| amap_poi_id | varchar(128) not null | 高德 POI ID |
| poi_name | varchar(128) | 高德 POI 名称 |
| poi_address | varchar(255) | 高德 POI 地址 |
| raw_data | jsonb | 高德 POI 原始返回数据 |

表：`store_aliases`

中文名：门店别名表。

用途：保存门店历史名称、俗称或别名，用于搜索召回和门店合并。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| alias_id | bigint not null | 门店别名业务主键 |
| store_id | bigint not null | 门店业务主键，关联 stores.store_id |
| alias_name | varchar(128) not null | 门店别名 |

表：`store_merge_records`

中文名：门店合并记录表。

用途：保存重复门店合并历史，保证推荐引用和社区统计可以追溯。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| merge_id | bigint not null | 门店合并记录业务主键 |
| source_store_id | bigint not null | 被合并的原门店业务主键 |
| target_store_id | bigint not null | 合并后的目标门店业务主键 |
| merge_reason | varchar(255) | 合并原因说明 |

#### 6.2.6 推荐服务表结构

数据库：

```text
foodmap_recommendation_db
```

表：`recommendations`

中文名：推荐菜单主表。

用途：保存用户对某个门店的推荐菜单，是 FoodMap 最核心的业务表。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| recommendation_id | bigint not null | 推荐业务主键，用于跨服务引用推荐内容 |
| user_id | bigint not null | 推荐创建者用户业务主键 |
| store_id | bigint not null | 被推荐门店业务主键 |
| dish_name | varchar(128) not null | 推荐菜名，必须为纯文字 |
| reason | varchar(1000) | 推荐理由 |
| price_amount | numeric(10,2) | 展示型价格金额，不用于支付结算 |
| rating | smallint | 推荐程度或评分，具体范围在 API 契约中定义 |
| visibility_type | varchar(32) not null | 可见范围，如 PRIVATE、SPECIFIC_USERS、FRIENDS、COUPLE、GROUP、PUBLIC |
| recommendation_status | varchar(32) not null | 推荐状态，如 NORMAL、HIDDEN、DELETED |

表：`recommendation_images`

中文名：推荐图片表。

用途：保存推荐菜单关联的图片引用和展示顺序，实际文件元数据由媒体服务维护。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| image_id | bigint not null | 推荐图片业务主键 |
| recommendation_id | bigint not null | 推荐业务主键，关联 recommendations.recommendation_id |
| media_id | bigint not null | 媒体业务主键，关联媒体服务 media_files.media_id |
| sort_order | int not null default 0 | 图片排序值，数值越小越靠前 |

表：`tags`

中文名：标签表。

用途：保存推荐标签。MVP 阶段支持用户自定义标签，后续可扩展系统标签。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| tag_id | bigint not null | 标签业务主键 |
| tag_name | varchar(64) not null | 标签名称 |
| tag_type | varchar(32) not null | 标签类型，如 USER、SYSTEM |
| owner_user_id | bigint | 标签创建者用户业务主键，系统标签可为空 |

表：`recommendation_tags`

中文名：推荐标签关联表。

用途：保存推荐菜单和标签的多对多关系。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| ref_id | bigint not null | 推荐标签关联业务主键 |
| recommendation_id | bigint not null | 推荐业务主键，关联 recommendations.recommendation_id |
| tag_id | bigint not null | 标签业务主键，关联 tags.tag_id |

表：`visibility_rules`

中文名：推荐可见规则表。

用途：保存指定用户、指定群组等细粒度可见授权规则。好友和情侣类可见范围主要通过关系服务实时校验。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| rule_id | bigint not null | 可见规则业务主键 |
| recommendation_id | bigint not null | 推荐业务主键，关联 recommendations.recommendation_id |
| visibility_type | varchar(32) not null | 可见范围类型，应与推荐主表可见范围保持一致或作为细分规则 |
| target_type | varchar(32) not null | 授权目标类型，如 USER、GROUP |
| target_id | bigint not null | 授权目标业务主键，如 user_id 或 group_id |

#### 6.2.7 社区服务表结构

数据库：

```text
foodmap_community_db
```

表：`public_recommendation_index`

中文名：公开推荐索引表。

用途：保存 PUBLIC 推荐的社区侧索引快照，用于社区列表、附近公开推荐和搜索。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| index_id | bigint not null | 公开推荐索引业务主键 |
| recommendation_id | bigint not null | 推荐业务主键，来源于推荐服务 |
| user_id | bigint not null | 推荐创建者用户业务主键 |
| store_id | bigint not null | 门店业务主键 |
| dish_name | varchar(128) not null | 推荐菜名快照 |
| city_code | varchar(32) | 城市编码快照 |
| longitude | numeric(10,7) not null | 门店经度快照 |
| latitude | numeric(10,7) not null | 门店纬度快照 |
| location | geography(Point, 4326) not null | PostGIS 地理位置点，用于附近公开推荐 |
| indexed_status | varchar(32) not null | 索引状态，如 ACTIVE、REMOVED |

表：`public_store_stats`

中文名：公开门店统计表。

用途：统计门店被 PUBLIC 推荐的次数，用于热门门店排序。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| stat_id | bigint not null | 门店统计业务主键 |
| store_id | bigint not null | 门店业务主键 |
| city_code | varchar(32) | 城市编码 |
| public_recommendation_count | bigint not null default 0 | 公开推荐次数 |
| latest_recommendation_time | timestamptz | 最近一次公开推荐时间 |

表：`public_dish_stats`

中文名：公开菜品统计表。

用途：统计某门店下某菜品被 PUBLIC 推荐的次数，用于热门菜品排序。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| stat_id | bigint not null | 菜品统计业务主键 |
| store_id | bigint not null | 门店业务主键 |
| dish_name | varchar(128) not null | 菜品名称 |
| public_recommendation_count | bigint not null default 0 | 公开推荐次数 |
| latest_recommendation_time | timestamptz | 最近一次公开推荐时间 |

表：`community_feed_items`

中文名：社区信息流表。

用途：保存社区推荐流排序结果或候选项，便于后续扩展推荐算法。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| feed_item_id | bigint not null | 社区信息流条目业务主键 |
| recommendation_id | bigint not null | 推荐业务主键 |
| store_id | bigint not null | 门店业务主键 |
| dish_name | varchar(128) not null | 推荐菜名快照 |
| score | numeric(12,4) not null default 0 | 信息流排序分数 |
| feed_status | varchar(32) not null | 信息流状态，如 ACTIVE、HIDDEN |

#### 6.2.8 媒体服务表结构

数据库：

```text
foodmap_media_db
```

表：`media_files`

中文名：媒体文件表。

用途：保存图片等媒体文件的元数据和对象存储位置。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| media_id | bigint not null | 媒体业务主键，用于跨服务引用文件 |
| owner_user_id | bigint not null | 文件所属用户业务主键 |
| file_name | varchar(255) not null | 原始文件名或展示文件名 |
| file_type | varchar(64) not null | 文件业务类型，如 IMAGE |
| mime_type | varchar(64) not null | 文件 MIME 类型，如 image/jpeg |
| file_size | bigint not null | 文件大小，单位字节 |
| storage_provider | varchar(32) not null | 存储提供方，如 MINIO、OSS |
| bucket_name | varchar(128) not null | 对象存储桶名称 |
| object_key | varchar(512) not null | 对象存储 Key |
| public_url | varchar(1000) | 可访问 URL 或 CDN URL，私有资源可为空 |
| media_status | varchar(32) not null | 媒体状态，如 UPLOADING、ACTIVE、DELETED |

表：`media_usage_refs`

中文名：媒体使用引用表。

用途：记录媒体文件被哪些业务对象使用，便于清理未引用文件和审计图片归属。

| 字段名 | 推荐类型 | 中文注释 |
| --- | --- | --- |
| usage_id | bigint not null | 媒体使用引用业务主键 |
| media_id | bigint not null | 媒体业务主键，关联 media_files.media_id |
| owner_user_id | bigint not null | 文件所属用户业务主键 |
| biz_type | varchar(64) not null | 使用场景，如 AVATAR、RECOMMENDATION_IMAGE、STORE_IMAGE |
| biz_id | bigint not null | 使用该媒体的业务对象主键，如 user_id、recommendation_id、store_id |

## 7. 服务通信模式

### 7.1 同步调用

当用户请求需要立即得到数据时，使用同步调用：

- 认证校验。
- 用户资料查询。
- 门店是否存在。
- 好友/情侣/群组关系校验。

可选技术：

- REST
- OpenFeign
- 后续可考虑 gRPC

### 7.2 异步事件

当业务允许最终一致性时，使用事件：

- 推荐创建。
- 推荐更新。
- 推荐删除。
- 推荐可见范围变更。
- 图片上传完成。
- 好友申请发送。
- 情侣申请发送。

可选技术：

- RocketMQ
- RabbitMQ

## 8. 关键领域事件

### 8.1 RecommendationCreatedEvent

发送方：

- 推荐服务

消费方：

- 社区服务
- 后续通知服务

事件内容：

```json
{
  "eventId": "uuid",
  "recommendationId": "uuid",
  "storeId": "uuid",
  "userId": "uuid",
  "dishName": "string",
  "visibilityType": "PUBLIC",
  "createdAt": "datetime"
}
```

### 8.2 RecommendationVisibilityChangedEvent

发送方：

- 推荐服务

消费方：

- 社区服务

用途：

- 将推荐加入或移出公开统计。

### 8.3 RecommendationDeletedEvent

发送方：

- 推荐服务

消费方：

- 社区服务

用途：

- 如果被删除推荐曾经是公开内容，则从公开统计中移除。

### 8.4 MediaUploadedEvent

发送方：

- 媒体服务

消费方：

- 如果未来采用异步媒体确认，推荐服务可以消费该事件。

## 9. API 设计原则

1. 外部 API 通过网关暴露。
2. 内部服务 API 应有清晰版本或内部路径。
3. API 使用 DTO，不直接暴露数据库实体。
4. API 响应不能泄露私密数据元信息。
5. 列表接口必须支持分页。
6. 地图接口必须支持边界框查询。
7. 所有写接口必须使用 Token 中的登录用户身份。

## 10. 初始外部 API 草案

### 10.1 认证接口

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
```

### 10.2 用户接口

```text
GET /api/users/me
PUT /api/users/me
GET /api/users/search?keyword=
```

### 10.3 关系接口

```text
POST /api/friends/requests
GET /api/friends/requests
POST /api/friends/requests/{requestId}/accept
POST /api/friends/requests/{requestId}/reject
DELETE /api/friends/{friendUserId}

POST /api/couple/requests
GET /api/couple
POST /api/couple/requests/{requestId}/accept
POST /api/couple/requests/{requestId}/reject
DELETE /api/couple
```

### 10.4 门店接口

```text
GET /api/stores/search?keyword=
POST /api/stores
GET /api/stores/{storeId}
GET /api/stores/map?bbox=&scope=&tags=&keyword=
```

### 10.5 推荐接口

```text
POST /api/recommendations
GET /api/recommendations/{recommendationId}
PUT /api/recommendations/{recommendationId}
DELETE /api/recommendations/{recommendationId}
GET /api/stores/{storeId}/recommendations
GET /api/recommendations/mine
```

### 10.6 媒体接口

```text
POST /api/media/upload-token
POST /api/media/upload
POST /api/media/complete
```

### 10.7 社区接口

```text
GET /api/community/stores/hot
GET /api/community/dishes/hot
GET /api/community/stores/nearby
```

## 11. 安全和权限要求

### 11.1 认证

- 使用 JWT Access Token。
- 使用 Refresh Token。
- Refresh Token 记录保存在认证服务数据库。
- 支持 Token 撤销。

### 11.2 授权

- 用户身份来自 JWT。
- 后端校验资源归属。
- 后端校验推荐内容可见范围。
- 网关可以校验基础 JWT 结构。
- 服务内部仍必须做权限校验。

### 11.3 密码

- 密码必须使用强哈希算法保存。
- 禁止保存明文密码。

### 11.4 媒体

- 校验文件大小。
- 校验文件类型。
- 保存对象 Key、URL、拥有者 ID 和状态。

## 12. 可见范围校验

推荐内容的可见范围必须由推荐服务校验。

规则：

- PRIVATE：仅作者可见。
- SPECIFIC_USERS：作者和指定用户可见。
- FRIENDS：作者和有效好友可见。
- COUPLE：作者和有效情侣可见。
- GROUP：作者和指定群组成员可见。
- PUBLIC：所有用户可见。

推荐服务可以调用关系服务校验：

- 好友关系
- 情侣关系
- 群组成员关系

## 13. 社区统计规则

社区服务只统计：

```text
visibility_type = PUBLIC
status = NORMAL
```

社区服务不能统计：

- PRIVATE
- SPECIFIC_USERS
- FRIENDS
- COUPLE
- GROUP
- DELETED
- HIDDEN

统计更新应通过事件驱动，并允许最终一致性。

## 14. 本地开发环境

本地开发环境采用环境 profile 自动切换：

| Profile | 启动位置 | 依赖访问方式 | 说明 |
| --- | --- | --- | --- |
| local | Mac 本机、IDEA、Maven | `127.0.0.1` | 默认 profile，不设置环境变量时自动使用 |
| orbstack | Docker / OrbStack 容器网络 | Compose 服务名，如 `nacos:8848` | 后续微服务容器化后使用 |
| prod | ECS 生产环境 | 显式环境变量 | 不允许依赖本地默认值 |

后端服务必须通过以下优先级确定启动环境：

```text
SPRING_PROFILES_ACTIVE > FOODMAP_PROFILE > local
```

原因：

- 本机 IDEA 启动时访问容器依赖应使用 `127.0.0.1`。
- 服务运行在 Docker / OrbStack 容器网络中时，访问依赖应使用 Compose 服务名。
- 生产环境必须显式注入配置，避免误连本地组件。

后端服务必须使用分文件配置，不再把多个环境写在同一个 `application.yml` 的多段 YAML 中：

```text
application.yml
application-local.yml
application-orbstack.yml
application-prod.yml
```

配置职责：

- `application.yml`：服务名、端口、Actuator、网关路由等所有环境共用配置。
- `application-local.yml`：Mac 本机和 IDEA 启动时使用的本地依赖地址。
- `application-orbstack.yml`：Docker / OrbStack 容器网络中的依赖地址。
- `application-prod.yml`：生产环境配置占位，只通过环境变量注入真实地址和密钥。

推荐本地组件：

```text
Docker Compose
├── PostgreSQL auth db
├── PostgreSQL user db
├── PostgreSQL relation db
├── PostgreSQL store db with PostGIS
├── PostgreSQL recommendation db
├── PostgreSQL community db
├── PostgreSQL media db
├── Redis
├── Nacos
├── RocketMQ or RabbitMQ
└── MinIO
```

早期开发时，可以在一个 PostgreSQL 容器中创建多个逻辑数据库。但每个服务仍必须使用自己的数据库或 schema，并且不能访问其他服务的数据。

本地隔离环境文件：

```text
.env.dev.example
deploy/docker-compose.dev.yml
deploy/dev-env/README.md
```

启动本地依赖：

```sh
cp .env.dev.example .env.dev
docker compose --env-file .env.dev -f deploy/docker-compose.dev.yml up -d
```

当前阶段 Java 微服务优先由 IDEA 或 Maven 在 Mac 本机启动，使用 `local` profile；待服务 Dockerfile 完成后，再切换到 `orbstack` profile 由 Compose 统一启动。

## 15. 服务器部署方案

### 15.1 当前服务器

ECS1：

```text
公网 IP：112.124.13.171
配置：2C2G
系统：CentOS 7.4 64
已安装：Docker，含 Redis、MySQL 容器
```

ECS2：

```text
公网 IP：115.190.223.31
配置：4C4G
系统：Ubuntu 22.04 64
已安装：无
```

对象存储：

```text
阿里云 OSS
```

### 15.2 跨云约束

两台服务器不在同一个云厂商或同一个 VPC，不能默认使用内网通信。

部署原则：

- 不通过裸公网访问 Redis、PostgreSQL、Nacos、RabbitMQ。
- 不把内部组件端口开放到公网。
- 如果必须跨服务器访问内部组件，必须先建立 WireGuard/VPN 隧道。
- MVP 阶段优先把核心运行时放在 ECS2 单机，降低跨公网依赖。

### 15.3 MVP 推荐部署

ECS2 作为主应用服务器：

```text
Nginx
Spring Cloud Gateway
Nacos standalone
PostgreSQL + PostGIS
RabbitMQ
Redis
MVP 微服务
```

ECS1 作为辅助服务器：

```text
备份
轻量监控，后续
跳板，后续可选
Redis 备用，不作为 MVP 默认跨公网 Redis
```

OSS 用于：

```text
用户头像
推荐菜单图片
门店图片
```

### 15.4 MVP 部署方式

MVP 使用：

```text
Docker Compose
```

暂不使用 Kubernetes。

原因：

- 当前服务器资源较小。
- 微服务仍处于项目早期。
- Compose 更适合快速交付和验证。
- 后续可以迁移到 ACK/Kubernetes。

### 15.5 安全组原则

公网允许：

```text
80/tcp
443/tcp
22/tcp，仅允许固定运维 IP
```

禁止公网开放：

```text
5432 PostgreSQL
6379 Redis
8848 Nacos
9848 Nacos gRPC
5672 RabbitMQ
15672 RabbitMQ Management
8080 Gateway
微服务内部端口
```

## 16. 单个服务建议代码结构

每个服务采用统一结构：

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
├── scripts，按需
└── pom.xml
```

包职责：

| 包 | 职责 |
| --- | --- |
| controller | HTTP API |
| application | 用例和业务编排 |
| domain | 领域模型和业务规则 |
| infrastructure | 外部客户端、MQ、存储 |
| mapper | MyBatis 数据访问 |
| dto | 请求和响应对象 |
| config | Spring 配置 |

## 17. MVP 后端里程碑

### B1：基础设施骨架

- 父级 Maven 项目。
- 统一依赖管理。
- 网关服务。
- Nacos 配置。
- Docker Compose 基础服务。
- Spring profile 环境切换。

### B2：认证和用户

- 注册。
- 登录。
- JWT。
- 用户资料。

### B3：门店

- 门店数据库。
- 高德搜索集成占位。
- 门店创建/搜索/详情接口。
- 基础地理查询。

### B4：推荐

- 推荐数据库。
- 创建/编辑/删除推荐。
- 标签。
- 可见范围。
- 可见权限校验。

### B5：关系

- 好友申请。
- 好友关系。
- 情侣申请。
- 情侣绑定。
- 内部关系校验接口。

### B6：社区

- 推荐事件。
- 事件消费。
- 公开门店统计。
- 公开菜品统计。
- 热门门店接口。
- 附近公开推荐接口。

### B7：媒体

- 上传接口。
- 对象存储集成。
- 图片元数据。

## 18. 后端待决策问题

1. MVP 使用 RocketMQ 还是 RabbitMQ。
2. 使用 MyBatis 还是 MyBatis-Plus。
3. Spring Cloud Alibaba 版本。
4. 本地开发使用一个 PostgreSQL 容器多个数据库，还是多个 PostgreSQL 容器。
5. 公开推荐是否需要审核后才进入社区接口。
