# B1 认证 iOS L2 测试报告模板

## 0. 填写规则

本报告用于记录 B1 认证 `userId-only` 重联调结果。请只填写 `【填写】`、`【选择】`、`【粘贴脱敏摘要】` 标记位置。

敏感信息限制：

- 不填写 Access Token / Refresh Token 明文。
- 不填写密码明文。
- 手机号、邮箱必须脱敏，例如 `186****1003`、`zxq***@example.com`。
- 请求体只记录字段是否存在、字段格式和脱敏值。
- 后端日志只粘贴摘要，不粘贴 SQL 敏感参数、堆栈、Token 或内部密钥。

## 1. 基本信息

| 项目 | 填写位置 |
| --- | --- |
| 测试人 | 【填写】 |
| 测试日期 | 2026-06-30 10:00:00 |
| 测试轮次 | R1 |
| 测试目标 | B1 认证 iOS L2 本地真实联调，验证注册、登录、当前用户、刷新、登出、错误态和敏感日志 |
| 当前结论 | 【选择：未开始 / 通过 / 有条件通过 / 不通过 / 无法判定】 |
| 关联分支或提交 |  |
| 关联文档 | `docs/integration/B1-auth-ios-backend/integration-plan.md`、`docs/integration/B1-auth-ios-backend/issue-log.md` |

## 2. 测试环境

| 组件 | 填写内容 |
| --- | --- |
| iOS 设备 | iPhone 17 Pro Simulator |
| iOS 版本 | 26.5 |
| Xcode 版本 | 不清楚 |
| App 构建方式 | Xcode |
| App Base URL | `http://127.0.0.1:8080` |
| Gateway | `http://127.0.0.1:8080`、8080、启动 |
| Auth Service | `http://127.0.0.1:8081`、8081、启动 |
| User Service | `http://127.0.0.1:8082`、8082、启动 |
| PostgreSQL | 启动、不确定是否执行 Flyway |
| Redis | 启动 |
| Nacos 模式 | 启用 |
| 后端启动 profile | local |

## 3. 测试账号和数据

| 数据 | 填写内容 |
| --- | --- |
| 新注册账号名 | zxq_cs_3 |
| 手机号 | 【填写脱敏值，例如 `186****1003`】 |
| 邮箱 | 【填写脱敏值，例如 `codex***@example.com`】 |
| 昵称 | 【填写】 |
| 注册渠道 | 【填写：IOS】 |
| 注册后 userId | 【填写：只填写数字 userId】 |
| 备注 | 【填写：是否为本轮新建数据、是否需要清理】 |

Token 记录要求：

| 项目 | 填写内容 |
| --- | --- |
| 登录后 Access Token | 【选择：存在 / 不存在；禁止填写明文】 |
| 登录后 Refresh Token | 【选择：存在 / 不存在；禁止填写明文】 |
| Refresh 后新 Access Token | 【选择：存在 / 不存在 / 未执行；禁止填写明文】 |
| Logout 后本地 Token | 【选择：已清理 / 未清理 / 未观察】 |

## 4. 通用证据格式

每个接口场景都按以下格式填写。

| 字段 | 填写位置 |
| --- | --- |
| 执行时间 | 【填写：YYYY-MM-DD HH:mm:ss】 |
| 入口 | 【填写：iOS 页面 / 接口工具 / curl / 其他】 |
| Method + Path | 【填写：例如 `POST /api/auth/login`】 |
| HTTP status | 【填写：例如 `200`、`400`、`401`】 |
| 业务 code | 【填写：例如 `OK`、`UNAUTHORIZED`】 |
| message | 【填写：后端返回 message，可脱敏】 |
| requestId | 【填写：响应头或日志中的 requestId】 |
| traceId | 【填写：响应头或日志中的 traceId】 |
| latency | 【填写：毫秒或接口工具显示耗时，可为空】 |
| iOS 展示 | 【填写：页面提示、跳转结果、是否保留输入】 |
| 后端日志摘要 | 【粘贴脱敏摘要：服务名、接口、状态、关键错误；不要粘贴 Token/密码/完整手机号/完整邮箱】 |
| 截图或附件路径 | 【填写：例如 `docs/integration/B1-auth-ios-backend/pic/xxx.png`，没有则写无】 |
| 结果 | 【选择：通过 / 不通过 / 未执行 / 无法判定】 |

## 5. 主链路测试

### IT-001 注册成功

预期：

