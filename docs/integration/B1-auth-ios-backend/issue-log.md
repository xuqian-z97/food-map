# B1 认证前后端联调问题记录

## 1. 问题总览

| BUG 编号 | 标题 | 发现阶段 | 严重级别 | 优先级 | 状态 | 责任侧 | 关联场景 | 修复提交 | 复测结论 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| BUG-001 | Gateway 暴露用户开通内部接口 | 后端联调审查 | Critical | P0 | 已关闭 | 后端/网关 | IT-006 | `b37929c` | 复测通过 |
| BUG-002 | 用户服务开通失败时 auth 数据未回滚 | 后端联调审查 | Critical | P0 | 已关闭 | 后端/认证 | IT-008 | `b37929c` | 复测通过 |
| BUG-003 | 当前用户查询未校验 accountId 归属 | 后端联调审查 | Major | P1 | 已关闭 | 后端/用户 | IT-007 | `b37929c` | 复测通过 |
| BUG-004 | iOS 默认联调入口仍直连 Auth 未默认走 Gateway | 前端联调安全点审查 | Major | P1 | 已复测待证据 | 前端 | IT-001/IT-002/IT-004 | 本次前端数据链路提交 | 用户手工确认注册、登录可用，待补请求摘要 |
| BUG-005 | iOS 缺少当前用户接口和 Bearer 认证请求能力 | 前端联调安全点审查 | Critical | P0 | 已复测待证据 | 前端 | IT-004 | 本次前端数据链路提交 | 用户手工确认登录后进入地图，待补 `/api/users/me` 请求摘要 |
| BUG-006 | iOS 网络层未解析非 2xx 统一错误响应和 `status` | 前端联调安全点审查 | Major | P1 | 已修复待复测 | 前端/契约 | IT-003/IT-005 | 本次前端数据链路提交 | 代码审查通过，真实 iOS 联调未复测 |
| BUG-007 | iOS 当前用户临时失败时可能误清有效 Token | 前端代码审查 | Major | P0 | 已修复待复测 | 前端 | IT-002/IT-004 | 本次前端数据链路提交 | 代码审查通过，真实 iOS 联调未复测 |
| BUG-008 | iOS 注册页未在提交前校验密码长度 | 真实 iOS L2 联调 | Major | P1 | 已部分复测待证据 | 前端/契约 | IT-001/IT-003 | 本次注册校验修复提交 | 8 位以上注册主链路已手工确认；短密码前端拦截待复测 |
| BUG-009 | 登出后未过期 Access Token 仍可访问受保护接口 | 真实后端 L2 联调 | Critical | P0 | 已关闭 | 后端/认证/网关 | IT-004/IT-005 | `5994d70` | 真实接口复测通过 |
| BUG-010 | iOS 注册响应无法解析 userId-only `accountId=null` | 真实 iOS L2 联调 | Critical | P0 | 已修复待复测 | 前端/契约 | IT-001/IT-002/IT-004 | 本次前端模型修复提交 | Swift 解码复现和 iOS Debug 构建通过，待模拟器注册复测 |

## 2. 问题详情

本次后端联调已记录并关闭 BUG-001 至 BUG-003。2026-06-22 前端联调安全点审查新增 BUG-004 至 BUG-006；2026-06-24 前端代码审查新增 BUG-007；2026-06-25 真实 iOS L2 注册联调新增 BUG-008；2026-06-29 后端 Token 退出登录联调新增 BUG-009 并已关闭；2026-06-30 真实 iOS L2 注册联调新增 BUG-010。当前 BUG-004、BUG-005 已经由用户手工确认主链路复测可用，BUG-008 的 8 位以上注册主链路已确认可用；BUG-009 已完成自动测试和真实接口复测；BUG-010 已完成前端模型修复和构建验证，待模拟器注册复测；BUG-006、BUG-007 和错误态仍需补充真实 iOS 复测，所有前端问题关闭前还需补网络摘要和后端 `requestId/traceId`。

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
| 当前状态 | 已复测待证据 |
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
| 复测时间 | 2026-06-25 用户手工确认主链路可用 |
| 复测步骤 | iOS 使用 Gateway Base URL 注册、登录、查询当前用户 |
| 复测结果 | 注册、登录主链路已由用户手工确认可用；待补请求摘要和 `requestId/traceId` 后关闭 |
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
| 当前状态 | 已复测待证据 |
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
| 复测时间 | 2026-06-25 用户手工确认登录后进入地图页 |
| 复测步骤 | 登录后立即请求 `/api/users/me`；重启 App 复测 Token 恢复；401/403 复测清理会话 |
| 复测结果 | 登录后进入地图页已由用户手工确认；按最新代码路径推断 `/api/users/me` 成功，待补网络摘要后关闭 |
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

