# B1 认证前后端联调问题记录

## 1. 问题总览

| BUG 编号 | 标题 | 发现阶段 | 严重级别 | 优先级 | 状态 | 责任侧 | 关联场景 | 修复提交 | 复测结论 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| BUG-001 | Gateway 暴露用户开通内部接口 | 后端联调审查 | Critical | P0 | 已关闭 | 后端/网关 | IT-006 | `b37929c` | 复测通过 |
| BUG-002 | 用户服务开通失败时 auth 数据未回滚 | 后端联调审查 | Critical | P0 | 已关闭 | 后端/认证 | IT-008 | `b37929c` | 复测通过 |
| BUG-003 | 当前用户查询未校验 accountId 归属 | 后端联调审查 | Major | P1 | 已关闭 | 后端/用户 | IT-007 | `b37929c` | 复测通过 |
| BUG-004 | iOS 默认联调入口仍直连 Auth 未默认走 Gateway | 前端联调安全点审查 | Major | P1 | 已修复待复测 | 前端 | IT-001/IT-002/IT-004 | 本次前端数据链路提交 | 代码审查通过，真实 iOS 联调未复测 |
| BUG-005 | iOS 缺少当前用户接口和 Bearer 认证请求能力 | 前端联调安全点审查 | Critical | P0 | 已修复待复测 | 前端 | IT-004 | 本次前端数据链路提交 | 代码审查通过，真实 iOS 联调未复测 |
| BUG-006 | iOS 网络层未解析非 2xx 统一错误响应和 `status` | 前端联调安全点审查 | Major | P1 | 已修复待复测 | 前端/契约 | IT-003/IT-005 | 本次前端数据链路提交 | 代码审查通过，真实 iOS 联调未复测 |
| BUG-007 | iOS 当前用户临时失败时可能误清有效 Token | 前端代码审查 | Major | P0 | 已修复待复测 | 前端 | IT-002/IT-004 | 本次前端数据链路提交 | 代码审查通过，真实 iOS 联调未复测 |

## 2. 问题详情

本次后端联调已记录并关闭 BUG-001 至 BUG-003。2026-06-22 前端联调安全点审查新增 BUG-004 至 BUG-006；2026-06-24 前端代码审查新增 BUG-007。当前 BUG-004 至 BUG-007 已完成代码修复，需在真实 iOS + Gateway L2 联调中复测后关闭。

### BUG-001 Gateway 暴露用户开通内部接口

| 项目 | 内容 |
| --- | --- |
| 发现时间 | 2026-06-22 |
| 发现人/Agent | Codex |
| 发现阶段 | 后端联调审查 |
| 严重级别 | Critical |
| 优先级 | P0 |
| 当前状态 | 已关闭 |
| 责任侧 | 后端/网关 |
| 所属模块 | 网关 / 用户服务 |
| 关联联调场景 | IT-006 |
| 关联接口/页面 | `POST /internal/users/provision` |
| 环境信息 | local |

#### 复现步骤

1. 通过 Gateway 请求 `POST /internal/users/provision`。
2. 请求体使用已有 `accountId/userId/nickname`。

#### 测试数据

| 数据类型 | 数据值 | 说明 |
| --- | --- | --- |
| 业务数据 | `accountId=100010`、`userId=200010` | 后端联调账号 |
| 请求参数 | internal 用户开通载荷 | 不记录 Token、密码或敏感正文 |

#### 期望结果

- Gateway 返回 `403/FORBIDDEN`，不允许外部访问内部开通接口。

#### 实际结果

- 修复前存在外部访问风险；修复后 Gateway 返回 `403/FORBIDDEN`。

#### 后端日志摘要

- `requestId`：`codex-b1-internal-forbidden`
- `traceId`：`codex-b1-gateway-trace`
- 服务：`foodmap-gateway-service`
- 接口：`POST /internal/users/provision`
- 日志等级：INFO
- 关键摘要：Gateway completed，HTTP status 403。
- 数据库/中间件状态：未转发到用户服务，无新增数据。

#### 初步分析