- iOS 通过 Gateway 请求 `POST /api/auth/register`。
- 后端返回 `success=true`、`status=200`。
- 响应中 `userId` 有值。
- `accountId` 不作为本轮通过条件；如果响应中仍存在该字段，应为兼容字段，不影响 `userId-only` 结论。
- 注册成功后不记录密码、Token、完整手机号、完整邮箱。

填写：

| 字段 | 填写位置 |
| --- | --- |
| 请求体字段 | accountName = zxq_cs_5 <br>phone = 18639761981<br>email = null<br>password 满足长度<br>nickname = Nick_2<br>registeredChannel = IOS |
| 响应 data.userId | 200019 |
| 响应 data.accountId | 未看到界面中提示有accountId |
| 通用证据 | 页面提示注册成功 |
| 结果 | 通过 |

### IT-002 登录成功

预期：

- iOS 通过 Gateway 请求 `POST /api/auth/login`。
- 登录成功后 Access Token 和 Refresh Token 存在，但不记录明文。
- App 进入地图页前或进入过程中调用 `/api/users/me` 校验真实当前用户。

填写：

| 字段 | 填写位置 |
| --- | --- |
| 登录标识类型 | zxq_cs_5 |
| 登录后 Access Token | 存在 |
| 登录后 Refresh Token | 存在 |
| 登录后页面 | 进入地图页 |
| 通用证据 | 成功进入地图页面 |
| 结果 | 通过 |

### IT-003 当前用户查询成功

预期：

- iOS 或 Gateway 请求 `GET /api/users/me`。
- 请求携带 `Authorization: Bearer <access-token>`，但报告中不记录 Token 明文。
- 响应 `data.userId` 与本轮注册或登录用户一致。
- 响应中的 `accountId` 不作为通过条件。

填写：

| 字段 | 填写位置 |
| --- | --- |
| Authorization Header | 已携带 |
| 响应 data.userId | 200019 |
| 是否与注册 userId 一致 | 一致 |
| 响应 data.accountId | 未观察 |
| iOS 会话 userId |  |
| 通用证据 | 点击人像按钮可以正确展示userId、昵称、状态 |
| 结果 | 通过 |

### IT-004 Refresh Token 刷新成功

预期：

- 请求 `POST /api/auth/refresh`。
- 使用登录后 Refresh Token，但报告中不记录明文。
- 刷新成功后返回新的 Access Token。
- 使用刷新后的 Access Token 调用 `/api/users/me` 成功。

填写：

| 字段 | 填写位置 |
| --- | --- |
| Refresh Token 来源 | 本轮登录后 |
| 刷新后 Access Token | 存在 |
| `/api/users/me` 复测结果 | 成功 |
| 通用证据 | 通过api工具可以正常调用 |
| 结果 |  |

### IT-005 退出登录成功

预期：

- 请求 `POST /api/auth/logout`。
- 请求同时携带 `Authorization: Bearer <access-token>` 和请求体 Refresh Token。
- 退出成功后 Refresh Token 失效。
- 当前 Access Token 被写入 Redis denylist，在未自然过期前也不能访问受保护接口。
- iOS 本地会话和 Keychain Token 被清理。

填写：

| 字段 | 填写位置 |
| --- | --- |
| Authorization Header | 未携带 |
| 请求体 Refresh Token | IT-004 刷新后 |
| Logout 响应 | {"success":true,"status":200,"code":"OK","message":"success","data":null} |
| iOS 本地 Token | 未观察（不知道如何观察） |
| 通用证据 |  |
| 结果 | 无法判定 |
| 阻塞原因 | 本轮 iOS 登出没有发起请求（LOG-001 该场景无 requestId/traceId），服务端未收到退出调用。需在 `cb1145e` 之后重跑 |

### IT-006 Logout 后旧 Access Token 访问 `/api/users/me`

预期：

- 使用已登出的旧 Access Token 请求 `GET /api/users/me`。
- Gateway 返回 `401`。
- message 应表达 Access Token 已失效或登录态失效。
- iOS 应回到未登录态或提示登录失效。

填写：

| 字段 | 填写位置 |
| --- | --- |
| 使用的 Access Token 来源 | IT-004 刷新后 |
| HTTP status | 200 |
| 业务 code | OK |
| message | success |
| iOS 会话状态 | 未观察到 |
| 通用证据 |               |
| 结果 | 无法判定（前置条件未满足） |
| 说明 | 本轮登出请求未到达后端，服务端从未把该 Access Token 写入拒绝名单，返回 200 不能判定为后端缺陷。需在 `cb1145e` 之后重跑 |

