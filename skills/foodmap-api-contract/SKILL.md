---
name: foodmap-api-contract
description: Use when designing, updating, checking, or implementing FoodMap API contracts, DTOs, request and response models, OpenAPI documents, frontend-backend integration models, pagination, map bbox queries, or visibility enum consistency.
---

# FoodMap API 契约 Skill

## 使用时机

当任务涉及接口契约时使用本 skill：

- 设计后端 API。
- 生成 DTO。
- 对齐 iOS 前端模型和后端响应。
- 编写 OpenAPI/Knife4j 文档。
- 定义错误码、分页、地图边界框查询。
- 检查可见范围枚举一致性。

## 必读文档

- `CODEX-after.md`
- `CODEX-front.md`
- `AGENTS.md`
- `.agents/api-agent.md`
- `harness/api-contract-checklist.md`

如涉及产品语义，同时读取 `CODEX-product.md`。

## API 范围

认证：

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`

用户：

- `GET /api/users/me`
- `PUT /api/users/me`
- `GET /api/users/search?keyword=`

关系：

- 好友申请、接受、拒绝、删除。
- 情侣申请、接受、拒绝、解除。

门店：

- `GET /api/stores/search?keyword=`
- `POST /api/stores`
- `GET /api/stores/{storeId}`
- `GET /api/stores/map?bbox=&scope=&tags=&keyword=`

推荐：

- `POST /api/recommendations`
- `GET /api/recommendations/{recommendationId}`
- `PUT /api/recommendations/{recommendationId}`
- `DELETE /api/recommendations/{recommendationId}`
- `GET /api/stores/{storeId}/recommendations`
- `GET /api/recommendations/mine`

媒体和社区：

- `POST /api/media/upload-token`
- `POST /api/media/upload`
- `POST /api/media/complete`
- `GET /api/community/stores/hot`
- `GET /api/community/dishes/hot`
- `GET /api/community/stores/nearby`

## 强制规则

- API 使用 DTO，不暴露数据库实体。
- API 正常和异常响应使用统一结构：`success`、`status`、`code`、`message`、`data`。
- `status` 使用 HTTP 数字状态码语义，`code` 使用稳定业务码；前端可先按 `status` 分类处理，再按 `code` 细化提示。
- 错误响应不能暴露异常类名、堆栈、SQL、Token、密码或内部依赖地址。
- `401` 表示未认证或登录状态失效，`403` 表示已认证但权限不足。
- 列表接口必须支持分页。
- 地图接口必须支持边界框查询。
- 写接口使用 Token 中的当前用户身份。
- 后端只返回当前用户有权查看的数据。
- 不泄露私密推荐元数据。
- 可见范围枚举保持一致：
  - `PRIVATE`
  - `SPECIFIC_USERS`
  - `FRIENDS`
  - `COUPLE`
  - `GROUP`
  - `PUBLIC`

## 工作流程

1. 明确接口属于哪个服务。
2. 定义请求、响应、错误结构和分页结构。
3. 对齐后端 DTO 与 iOS 模型。
4. 如契约变化，更新 `CODEX-after.md`、`CODEX-front.md` 或 `docs/api`。
5. 按 `harness/api-contract-checklist.md` 检查权限、可见范围、分页、bbox 和统计口径。
6. 输出接口变更摘要。

## 验收标准

- API 路径、方法、请求体、响应体清晰。
- 前后端字段命名一致。
- 分页、bbox、可见范围、错误结构明确。
- 不出现前端自行判断核心权限的契约设计。