- 可能原因：Gateway 对非 `/api/**` 直接放行，且路由包含 `/internal/users/**`。
- 影响范围：内部业务接口可能被外部绕过服务边界调用。
- 建议修复范围：Gateway 认证过滤器拦截非健康类 `/internal/**`。
- 是否涉及权限、隐私、Token、可见范围或 PUBLIC 统计口径：涉及权限和服务边界。

#### 修复和复测

| 项目 | 内容 |
| --- | --- |
| 修复负责人 | Codex |
| 修复提交 | `b37929c` |
| 复测时间 | 2026-06-22 |
| 复测步骤 | Gateway `POST /internal/users/provision` |
| 复测结果 | 返回 `403/FORBIDDEN`，通过 |
| 关闭人和关闭时间 | Codex，2026-06-22 |
| 是否关闭 | 是 |

### BUG-002 用户服务开通失败时 auth 数据未回滚

| 项目 | 内容 |
| --- | --- |
| 发现时间 | 2026-06-22 |
| 发现人/Agent | Codex |
| 发现阶段 | 后端联调审查 |
| 严重级别 | Critical |
| 优先级 | P0 |
| 当前状态 | 已关闭 |
| 责任侧 | 后端/认证 |
| 所属模块 | 认证 / 用户服务 |
| 关联联调场景 | IT-008 |
| 关联接口/页面 | `POST /api/auth/register` |
| 环境信息 | local |

#### 复现步骤

1. 启动 auth 服务并将 `AUTH_USER_SERVICE_URL` 指向不可用地址。
2. 请求 `POST /api/auth/register`。
3. 查询 `foodmap_auth_db.auth_accounts` 和 `auth_credentials`。

#### 测试数据

| 数据类型 | 数据值 | 说明 |
| --- | --- | --- |
| 账号 | `codex_rollback_170622` | 回滚验证账号 |
| 请求参数 | 注册请求 | 不记录密码明文 |

#### 期望结果

- 注册返回上游失败错误，auth 账号和凭证不落库。

#### 实际结果

- 修复后返回 `504/GATEWAY_TIMEOUT`，`auth_accounts=0`，`auth_credentials=0`。

#### 后端日志摘要

- `requestId`：`codex-b1-rollback-request`
- `traceId`：`codex-b1-rollback-trace`
- 服务：`foodmap-auth-service`
- 接口：`POST /api/auth/register`
- 日志等级：WARN
- 关键摘要：用户服务资料开通超时，统一异常响应 `GATEWAY_TIMEOUT`。
- 数据库/中间件状态：auth 账号和凭证计数均为 0。

#### 初步分析

- 可能原因：注册用例本地写入和内部 Feign 调用不在同一事务边界中。
- 影响范围：可能出现 auth 已注册但 user 资料缺失的半成功状态。
- 建议修复范围：B1 同步注册链路增加本地事务回滚控制。
- 是否涉及权限、隐私、Token、可见范围或 PUBLIC 统计口径：涉及账号一致性。

#### 修复和复测

| 项目 | 内容 |
| --- | --- |
| 修复负责人 | Codex |
| 修复提交 | `b37929c` |
| 复测时间 | 2026-06-22 |
| 复测步骤 | 坏用户服务 URL 注册失败后查库 |
| 复测结果 | `auth_accounts=0`，`auth_credentials=0`，通过 |
| 关闭人和关闭时间 | Codex，2026-06-22 |
| 是否关闭 | 是 |

### BUG-003 当前用户查询未校验 accountId 归属

| 项目 | 内容 |
| --- | --- |
| 发现时间 | 2026-06-22 |
| 发现人/Agent | Codex |
| 发现阶段 | 后端联调审查 |
| 严重级别 | Major |
| 优先级 | P1 |
| 当前状态 | 已关闭 |
| 责任侧 | 后端/用户 |
| 所属模块 | 用户服务 |
| 关联联调场景 | IT-007 |
| 关联接口/页面 | `GET /api/users/me` |
| 环境信息 | local |

#### 复现步骤

1. 使用正确 `X-FoodMap-User-Id`。
2. 使用错误 `X-FoodMap-Account-Id`。
3. 请求用户服务 `GET /api/users/me`。

#### 测试数据

| 数据类型 | 数据值 | 说明 |
| --- | --- | --- |
| 业务数据 | `userId=200010`、错误 `accountId=999999` | 后端联调账号 |