### BUG-008 iOS 注册页未在提交前校验密码长度

| 项目 | 内容 |
| --- | --- |
| 发现时间 | 2026-06-25 |
| 发现人/Agent | Codex |
| 发现阶段 | 真实 iOS L2 联调 |
| 严重级别 | Major |
| 优先级 | P1 |
| 当前状态 | 已部分复测待证据 |
| 责任侧 | 前端/契约 |
| 所属模块 | iOS / 注册 / API 契约 |
| 关联联调场景 | IT-001、IT-003 |
| 关联接口/页面 | `RegisterView`、`POST /api/auth/register` |
| 环境信息 | local |

#### 复现步骤

1. 打开 iOS 注册页。
2. 输入账号名、手机号、昵称和少于 8 位的密码，邮箱留空。
3. 点击注册按钮。

#### 测试数据

| 数据类型 | 数据值 | 说明 |
| --- | --- | --- |
| 密码长度 | 少于 8 位 | 不记录密码明文 |
| 服务地址 | `http://127.0.0.1:8081` | 本次截图为 Auth 直连，不是 Gateway L2 入口 |
| 复现请求 | `requestId=codex-ios-register-debug-direct` | 使用合成账号和手机号，不记录用户真实数据 |

#### 期望结果

- 前端提交前提示密码至少 8 位，不发起无效注册请求。
- 如后端返回统一 400，前端应展示后端 `message`，而不是只展示 HTTP 状态码。
- 完整 L2 联调时 App 服务地址应使用 Gateway `http://127.0.0.1:18080`。

#### 实际结果

- 修复前前端允许少于 8 位密码发起注册请求。
- 后端 Auth 服务返回 `400/BAD_REQUEST`，message 为密码长度必须在 8 到 128 之间。
- 用户可见提示为 `请求失败：400`，未展示具体密码长度原因；该现象也提示模拟器运行包可能不是最新前端数据链路构建。

#### 前端日志和现象摘要

- 页面：RegisterView。
- 操作：点击注册。
- 网络请求：`POST /api/auth/register`。
- 状态展示：请求失败 400。
- 截图/录屏：用户提供截图，未保存到仓库。

#### 后端日志摘要

- `requestId`：`codex-ios-register-debug-direct`。
- `traceId`：`codex-ios-register-debug-trace`。
- 服务：`foodmap-auth-service`。
- 接口：`POST /api/auth/register`。
- 日志等级：无异常堆栈；Bean Validation 返回 400。
- 关键摘要：`BAD_REQUEST`，密码长度校验失败。
- 数据库/中间件状态：参数校验阶段失败，无业务数据写入。

#### 初步分析

- 可能原因：后端注册 DTO 已有 `@Size(min = 8, max = 128)`，但 API 文档未写明长度，iOS 注册页也未在提交前做同口径校验。
- 影响范围：用户使用短密码时会进入无效网络请求，联调时只能看到 400，影响问题定位。
- 建议修复范围：API 契约补充密码长度；iOS 注册页提交前按 trim 后长度校验并提示。
- 是否涉及权限、隐私、Token、可见范围或 PUBLIC 统计口径：涉及密码输入体验和联调错误提示，不涉及 Token 泄露。

#### 修复和复测

| 项目 | 内容 |
| --- | --- |
| 修复负责人 | Codex |
| 修复提交 | 本次注册校验修复提交 |
| 复测时间 | 2026-06-25 用户手工确认 8 位以上注册主链路可用 |
| 复测步骤 | 使用少于 8 位密码点击注册，确认前端提示 `密码至少 8 位` 且不发请求；再使用 8 位以上密码通过 Gateway 复测注册 |
| 复测结果 | 8 位以上密码注册主链路已手工确认可用；短密码前端拦截和请求摘要待补充 |
| 关闭人和关闭时间 |  |
| 是否关闭 | 否 |

### BUG-009 登出后未过期 Access Token 仍可访问受保护接口

