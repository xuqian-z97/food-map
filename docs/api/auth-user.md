# 认证和用户 API 契约

状态：目标契约。当前 B1 代码仍处于旧 `accountId + userId` 模型，身份重构完成后以本文档为准。

## 1. 通用响应

所有接口使用统一响应结构：

```json
{
  "success": true,
  "status": 200,
  "code": "OK",
  "message": "success",
  "data": {}
}
```

错误响应中 `success=false`，`status` 使用 HTTP 数字状态码语义，`code` 使用稳定错误码，`data=null`。

本地 iOS 前后端联调时，App 必须优先通过 Gateway 访问外部 API：

```text
http://127.0.0.1:18080
```

认证服务和用户服务直连端口只用于后端排障，不作为 App 正式联调入口。

## 2. 身份模型基线

目标身份模型：

- `userId` 是 FoodMap 全系统唯一登录用户主体。
- `accountId` 属于 B1 旧模型字段，新接口、新 Token、新前端会话不再依赖。
- 账号名、手机号、邮箱、微信和 Apple 都是 `userId` 下的登录身份绑定。
- Gateway 校验 Token 后标准透传 `X-FoodMap-User-Id`。
- `X-FoodMap-Account-Id` 仅允许旧链路兼容期存在，新功能不得依赖。

Token 存储原则：

- Access Token 不落库，短有效期 JWT。
- Refresh Token 明文不落库。
- auth-service DB 保存 `auth_sessions`，只保存 Refresh Token 哈希和会话状态。
- Redis 用于 session 热缓存、denylist、验证码、登录失败计数和 OAuth state，不作为长期凭证和会话事实的唯一来源。

## 3. POST /api/auth/register

所属服务：`foodmap-auth-service`

用途：注册 FoodMap 用户，并绑定初始登录身份。

请求体：