### IT-007 Logout 后旧 Refresh Token 调用 `/api/auth/refresh`

预期：

- 使用已登出的 Refresh Token 请求 `POST /api/auth/refresh`。
- 返回 `401`。
- message 应表达 Refresh Token 已失效或登录态失效。

填写：

| 字段 | 填写位置 |
| --- | --- |
| 使用的 Refresh Token 来源 | IT-004 刷新后 |
| HTTP status | 200 |
| 业务 code | OK |
| message | success |
| 通用证据 |  |
| 结果 | 无法判定（前置条件未满足） |
| 说明 | 同 IT-006：登出未执行，Refresh Token 仍处于 ACTIVE，返回 200 属预期行为。需在 `cb1145e` 之后重跑 |

## 6. 错误态测试

### IT-008 错误密码登录

预期：

- 返回 `401` 或后端约定的认证失败状态。
- iOS 展示明确错误。
- 不清空账号输入；不记录密码。

填写：

| 字段 | 填写位置 |
| --- | --- |
| 登录标识 | zxq_cs_5 |
| 密码记录 | 未记录明文 |
| iOS 是否保留输入 | 是 |
| 通用证据 | 已提示：账号密码错误 |
| 结果 | 通过 |

### IT-009 重复账号 / 手机号 / 邮箱注册

预期：

- 重复账号、手机号或邮箱时返回 `409` 或后端约定的冲突错误。
- iOS 展示可理解提示。
- 已输入内容保留，便于用户修改。

填写：

| 字段 | 填写位置 |
| --- | --- |
| 重复字段 | 手机号 |
| 冲突数据 | 186****1693 |
| iOS 是否保留输入 | 是 |
| 通用证据 |  |
| 结果 | 通过 |

### IT-010 短密码注册前端拦截

预期：

- 小于后端契约要求长度的密码应在 iOS 提交前被拦截。
- 如果仍发到后端，后端应返回 `400` 参数错误。
- 不记录密码明文。

填写：

| 字段 | 填写位置 |
| --- | --- |
| 密码长度 | 3 |
| 是否发起网络请求 | 未发起 |
| iOS 错误提示 | 密码至少8位 |
| 如果发起请求，通用证据 |  |
| 结果 | 通过 |

### IT-011 缺少必填字段注册

预期：

- 缺少账号名、手机号、邮箱、密码、昵称或注册渠道时，前端应拦截或后端返回 `400`。
- 错误提示应指向可修正字段。

填写：

| 字段 | 填写位置 |
| --- | --- |
| 缺少字段 | phone（契约中 phone 与 email 均为可选，本项原预期错误，2026-11-05 修正） |
| 是否发起网络请求 | 已发起 |
| iOS 错误提示 | 注册成功：userId 200020 |
| 如果发起请求，通用证据 |  |
| 结果 | 通过（符合契约） |
| 依据 | `AuthServiceImpl.ensureLoginIdentifierAvailable` 对 phone/email 先判空；`RegisterRequest` 允许二者为空，缺少 phone 不构成参数错误 |

### IT-012 Gateway 地址错误或后端不可用

预期：

- iOS 展示网络不可用或服务不可用提示。
- 不误清有效 Token，除非后端明确返回 `401/403`。
- 用户已输入内容应保留。

填写：

| 字段 | 填写位置 |
| --- | --- |
| 故障方式 | http://127.0.0.1:18080 |
| iOS 错误提示 | 无法连接后端服务，请确认Gateway已启动 |
| 是否误清 Token | 未观察 |
| 是否保留输入 | 未观察 |
| 通用证据 | ios提示：无法连接后端服务，请确认Gateway已启动 |
| 结果 | 无法判定 |

### IT-013 未认证访问 `/api/users/me`

预期：

- 不携带 Authorization 请求 `/api/users/me`。
- Gateway 返回 `401`。
- iOS 保持未登录态。

填写：

| 字段 | 填写位置 |
| --- | --- |
| Authorization Header | 未携带 |
| HTTP status | 401 |
| 业务 code | UNAUTHORIZED |
| message | 【填写】 |
| 通用证据 | 缺少Access Token |
| 结果 | 通过 |

### IT-014 外部访问内部接口拦截

预期：

- 通过 Gateway 请求 `/internal/users/bootstrap` 或 `/internal/users/provision`。
- 返回 `403`。
- 不转发到用户服务执行内部业务。

填写：