| 项目 | 内容 |
| --- | --- |
| 发现时间 | 2026-06-29 |
| 发现人/Agent | 用户 / Codex |
| 发现阶段 | 真实后端 L2 联调 |
| 严重级别 | Critical |
| 优先级 | P0 |
| 当前状态 | 已关闭 |
| 责任侧 | 后端/认证/网关 |
| 所属模块 | 认证 / 网关 / 用户服务 |
| 关联联调场景 | IT-004 / IT-005 |
| 关联接口/页面 | `POST /api/auth/logout`、`GET /api/users/me` |
| 环境信息 | local |

#### 复现步骤

1. 通过 Gateway 注册并登录，取得 Access Token 和 Refresh Token。
2. 使用 Refresh Token 调用 `POST /api/auth/logout`。
3. 使用登出前或刷新后的 Access Token 调用 `GET /api/users/me`。

#### 测试数据

| 数据类型 | 数据值 | 说明 |
| --- | --- | --- |
| 用户 | `userId=200015` | 用户提供的本地联调账号 |
| 请求参数 | 登出请求体包含 Refresh Token | 不记录 Token 明文 |
| 业务数据 | `/api/users/me` 返回用户资料 | 登出后仍成功返回，说明 Access Token 未被短期失效 |

#### 期望结果

- 登出后 Refresh Token 失效。
- 登出请求如携带 `Authorization: Bearer <access-token>`，认证服务将 Access Token 摘要写入 Redis denylist。
- Gateway 后续校验同一个 Access Token 时返回 `401/UNAUTHORIZED`，不再转发到用户服务。

#### 实际结果

- 修复前 Refresh Token 已失效，但原 Access Token 在过期前仍可通过 Gateway 访问 `/api/users/me`。
- 修复后使用登录后的 Access Token 和 Refresh Token 调用 logout，再使用同一个 Access Token 调用 `/api/users/me`，Gateway 返回 `401/UNAUTHORIZED`，`message=Access Token已失效`。
- 修复后使用已登出的 Refresh Token 调用 `/api/auth/refresh`，认证服务返回 `401/UNAUTHORIZED`，`message=Refresh Token已失效`。

#### 前端日志和现象摘要

- 页面：接口工具 / iOS 登录后会话。
- 操作：登出后再次调用当前用户接口。
- 网络请求：`GET /api/users/me` 返回 `200/OK`。
- 状态展示：仍能看到当前用户资料。
- 截图/录屏：用户聊天记录中提供接口响应摘要，未沉淀附件。

#### 后端日志摘要

- `requestId`：接口工具复测未提供，后续日志证据补齐时追加。
- `traceId`：接口工具复测未提供，后续日志证据补齐时追加。
- 服务：`foodmap-gateway-service`、`foodmap-auth-service`。
- 接口：`POST /api/auth/logout`、`GET /api/users/me`。
- 日志等级：待真实复测补充。
- 关键摘要：修复后 Gateway 应在 Access Token denylist 命中时返回 `401`。
- 数据库/中间件状态：Refresh Token 仍由 DB 撤销；Access Token 摘要写入 Redis denylist，TTL 到 Access Token 原过期时间。

#### 初步分析

- 可能原因：Access Token 是短有效期 JWT，未落库；此前 logout 只撤销 Refresh Token，Gateway 只做 JWT 签名和过期时间校验，没有查询登出状态。
- 影响范围：用户登出后，在 Access Token 原过期时间前仍可访问受保护 API，属于认证状态失效风险。
- 建议修复范围：common 增加 Access Token denylist 抽象和 Redisson 适配器；auth-service logout 写入 denylist；gateway-service 校验 denylist 并返回 `401`。
- 是否涉及权限、隐私、Token、可见范围或 PUBLIC 统计口径：涉及 Token 和认证状态，按 Critical 处理。

#### 修复和复测

| 项目 | 内容 |
| --- | --- |
| 修复负责人 | Codex |
| 修复提交 | `5994d70` |
| 复测时间 | 2026-06-29 自动测试；2026-06-29 用户真实接口复测 |
| 复测步骤 | 先登录，再登出；使用登录后的 Access Token 调用 `/api/users/me`；使用已登出的 Refresh Token 调用 `/api/auth/refresh` |
| 复测结果 | `/api/users/me` 返回 `401 Access Token已失效`；`/api/auth/refresh` 返回 `401 Refresh Token已失效`，通过 |
| 关闭人和关闭时间 | Codex，2026-06-29 |
| 是否关闭 | 是 |

### BUG-010 iOS 注册响应无法解析 userId-only `accountId=null`