#### 期望结果

- 用户服务返回 `403/FORBIDDEN`。

#### 实际结果

- 修复后返回 `403/FORBIDDEN`，提示当前账号与用户资料不匹配。

#### 后端日志摘要

- `requestId`：`codex-b1-account-mismatch`
- `traceId`：`codex-b1-gateway-trace`
- 服务：`foodmap-user-service`
- 接口：`GET /api/users/me`
- 日志等级：WARN
- 关键摘要：统一业务异常 `FORBIDDEN`。
- 数据库/中间件状态：仅读取用户主表，无数据变更。

#### 初步分析

- 可能原因：服务层只按 userId 查资料，未二次校验 accountId 归属。
- 影响范围：直连服务或可信头异常时存在资料串读风险。
- 建议修复范围：用户服务 service 层校验 accountId 和用户资料归属一致。
- 是否涉及权限、隐私、Token、可见范围或 PUBLIC 统计口径：涉及权限和隐私。

#### 修复和复测

| 项目 | 内容 |
| --- | --- |
| 修复负责人 | Codex |
| 修复提交 | `b37929c` |
| 复测时间 | 2026-06-22 |
| 复测步骤 | 错配 accountId 请求 `/api/users/me` |
| 复测结果 | 返回 `403/FORBIDDEN`，通过 |
| 关闭人和关闭时间 | Codex，2026-06-22 |
| 是否关闭 | 是 |

### BUG-004 iOS 默认联调入口仍直连 Auth 未默认走 Gateway

| 项目 | 内容 |
| --- | --- |
| 发现时间 | 2026-06-22 |
| 发现人/Agent | Codex |
| 发现阶段 | 前端联调安全点审查 |
| 严重级别 | Major |
| 优先级 | P1 |
| 当前状态 | 已修复待复测 |
| 责任侧 | 前端 |
| 所属模块 | iOS / 认证 |
| 关联联调场景 | IT-001、IT-002、IT-004 |
| 关联接口/页面 | `LoginViewModel`、`RegisterView`、Gateway `/api/auth/**`、`/api/users/me` |
| 环境信息 | local |

#### 复现步骤

1. 查看 `front/FoodMapApp/FoodMapApp/Features/Auth/LoginViewModel.swift`。
2. 查看登录页服务地址默认值。
3. 对照 B1 联调要求的 Gateway `http://127.0.0.1:18080`。

#### 测试数据

| 数据类型 | 数据值 | 说明 |
| --- | --- | --- |
| 服务地址 | `http://127.0.0.1:8081` | 修复前代码默认值 |
| Gateway 地址 | `http://127.0.0.1:18080` | B1 完整联调入口 |

#### 期望结果

- iOS B1 联调默认或推荐入口为 Gateway，登录、注册、当前用户都通过 Gateway 触发。

#### 实际结果

- 修复前默认值仍直连 Auth 服务，容易绕过 Gateway 的 Token 校验、可信身份头覆盖和内部路径拦截验证。
- 修复后默认值改为 Gateway `http://127.0.0.1:18080`；若本机保存过早期 Auth 直连默认值，App 会自动回到 Gateway 默认值。

#### 前端日志和现象摘要

- 页面：LoginView / RegisterView。
- 操作：打开登录页，查看服务地址。
- 网络请求：尚未执行。
- 状态展示：可手工修改地址，但未形成默认安全入口。
- 截图/录屏：未生成。

#### 后端日志摘要

- `requestId`：无。
- `traceId`：无。
- 服务：无。
- 接口：无。
- 日志等级：无。
- 关键摘要：尚未发起请求。
- 数据库/中间件状态：无。

#### 初步分析

- 可能原因：前端认证测试壳早于 Gateway 联调完成，默认地址沿用 auth 直连端口。
- 影响范围：IT-001、IT-002、IT-004 可能没有覆盖 Gateway 行为。
- 建议修复范围：前端配置和 README 改为 Gateway 优先；保留手工覆盖能力用于排障。
- 是否涉及权限、隐私、Token、可见范围或 PUBLIC 统计口径：涉及 Token 校验入口和可信身份头安全边界。

#### 修复和复测