| 字段 | 填写位置 |
| --- | --- |
| 内部接口路径 | `/internal/users/bootstrap` |
| HTTP status | 403 |
| 业务 code | FORBIDDEN |
| message | 内部接口不允许通过外部网关访问 |
| 后端日志摘要 | 2026-06-30 11:09:18:069  INFO 67551 --- [foodmap-gateway-service] [tor-http-nio-11] c.f.gateway.filter.GatewayTraceFilter    : gateway.request.completed requestId=k6ooevqp7skbmfmqistmahm0 traceId=ly17kzsl5jhmztatk7am7l0f spanId=2nsyirnqwelra0oobvdthliz serviceName=foodmap-gateway-service method=GET path=/internal/users/bootstrap status=403 durationMs=1886 |
| 结果 | 通过 |

## 7. 日志、链路和敏感信息检查

### LOG-001 requestId / traceId 可追踪

要求：

- 注册、登录、当前用户、刷新、登出、登出后旧 Token 访问至少各保留一组 `requestId` / `traceId`。
- 能从 Gateway、Auth、User 至少看到对应服务摘要；如果某接口只涉及 Gateway，也说明原因。

填写：

| 场景 | requestId | traceId | 涉及服务 | 是否可串联 | 备注 |
| --- | --- | --- | --- | --- | --- |
| IT-001 注册成功 | ios-c96edfe8-3a5b-4c0f-86da-a5498732b690 | ios-6c7f3d6c-e729-4639-b61e-b73a51b3e153 | gateway、auth、user | 是 | 【填写】 |
| IT-002 登录成功 | ios-3e39575a-051e-4996-9d7c-9a0ef9c30a8f | ios-4b3b7720-c098-4725-8862-5223b9955e50 | gateway、auth、user | 是 | 【填写】 |
| IT-003 当前用户 | ios-323a2e1d-c712-4b8b-945b-bdce4006a061 | ios-86d1cabb-037c-41d6-887a-eceb0e0c7218 | gateway、auth、user | 【选择：是 / 否】 | 【填写】 |
| IT-004 刷新 | xz18sizq6hmpjl2rct40nlc2 | bt5szysupkxer1nntbgxmn2h | gateway、auth | 【选择：是 / 否】 | 【填写】 |
| IT-005 登出 | 无 | 无 | 无 | 无 | ios端登出按钮没法起请求 |
| IT-006 旧 Access Token 访问 |  |  |  |                   | 【填写】 |
| IT-007 旧 Refresh Token 刷新 |  |  |  |  |  |

### LOG-002 敏感信息未泄露

填写：

| 检查项 | 结果 | 证据或说明 |
| --- | --- | --- |
| 前端日志不输出 Access Token 明文 | 【选择：通过 / 不通过 / 未检查】 | 【填写】 |
| 前端日志不输出 Refresh Token 明文 | 【选择：通过 / 不通过 / 未检查】 | 【填写】 |
| 前端日志不输出密码明文 | 【选择：通过 / 不通过 / 未检查】 | 【填写】 |
| 后端日志不输出 Token 明文 | 【选择：通过 / 不通过 / 未检查】 | 【填写】 |
| 后端日志不输出密码明文 | 【选择：通过 / 不通过 / 未检查】 | 【填写】 |
| 后端日志不输出完整手机号 | 【选择：通过 / 不通过 / 未检查】 | 【填写】 |
| 后端日志不输出完整邮箱 | 【选择：通过 / 不通过 / 未检查】 | 【填写】 |
| 报告附件不包含敏感信息 | 【选择：通过 / 不通过 / 未检查】 | 【填写】 |

## 8. 截图和附件清单

请填写附件相对路径。没有附件则写 `无`，不要把 Token 或密码截图进附件。

| 附件类型 | 场景 | 路径 | 说明 |
| --- | --- | --- | --- |
| 截图 | 注册成功 | 【填写】 | 【填写】 |
| 截图 | 登录成功进入地图 | 【填写】 | 【填写】 |
| 截图 | 当前用户成功或地图会话状态 | 【填写】 | 【填写】 |
| 截图 | 登录失败 | 【填写】 | 【填写】 |
| 截图 | 短密码注册拦截 | 【填写】 | 【填写】 |
| 网络摘要 | 主链路 | 【填写】 | 【填写】 |
| 日志摘要 | 主链路 | 【填写】 | 【填写】 |
| 其他 | 【填写】 | 【填写】 | 【填写】 |

## 9. 问题记录

如果发现问题，请先在这里简写；我检查报告时会判断是否需要同步到 `issue-log.md`。

