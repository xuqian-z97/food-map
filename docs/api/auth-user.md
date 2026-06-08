# 认证和用户 API 契约

## 1. 通用响应

所有接口使用统一响应结构：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {}
}
```

错误响应中 `success=false`，`code` 使用稳定错误码，`data=null`。

## 2. POST /api/auth/register

所属服务：`foodmap-auth-service`

用途：注册账号，返回账号和用户业务主键。

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
| accountName | string | 是 | 账号名，最长 64 |
| phone | string | 否 | 手机号，最长 32 |
| email | string | 否 | 邮箱，最长 128 |
| password | string | 是 | 登录密码，后端只保存哈希 |
| nickname | string | 是 | 默认昵称，最长 64 |
| registeredChannel | string | 否 | 注册来源，默认 IOS |

响应 `data`：

```json
{
  "accountId": 100001,
  "userId": 200001,
  "accountStatus": "NORMAL"
}
```

## 3. POST /api/auth/login

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
  "accountId": 100001,
  "userId": 200001,
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "accessTokenExpiresTime": "2026-06-08T19:00:00+08:00",
  "refreshTokenExpiresTime": "2026-07-08T17:00:00+08:00"
}
```

安全约束：

- 响应不返回密码哈希。
- Refresh Token 数据库存储时只保存哈希。
- 前端必须将 Access Token 和 Refresh Token 存储在 Keychain。

## 4. GET /api/users/me

所属服务：`foodmap-user-service`

用途：查询当前登录用户资料。

当前 B1 阶段身份来源：

```text
X-FoodMap-User-Id
X-FoodMap-Account-Id
X-FoodMap-Account-Name
```

后续网关接入 JWT 后，这些字段由网关或认证上下文统一注入，前端不应自行伪造用户身份。

响应 `data`：

```json
{
  "userId": 200001,
  "accountId": 100001,
  "accountName": "foodie_01",
  "nickname": "小张",
  "avatarMediaId": 300001,
  "userStatus": "NORMAL"
}
```

## 5. 当前未实现接口

以下接口已在产品和架构文档中规划，但本轮尚未实现：

- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `PUT /api/users/me`
- `GET /api/users/search?keyword=`

## 6. 分层约束

- API 请求和响应使用 DTO。
- 数据库持久化实体位于 `infrastructure.persistence.entity`。
- Controller 不直接返回 Entity。
- VO 如后续出现，仅用于前端展示或 BFF 聚合，不能替代 DTO 或 Entity。