| 项目 | 内容 |
| --- | --- |
| 修复负责人 | Codex |
| 修复提交 | 本次前端数据链路提交 |
| 复测时间 | 2026-06-24 代码审查；真实 iOS 联调待执行 |
| 复测步骤 | iOS 使用 Gateway Base URL 注册、登录、查询当前用户 |
| 复测结果 | 代码审查通过，真实 iOS 联调未复测 |
| 关闭人和关闭时间 |  |
| 是否关闭 | 否 |

### BUG-005 iOS 缺少当前用户接口和 Bearer 认证请求能力

| 项目 | 内容 |
| --- | --- |
| 发现时间 | 2026-06-22 |
| 发现人/Agent | Codex |
| 发现阶段 | 前端联调安全点审查 |
| 严重级别 | Critical |
| 优先级 | P0 |
| 当前状态 | 已修复待复测 |
| 责任侧 | 前端 |
| 所属模块 | iOS / 认证 / 用户 |
| 关联联调场景 | IT-004 |
| 关联接口/页面 | `GET /api/users/me`、`APIClient`、`AuthSessionStore` |
| 环境信息 | local |

#### 复现步骤

1. 搜索 `front/FoodMapApp` 中的 `/api/users/me` 和 `Authorization`。
2. 查看 `APIClient` 支持的方法。
3. 查看 `AuthSessionStore` Token 恢复逻辑。

#### 测试数据

| 数据类型 | 数据值 | 说明 |
| --- | --- | --- |
| Token | 只记录是否存在 | 不记录明文 |
| 用户资料 | `userId/accountId` | 应由 `/api/users/me` 返回 |

#### 期望结果

- 登录成功后 iOS 使用 Access Token 请求 Gateway `/api/users/me`，并用后端返回的真实 `userId/accountId` 建立会话。
- App 启动恢复 Token 时先查询当前用户；401/403 时清理本地 Token 并回到登录页。

#### 实际结果

- 修复前 `APIClient` 仅支持登录和注册 POST，`AuthSessionStore` 读取到 Token 时创建 `accountId/userId = 0` 的占位会话。
- 修复后 `APIClient` 支持 `GET /api/users/me` 和 Bearer Token；登录成功和 Token 恢复后都通过当前用户接口建立真实会话。

#### 前端日志和现象摘要

- 页面：AppRouter / MapHomeView。
- 操作：存在本地 Token 后启动 App。
- 网络请求：未请求 `/api/users/me`。
- 状态展示：可能以占位用户进入地图页。
- 截图/录屏：未生成。

#### 后端日志摘要

- `requestId`：无。
- `traceId`：无。
- 服务：无。
- 接口：无。
- 日志等级：无。
- 关键摘要：前端未发起当前用户请求。
- 数据库/中间件状态：无。

#### 初步分析

- 可能原因：前端认证测试壳只完成登录成功跳转，尚未接入用户服务当前用户链路。
- 影响范围：无法验证注册后用户资料创建、Gateway 可信身份头覆盖、accountId 归属校验和登录态恢复。
- 建议修复范围：扩展 APIClient GET/Bearer 能力，新增当前用户模型，调整 AuthSessionStore 登录和恢复流程。
- 是否涉及权限、隐私、Token、可见范围或 PUBLIC 统计口径：涉及 Token、用户身份和资料串读风险。

#### 修复和复测

| 项目 | 内容 |
| --- | --- |
| 修复负责人 | Codex |
| 修复提交 | 本次前端数据链路提交 |
| 复测时间 | 2026-06-24 代码审查；真实 iOS 联调待执行 |
| 复测步骤 | 登录后立即请求 `/api/users/me`；重启 App 复测 Token 恢复；401/403 复测清理会话 |
| 复测结果 | 代码审查通过，真实 iOS 联调未复测 |
| 关闭人和关闭时间 |  |
| 是否关闭 | 否 |

### BUG-006 iOS 网络层未解析非 2xx 统一错误响应和 `status`