```json
{
  "accountName": "foodie_01",
  "phone": "13800138000",
  "email": "foodie@example.com",
  "password": "secret123",
  "nickname": "小张",
  "registeredChannel": "IOS"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| accountName | string | 是 | 初始账号名登录身份，最长 64 |
| phone | string | 否 | 初始手机号登录身份，最长 32，后端标准化后保存哈希和必要密文 |
| email | string | 否 | 初始邮箱登录身份，最长 128，后端标准化后保存哈希和必要密文 |
| password | string | 是 | 登录密码，长度 8-128，后端只保存密码哈希 |
| nickname | string | 是 | 默认昵称，最长 64，由用户服务保存 |
| registeredChannel | string | 否 | 注册来源，默认 IOS |

响应 `data`：

```json
{
  "userId": 200001,
  "userStatus": "NORMAL"
}
```

错误：

- `409 / IDENTITY_ALREADY_BOUND`：账号名、手机号、邮箱或第三方身份已绑定其他用户。
- `400 / BAD_REQUEST`：请求体格式错误或字段校验失败。

## 4. POST /api/auth/login

所属服务：`foodmap-auth-service`

用途：使用账号名、手机号或邮箱登录。

请求体：

```json
{
  "loginIdentifier": "foodie_01",
  "password": "secret123"
}
```

响应 `data`：

```json
{
  "userId": 200001,
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "accessTokenExpiresTime": "2026-06-29T20:00:00+08:00",
  "refreshTokenExpiresTime": "2026-07-29T20:00:00+08:00"
}
```

安全约束：

- 响应不返回密码哈希。
- 响应不返回 Refresh Token 哈希。
- 前端必须将 Access Token 和 Refresh Token 存储在 Keychain。
- 登录失败不能暴露账号是否存在、手机号是否注册、邮箱是否绑定等可枚举信息。

## 5. POST /api/auth/refresh

所属服务：`foodmap-auth-service`

用途：使用 Refresh Token 刷新 Access Token。

请求体：

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

响应 `data`：

```json
{
  "userId": 200001,
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "accessTokenExpiresTime": "2026-06-29T20:30:00+08:00",
  "refreshTokenExpiresTime": "2026-07-29T20:00:00+08:00"
}
```

实现要求：

- Redis 优先读取 session 热缓存。
- Redis 未命中时从 `auth_sessions` DB 回源。
- Refresh Token 只保存哈希。
- 如启用 Refresh Token rotation，旧 Refresh Token 必须失效，并支持重放检测。

## 6. POST /api/auth/logout

所属服务：`foodmap-auth-service`

用途：退出登录并撤销当前 Refresh Token 对应的 session；如请求携带当前 Access Token，则同步让该 Access Token 立即失效。

请求头：

```http
Authorization: Bearer <access-token>
```

说明：

- `Authorization` 推荐传入当前登录会话持有的 Access Token。
- 认证服务不会把 Access Token 明文落库或写入 Redis。
- 认证服务只把 Access Token 摘要写入 Redis denylist，并设置 TTL 到该 Access Token 原过期时间。
- 如果不传 `Authorization`，接口仍会兼容只撤销请求体中的 Refresh Token，但当前未过期 Access Token 无法被立即加入 denylist。

请求体：

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

响应 `data`：

```json
{}
```

错误响应：

- `401 UNAUTHORIZED`：Refresh Token 无效、已过期或已失效；Access Token 已过期、签名无效或命中 denylist。
- `403 FORBIDDEN`：已认证但账号状态不允许继续使用。

## 7. GET /api/users/me

所属服务：`foodmap-user-service`

用途：查询当前登录用户资料。

外部调用路径：

```http
GET /api/users/me
Authorization: Bearer <access-token>
X-Request-Id: <request-id>
X-Trace-Id: <trace-id>
```

用户服务身份来源：

```text
X-FoodMap-User-Id
```

该字段由 Gateway 校验 JWT 后统一注入。前端不得自行伪造 `X-FoodMap-*` 可信身份头；即使外部请求携带这些头，Gateway 也必须覆盖为 Token 对应身份。

响应 `data`：

```json
{
  "userId": 200001,
  "nickname": "小张",
  "avatarMediaId": 300001,
  "userStatus": "NORMAL"
}
```

## 8. POST /internal/users/bootstrap

所属服务：`foodmap-user-service`

用途：认证服务注册成功前后调用，创建用户主体、扩展资料和默认隐私设置。该接口是内部接口，不直接对 App 暴露。

调用方：`foodmap-auth-service` 的 OpenFeign 客户端。

访问约束：

- 只能由认证服务通过 Nacos 服务发现或内网直连调用。
- 外部 Gateway 对非健康类 `/internal/**` 路径返回 `403`，不得把该接口直接暴露给 App 或公网调用方。
- 跨服务注册流程必须有失败状态、补偿任务、重试或人工处理入口，不能静默失败。

请求体：

```json
{
  "nickname": "小张",
  "registeredChannel": "IOS"
}
```

响应 `data`：

```json
{
  "userId": 200001,
  "nickname": "小张",
  "avatarMediaId": null,
  "userStatus": "NORMAL"
}
```

## 9. GET /internal/users/{userId}/login-state

所属服务：`foodmap-user-service`

用途：认证服务登录前校验用户是否允许登录。

响应 `data`：

```json
{
  "userId": 200001,
  "userStatus": "NORMAL",
  "loginAllowed": true
}
```

## 10. 微信登录预留

### 10.1 POST /api/auth/wechat/login

请求体：

```json
{
  "code": "wx-code-from-ios",
  "registeredChannel": "IOS"
}
```

响应 `data`：

```json
{
  "userId": 200001,
  "isNewUser": true,
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "accessTokenExpiresTime": "2026-06-29T20:00:00+08:00",
  "refreshTokenExpiresTime": "2026-07-29T20:00:00+08:00"
}
```

未绑定错误：

```json
{
  "success": false,
  "status": 409,
  "code": "WECHAT_IDENTITY_NOT_BOUND",
  "message": "微信账号尚未绑定 FoodMap 用户",
  "data": null
}
```

### 10.2 POST /api/auth/wechat/bind

用途：当前登录用户绑定微信身份。

### 10.3 POST /api/auth/wechat/unbind

用途：当前登录用户解绑微信身份。服务端必须校验解绑后用户仍至少保留一种可用登录方式。

## 11. 当前未实现接口

以下接口已在产品和架构文档中规划，但当前代码未全部完成：

- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `POST /api/auth/wechat/login`
- `POST /api/auth/wechat/bind`
- `POST /api/auth/wechat/unbind`
- `PUT /api/users/me`
- `GET /api/users/search?keyword=`

## 12. 分层约束

- API 请求和响应使用 DTO。
- 数据库持久化实体位于 `infrastructure.persistence.entity`。
- Controller 不直接返回 Entity。
- VO 如后续出现，仅用于前端展示或 BFF 聚合，不能替代 DTO 或 Entity。
- 新增认证和用户 API 不返回 `accountId`。