| 项目 | 内容 |
| --- | --- |
| 发现时间 | 2026-06-30 |
| 发现人/Agent | 用户 / Codex |
| 发现阶段 | 真实 iOS L2 联调 |
| 严重级别 | Critical |
| 优先级 | P0 |
| 当前状态 | 已修复待复测 |
| 责任侧 | 前端/契约 |
| 所属模块 | iOS / 认证 |
| 关联联调场景 | IT-001、IT-002、IT-004 |
| 关联接口/页面 | `RegisterView`、`AuthModels`、`AuthSessionStore`、Gateway `POST /api/auth/register`、`GET /api/users/me` |
| 环境信息 | local，iPhone 17 Pro Simulator，iOS 26.5 |

#### 复现步骤

1. 打开 iOS 注册页。
2. Base URL 填写 Gateway 地址 `http://127.0.0.1:8080`。
3. 输入账号名、手机号、昵称、密码后点击注册。

#### 测试数据

| 数据类型 | 数据值 | 说明 |
| --- | --- | --- |
| 账号 | `zxq_cs_4` | 用户截图中的测试账号 |
| 请求参数 | 注册请求，包含账号名、手机号、昵称、密码和 `registeredChannel=IOS` | 测试数据场景，报告中可保留测试数据；长期提交证据仍建议避免 Token 和密码 |
| 业务数据 | 后端 userId-only 注册响应中 `accountId=null` | `accountId` 为旧身份模型兼容字段 |

#### 期望结果

- iOS 能解析后端 userId-only 注册响应。
- 注册成功后展示 `userId` 或注册成功状态，不再依赖 `accountId`。
- 登录和 `/api/users/me` 也能解析 `accountId=null` 的响应。

#### 实际结果

- 修复前 iOS 显示“服务响应格式不符合约定，请联系开发者排查”。
- Swift 临时解码复现得到 `DecodingError.valueNotFound: Expected value of type Int64 but found null instead. Path: accountId`。
- 修复后 `RegisterResponse`、`LoginResponse`、`CurrentUserResponse` 和 `AuthSession` 的 `accountId` 改为可空兼容字段，运行时身份以 `userId` 为准。

#### 前端日志和现象摘要

- 页面：RegisterView。
- 操作：点击注册。
- 网络请求：`POST /api/auth/register`，后端响应 data 中 `accountId=null`。
- 状态展示：修复前显示响应格式不符合约定；修复后待用户在模拟器复测。
- 截图/录屏：用户聊天记录中提供截图。

#### 后端日志摘要

- `requestId`：待真实复测补充。
- `traceId`：待真实复测补充。
- 服务：`foodmap-gateway-service`、`foodmap-auth-service`、`foodmap-user-service`。
- 接口：`POST /api/auth/register`，后续登录复测涉及 `POST /api/auth/login`、`GET /api/users/me`。
- 日志等级：待真实复测补充。
- 关键摘要：后端返回符合当前 userId-only 契约的 `accountId=null`，前端旧模型按非空 `Int64` 解码失败。
- 数据库/中间件状态：注册是否落库待用户复测时确认；本次根因在前端响应模型。

#### 初步分析

- 可能原因：后端已按 userId-only 返回旧 `accountId` 兼容字段为 null，但 iOS 注册、登录和当前用户模型仍把 `accountId` 定义为非空 `Int64`。
- 影响范围：注册成功响应解析、登录后当前用户解析、Token 恢复当前用户解析都可能失败，阻断 B1 iOS L2 主链路。
- 建议修复范围：iOS 认证响应模型和运行时会话以 `userId` 为主，`accountId` 只保留可空兼容；地图本地缓存隔离改用 `userId`。
- 是否涉及权限、隐私、Token、可见范围或 PUBLIC 统计口径：涉及认证主链路和身份模型一致性，按 Critical 处理。

#### 修复和复测

| 项目 | 内容 |
| --- | --- |
| 修复负责人 | Codex |
| 修复提交 | 本次前端模型修复提交 |
| 复测时间 | 2026-06-30 Swift 解码检查；2026-06-30 iOS Debug 构建 |
| 复测步骤 | 使用 `accountId=null` 的注册、登录和当前用户 JSON 执行 Swift 解码检查；执行 iOS Debug 构建 |
| 复测结果 | Swift 解码检查输出 `decode-ok`；`xcodebuild` Debug 构建通过；真实模拟器注册复测待用户执行 |
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