| 项目 | 内容 |
| --- | --- |
| 发现时间 | 2026-06-22 |
| 发现人/Agent | Codex |
| 发现阶段 | 前端联调安全点审查 |
| 严重级别 | Major |
| 优先级 | P1 |
| 当前状态 | 已修复待复测 |
| 责任侧 | 前端/契约 |
| 所属模块 | iOS / 网络层 |
| 关联联调场景 | IT-003、IT-005 |
| 关联接口/页面 | `APIResponse`、`APIClient`、`NetworkError` |
| 环境信息 | local |

#### 复现步骤

1. 查看 `front/FoodMapApp/FoodMapApp/Core/Networking/APIResponse.swift`。
2. 查看 `front/FoodMapApp/FoodMapApp/Core/Networking/APIClient.swift` 非 2xx 处理。
3. 对照 `docs/api/auth-user.md` 的统一响应契约。

#### 测试数据

| 数据类型 | 数据值 | 说明 |
| --- | --- | --- |
| 登录失败 | 错误密码 | 不记录密码明文 |
| 参数错误 | 缺少必填字段 | 不记录敏感正文 |

#### 期望结果

- 前端统一响应模型包含 `success/status/code/message/data`。
- 非 2xx 响应也尽量解析后端错误体，页面展示后端稳定 `message`，并保留 `status/code` 供联调摘要记录。

#### 实际结果

- 修复前 `APIResponse` 未包含 `status`，`APIClient` 遇到非 2xx 直接抛 `requestFailed(statusCode:)`，会丢失后端业务 `code/message`。
- 修复后 `APIResponse` 包含 `success/status/code/message/data`，非 2xx 响应会优先解析后端统一错误体并保留 `status/code/message`。

#### 前端日志和现象摘要

- 页面：LoginView / RegisterView。
- 操作：提交错误密码或错误参数。
- 网络请求：尚未执行。
- 状态展示：预计只能展示 HTTP 状态码，不能稳定展示业务错误。
- 截图/录屏：未生成。

#### 后端日志摘要

- `requestId`：无。
- `traceId`：无。
- 服务：无。
- 接口：无。
- 日志等级：无。
- 关键摘要：尚未发起请求。
- 数据库/中间件状态：无。

#### 初步分析

- 可能原因：前端网络层早于后端统一响应增加 `status` 字段。
- 影响范围：登录失败、账号冲突、权限不足、服务异常等场景的用户提示和联调排查信息不足。
- 建议修复范围：更新 `APIResponse`、`NetworkError` 和 `APIClient` 错误解析；补充 mock 网络测试。
- 是否涉及权限、隐私、Token、可见范围或 PUBLIC 统计口径：涉及 Token 失效和权限不足提示。

#### 修复和复测

| 项目 | 内容 |
| --- | --- |
| 修复负责人 | Codex |
| 修复提交 | 本次前端数据链路提交 |
| 复测时间 | 2026-06-24 代码审查；真实 iOS 联调待执行 |
| 复测步骤 | 错误密码、缺少必填字段、重复账号、未认证当前用户、accountId 错配 |
| 复测结果 | 代码审查通过，真实 iOS 联调未复测 |
| 关闭人和关闭时间 |  |
| 是否关闭 | 否 |

### BUG-007 iOS 当前用户临时失败时可能误清有效 Token

| 项目 | 内容 |
| --- | --- |
| 发现时间 | 2026-06-24 |
| 发现人/Agent | 代码审查子代理 |
| 发现阶段 | 前端代码审查 |
| 严重级别 | Major |
| 优先级 | P0 |
| 当前状态 | 已修复待复测 |
| 责任侧 | 前端 |
| 所属模块 | iOS / 认证 / 会话恢复 |
| 关联联调场景 | IT-002、IT-004 |
| 关联接口/页面 | `AuthSessionStore`、`NetworkError`、`GET /api/users/me` |
| 环境信息 | local |

#### 复现步骤

1. 登录接口成功返回 Access Token 和 Refresh Token。
2. 在随后的 `GET /api/users/me` 阶段模拟用户服务 5xx、Gateway 超时、断网或响应临时异常。
3. 查看 Keychain 中已保存 Token 是否被清理。

#### 测试数据

| 数据类型 | 数据值 | 说明 |
| --- | --- | --- |
| Token | 只记录是否存在 | 不记录明文 |
| 错误类型 | 5xx、504、断网、响应格式异常、401、403 | 401/403 才表示认证应清理 |

