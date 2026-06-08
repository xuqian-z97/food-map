# B1 认证和用户基础能力设计

## 目标

本轮实现 FoodMap 后端的认证和用户基础能力，先建立数据库迁移、持久化对象分层和最小 API 契约，为后续门店、推荐、好友和情侣权限提供身份基础。

## 范围

- 认证服务新增注册、登录的最小应用层链路。
- 用户服务新增当前用户资料查询的最小应用层链路。
- 认证服务和用户服务分别拥有自己的 Flyway 迁移脚本。
- 数据库结构对应的 Java 类只作为持久化基础对象使用，不能作为 Controller 入参或响应体。
- 暂不实现真实 MyBatis Mapper 和跨服务用户资料创建；认证服务通过接口预留用户服务联动边界。

## 分层设计

- `common.persistence.BaseEntity` 承载所有业务表固定字段：`id`、`createdTime`、`updatedTime`、`isDelete`。
- 数据库表对应类放在各服务的 `infrastructure.persistence.entity` 包内，例如 `AuthAccountEntity`、`UserEntity`。
- HTTP 入参和响应放在 `dto` 包内，例如 `RegisterRequest`、`LoginResponse`、`CurrentUserResponse`。
- 后续如前端需要展示专用字段，可在前端或 BFF 场景增加 VO，但不能直接暴露 Entity。

## 数据库设计

认证服务迁移脚本创建：

- `auth_accounts`
- `auth_credentials`
- `refresh_tokens`
- `login_logs`

用户服务迁移脚本创建：

- `users`
- `user_profiles`
- `user_settings`

所有表都包含 `id / created_time / updated_time / is_delete`，并通过 PostgreSQL `comment on table` 和 `comment on column` 写入中文注释。

## API 设计

本轮先落地：

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/users/me`

注册和登录返回 DTO 使用接口契约对象，不暴露密码哈希、Refresh Token 哈希或数据库自增 ID。

## 测试策略

- 先写失败测试，再补实现。
- common 层测试 `BaseEntity` 固定字段。
- auth 层测试密码哈希、Token 生成和注册命令校验。
- user 层测试当前用户资料响应构造。

## 约束

- 本轮不引入新数据库访问框架实现，避免在 Flyway、Entity、DTO 和业务链路还未稳定时过早绑定 Mapper。
- 所有新增公共类、枚举项、公共方法必须补充说明职责、边界和排查价值的注释。
- 不新增 `CommonUtils`、`StringUtils`、`DateUtils` 一类无边界工具类。