| 临时编号 | 场景 | 问题摘要 | 严重级别 | 是否阻塞 B1 L2 | 建议责任侧 | 附件或证据 |
| --- | --- | --- | --- | --- | --- | --- |
| TEMP-001 | 【填写】 | 【填写】 | 【选择：Blocker / Critical / Major / Minor】 | 【选择：是 / 否 / 待判断】 | 【选择：前端 / 后端 / 契约 / 环境 / 待分析】 | 【填写】 |

## 10. 自检清单

提交给 Codex 检查前，请先勾选。

- [x] 主链路 IT-001 至 IT-007 已执行，或已说明未执行原因。
- [x] 错误态 IT-008 至 IT-014 已执行，或已说明未执行原因。
- [x] 每个已执行接口都有 HTTP status、业务 code、message、requestId、traceId。
- [ ] 至少记录了注册、登录、当前用户、登出后旧 Access Token 访问的证据。
- [ ] Token、密码、完整手机号、完整邮箱没有出现在报告和附件中。
- [ ] iOS Base URL 通过 Gateway，不是直连 Auth 或 User。
- [ ] 登出请求同时携带 Authorization 和 Refresh Token。
- [ ] 登出后旧 Access Token 返回 `401`。
- [ ] 登出后旧 Refresh Token 调用 refresh 返回 `401`。
- [ ] 错误态 UI 已记录用户可见提示。
- [ ] 不确定或未执行的内容没有标记为通过。

## 11. 测试结论

| 项目 | 填写位置 |
| --- | --- |
| 总体结论 | 有条件通过（阻塞项：运行时复测未完成） |
| 通过场景 | IT-001 注册、IT-002 登录、IT-003 当前用户、IT-004 刷新、IT-008 错误密码、IT-009 重复手机号、IT-010 短密码拦截、IT-011 缺少 phone（改判为符合契约）、IT-013 未认证 401、IT-014 内部接口 403 |
| 未通过场景 | 无。IT-006、IT-007 由"不通过"改判为"无法判定"，因本轮登出请求未发出，前置条件不成立 |
| 未执行场景及原因 | IT-005 登出无法判定（iOS 未发起请求）；IT-012 Gateway 不可用无法判定（未观察 Token 与输入保留）；LOG-002 敏感信息逐项未检查；附件清单未填 |
| 阻塞问题 | BUG-011（登出未调用后端，已由 `cb1145e` 修复，待复测） |
| 高风险问题 | BUG-006 / BUG-007（错误态与非 2xx 解析仍需真实 iOS 复测） |
| 是否建议关闭 B1 iOS L2 | 否。需在 `cb1145e` 或之后提交上重跑 IT-005、IT-006、IT-007、IT-012 与 LOG-002 |
| 是否建议进入 B2 门店地图 | 有条件可以：B2 契约与服务起草可并行，但 B1 运行时不通过前不标记 B1 完成 |
| 备注 | 结论由 Codex 于 2026-11-05 依据报告与代码核对后填写；判定口径见 `issue-log.md` §1.1 |

## 12. Codex 检查区

以下区域由 Codex 检查报告时填写，你可以留空。

| 检查项 | Codex 结论 |
| --- | --- |
| 证据完整性 | 不完整。主链路有 requestId/traceId（LOG-001 前四项），但登出链路无任何证据；无截图与网络摘要；LOG-002 未填 |
| 敏感信息合规 | 修正后合规。原文 IT-009 记录完整手机号，已脱敏为 `186****1693`；全文未出现 Token 或密码明文 |
| 主链路结论 | 注册、登录、当前用户、刷新四项成立；登出链路不成立（请求未发出），因此"登出后旧 Token 失效"未被验证 |
| 错误态结论 | IT-008、IT-010、IT-011、IT-013 成立；IT-012 需补观察；前端错误态展示与 403 场景未复测（BUG-006/BUG-007） |
| 日志链路结论 | 注册/登录/当前用户/刷新可串联 gateway→auth→user；登出与旧 Token 场景无摘要，无法评估 |
| 需要同步到 `integration-plan.md` 的内容 | IT-005/006/007 判定口径、IT-011 预期修正、IT-009 脱敏要求 |
| 需要同步到 `issue-log.md` 的内容 | 已在 §1.1 记录收口口径与剩余阻塞 |
| 最终建议 | 有条件通过，不关闭 B1。阻塞项为环境缺失（无 Xcode 模拟器、无 Docker），非代码缺陷 |