#### 期望结果

- 网络失败、5xx、504、响应格式临时异常不应清理已获取的有效 Token。
- 401、403 或明确账号/用户失效业务码才允许清理 Keychain Token 并回到登录页。

#### 实际结果

- 修复前登录成功保存 Token 后，`/api/users/me` 任意失败都会执行 `tokenStore.clear()`。
- 修复前启动恢复时，`invalidBaseURL`、`invalidResponse`、`emptyData`、`decodingFailed` 也会被视为不可恢复并清理 Token。
- 修复后统一收敛到 `NetworkError.shouldClearStoredAuthentication`，只在 401、403 或明确账号/用户失效业务码时清理 Token。

#### 前端日志和现象摘要

- 页面：LoginView / AppRouter。
- 操作：登录成功后模拟当前用户接口异常；或已有 Token 后启动 App。
- 网络请求：`POST /api/auth/login` 成功，`GET /api/users/me` 失败。
- 状态展示：应停留在未登录或恢复失败状态并展示错误，但不误删有效 Token。
- 截图/录屏：未生成。

#### 后端日志摘要

- `requestId`：真实联调待记录。
- `traceId`：真实联调待记录。
- 服务：Gateway / User Service。
- 接口：`GET /api/users/me`。
- 日志等级：真实联调待记录。
- 关键摘要：区分 401/403 与 5xx/504/网络失败。
- 数据库/中间件状态：无数据变更。

#### 初步分析

- 可能原因：前端把“当前用户资料暂时不可获取”和“Token 已失效”混为同一个清理路径。
- 影响范围：服务短暂不可用时会破坏用户已获取的有效认证凭证，造成重复登录或联调误判。
- 建议修复范围：收窄 Token 清理条件，登录后拉用户资料和启动恢复统一使用该条件。
- 是否涉及权限、隐私、Token、可见范围或 PUBLIC 统计口径：涉及 Token 本地持久化和登录态恢复。

#### 修复和复测

| 项目 | 内容 |
| --- | --- |
| 修复负责人 | Codex |
| 修复提交 | 本次前端数据链路提交 |
| 复测时间 | 2026-06-24 代码审查；真实 iOS 联调待执行 |
| 复测步骤 | 模拟 5xx、504、断网、响应格式异常、401 和 403，观察 Keychain Token 是否按规则保留或清理 |
| 复测结果 | 代码审查通过，真实 iOS 联调未复测 |
| 关闭人和关闭时间 |  |
| 是否关闭 | 否 |

新增问题时复制以下模板。

### BUG-001 标题

| 项目 | 内容 |
| --- | --- |
| 发现时间 |  |
| 发现人/Agent |  |
| 发现阶段 |  |
| 严重级别 |  |
| 优先级 |  |
| 当前状态 | 新建 |
| 责任侧 | 待分析 |
| 所属模块 | 认证 / iOS / 网关 / 用户服务 / 日志 |
| 关联联调场景 |  |
| 关联接口/页面 |  |
| 环境信息 | local / orbstack / prod-like |

#### 复现步骤

1. 
2. 
3. 

#### 测试数据

| 数据类型 | 数据值 | 说明 |
| --- | --- | --- |
| 账号 |  |  |
| 请求参数 |  | 不记录 Token、密码或敏感正文 |
| 业务数据 |  |  |

#### 期望结果

- 

#### 实际结果

- 

#### 前端日志和现象摘要

- 页面：
- 操作：
- 网络请求：
- 状态展示：
- 截图/录屏：

#### 后端日志摘要

- `requestId`：
- `traceId`：
- 服务：
- 接口：
- 日志等级：
- 关键摘要：
- 数据库/中间件状态：

#### 初步分析

- 可能原因：
- 影响范围：
- 建议修复范围：
- 是否涉及权限、隐私、Token、可见范围或 PUBLIC 统计口径：

#### 修复和复测

| 项目 | 内容 |
| --- | --- |
| 修复负责人 |  |
| 修复提交 |  |
| 复测时间 |  |
| 复测步骤 |  |
| 复测结果 | 未复测 |
| 关闭人和关闭时间 |  |
| 是否关闭 | 否 |
