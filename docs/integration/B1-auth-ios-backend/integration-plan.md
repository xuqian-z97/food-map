# B1 认证前后端联调说明

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| 联调名称 | B1 认证 iOS 与后端联调 |
| 迭代编号 | B1 |
| 联调文件夹 | `docs/integration/B1-auth-ios-backend` |
| 创建时间 | 2026-06-13 |
| 最近更新 | 2026-06-22 |
| 主代理 | Codex 主代理 |
| 后端观察子代理 | 后端子代理，观察 `after/foodmap-auth-service`、`after/foodmap-user-service`、`after/foodmap-gateway-service` |
| 前端观察子代理 | 前端子代理，观察 `front/FoodMapApp` 认证页面、会话状态和网络层 |
| 联调验收子代理 | QA 子代理，按本文档和 `issue-log.md` 判定 |
| 前端提交 | 待补齐 B1 iOS 阻断项后填写 |
| 后端提交 | `b37929c feat: complete B1 auth backend integration` |
| 文档提交 | 本次计划调整待提交 |
| 相关文档 | `CODEX-front.md`、`CODEX-after.md`、`CODEX-gen.md`、`docs/api/auth-user.md`、`docs/superpowers/plans/2026-06-22-b1-auth-ios-backend-integration.md` |

## 2. 联调目标

本次联调需要验证的功能：

- [ ] iOS 登录页通过 Gateway 调用真实认证登录接口。
- [ ] 登录成功后保存 Token，并进入地图首页。
- [ ] iOS 注册页通过 Gateway 调用真实注册接口。
- [ ] 注册成功后认证服务通过用户服务创建用户资料，随后可查询 `/api/users/me`。
- [ ] iOS 使用 Access Token 通过 Gateway 查询 `/api/users/me`，展示或缓存真实当前用户身份。
- [x] 后端通过 Gateway 验证注册、登录、当前用户、内部接口拦截、accountId 归属校验和失败回滚。
- [ ] 登录失败、参数错误、账号不存在、密码错误、未认证、权限不足和服务异常时，前端展示明确错误。
- [ ] Token、密码、完整手机号、完整邮箱不出现在前端日志、后端日志、网络摘要或问题记录中。
- [ ] 后端访问日志包含 `requestId`、`traceId`、`serviceName`，并能串起 Gateway/Auth/User 调用链路。

本次不纳入联调范围的功能：

- [ ] 高德地图真实 SDK 渲染和定位授权。
- [ ] 门店搜索、推荐创建、好友和情侣关系。
- [ ] Refresh Token 自动续期完整闭环；如前端尚未实现，只验证未认证/登录失效的错误展示。
- [ ] 生产环境 HTTPS、证书和正式域名配置。

## 3. 当前审查结论

完整 iOS 前后端联调当前不建议开始，结论为：`暂不通过联调安全点`。

已具备的能力：

- 后端 Gateway/Auth/User 本地 L2 联调已通过，提交为 `b37929c`。
- iOS 工程 `FoodMapApp` 可通过命令行 Debug 构建。
- iOS 登录和注册页面已经有基本表单、加载态和 Keychain Token 存储能力。
- iOS 注册请求体是扁平 JSON，字段与后端注册契约基本一致。
- 当前前端代码未发现 `print`、`debugPrint`、`NSLog` 直接输出 Token 或密码。

阻断完整联调的前端问题：

- `LoginViewModel` 默认 Base URL 仍为 `http://127.0.0.1:8081`，未默认走 Gateway。
- `APIClient` 目前只有登录/注册 POST，不支持通用 GET、`Authorization: Bearer` 和 `/api/users/me`。
- `APIResponse` 未建模 `status`，`APIClient` 对非 2xx 直接丢弃后端统一错误体，无法稳定展示业务错误。
- `AuthSessionStore` 启动恢复 Token 时用 `accountId/userId = 0` 占位，未通过 `/api/users/me` 校验真实会话。
- 地图首页仍使用本地样例点位和缓存，不阻断 B1 认证联调，但不能作为地图业务联调证据。

## 4. 前后端开发计划

### 4.1 前端开发计划

| 优先级 | 任务 | 文件范围 | 完成标准 |
| --- | --- | --- | --- |
| P0 | 统一 B1 联调入口到 Gateway | `front/FoodMapApp/FoodMapApp/Features/Auth/LoginViewModel.swift`、`front/FoodMapApp/FoodMapApp/Features/Auth/RegisterView.swift`、`front/FoodMapApp/README.md` | 默认或推荐 Base URL 为 `http://127.0.0.1:18080`；登录和注册都通过 Gateway 请求 `/api/auth/**`；README 说明模拟器和真机地址差异 |
| P0 | 扩展 `APIClient` 通用请求能力 | `Core/Networking/APIClient.swift`、`APIResponse.swift`、`NetworkError.swift`、`APIRequestTimeout.swift` | 支持 GET/POST、请求超时、`Authorization: Bearer`、`X-Request-Id`、`X-Trace-Id`、统一成功和失败响应解析 |
| P0 | 接入当前用户接口 | `Features/Auth/AuthModels.swift`、`Core/Auth/AuthSessionStore.swift`、必要时新增 `Features/Profile` 或 `Core/Auth/CurrentUser` 模型 | 登录成功后调用 Gateway `GET /api/users/me`；Token 恢复后先拉取当前用户，失败则回到登录页并清理无效会话；会话中不再出现 `accountId/userId = 0` 占位 |
| P0 | 完成错误展示分类 | `LoginView.swift`、`RegisterView.swift`、`NetworkError.swift` | 400 参数错误、401 未认证、403 权限不足、409 冲突、500/504 服务异常、网络不可用均展示可理解提示，并保留可修正输入 |
| P1 | 增加前端测试靶点 | `front/FoodMapApp` Xcode project、`front/FoodMapApp/FoodMapAppTests` | 至少覆盖登录成功、注册成功、当前用户成功、401、403、409、网络失败和 Token 清理；使用 mock `URLProtocol`，不请求真实后端 |
| P1 | 补齐联调证据 | `docs/integration/B1-auth-ios-backend/screenshots`、`network`、`tests` | 保存登录成功、注册成功、登录失败、当前用户成功的截图或录屏，以及脱敏网络摘要 |

### 4.2 后端开发计划

| 优先级 | 任务 | 文件范围 | 完成标准 |
| --- | --- | --- | --- |
| P0 | 固化 Gateway 联调启动方式 | `after/scripts`、`docs/integration/B1-auth-ios-backend/integration-plan.md`、必要时 `front/FoodMapApp/README.md` | 能一键或按文档启动 Gateway 18080、Auth 18081、User 18082，前端只需要配置 Gateway Base URL |
| P0 | 复测 Gateway 当前用户链路 | `after/foodmap-gateway-service`、`after/foodmap-user-service` 测试或联调脚本 | Gateway 覆盖外部伪造身份头，`GET /api/users/me` 返回 Token 对应用户资料，错配 accountId 返回 403 |
| P0 | 保持统一响应契约 | `docs/api/auth-user.md`、后端统一响应类和异常处理 | 正常和异常响应均包含 `success/status/code/message/data`，错误不暴露异常类名、SQL、Token 或内部地址 |
| P1 | 保留后端回归用例 | `after/foodmap-auth-service`、`after/foodmap-user-service`、`after/foodmap-gateway-service` 测试 | internal 外部拦截、用户开通失败回滚、accountId 错配、敏感日志脱敏持续通过 |
| P1 | 提供联调数据和日志摘要 | `docs/integration/B1-auth-ios-backend/network`、`logs` | 每个失败场景能记录脱敏 `requestId/traceId`、HTTP status、业务 code、服务名和排查摘要 |

## 5. 联调安全点

本次正式联调目标等级：

- [ ] L0 契约联调
- [ ] L1 Mock 联调
- [x] L2 本地真实联调
- [ ] L3 环境联调
- [ ] L4 验收联调

准入检查：

| 检查项 | 前端状态 | 后端状态 | 结论 |
| --- | --- | --- | --- |
| API 契约已确认 | 需按 `status`、Bearer Token、`/api/users/me` 更新模型 | `docs/api/auth-user.md` 已描述注册、登录、当前用户和内部开通 | 有条件确认，前端需补代码 |
| 前端可发起真实请求 | 登录/注册可发 POST；缺 Gateway 默认、Bearer GET、当前用户 | - | 未通过 |
| 后端接口可被 curl/Postman/测试调通 | - | Gateway/Auth/User 已通过本地 curl 和 Maven 验证 | 通过 |
| 测试数据已准备 | 需由 iOS 注册或复用后端账号 | 注册接口已创建 `codex_b1_gateway_171349` 等联调账号 | 有条件通过 |
| `requestId` / `traceId` 可追踪 | 需生成或记录脱敏网络摘要 | Gateway/Auth/User 已验证链路追踪 | 后端通过，前端待补 |
| 必要环境已启动 | iOS 模拟器待启动；App 可编译 | Gateway 18080、Auth 18081、User 18082、PostgreSQL 可启动 | 有条件通过 |
| 本次范围已冻结 | 登录、注册、当前用户、错误态、敏感日志 | 登录、注册、当前用户、internal 拦截、回滚、accountId 校验 | 已确认 |

联调开始前结论：

- [ ] 可以开始完整 iOS 前后端联调
- [x] 暂不开始，前端需要补齐：Gateway Base URL、Bearer 请求、`/api/users/me`、统一错误解析、Token 恢复校验和基本测试。
- [ ] 暂不开始，后端需要补齐：
- [ ] 暂不开始，环境或测试数据需要补齐：

完成以下门禁后，才允许把 B1 从“后端 L2 已通过”推进到“完整 iOS L2 联调”：

1. iOS Debug 构建通过。
2. `APIClient` 能通过 Gateway 完成注册、登录和当前用户查询。
3. 登录成功后 Keychain 有 Token，`AuthSessionStore.session` 中 `accountId/userId` 为后端真实值。
4. Token 恢复时先调用 `/api/users/me`；401/403 时清理本地会话并回登录页。
5. 登录失败、注册参数错误、账号冲突、服务不可用均有 UI 错误提示。
6. 前端和后端日志、问题记录不保存 Token、密码、完整手机号和完整邮箱。
7. 至少保留一组脱敏网络摘要和一组后端 `requestId/traceId` 日志摘要。

## 6. 前端职责

| 页面/模块 | 交互 | 预期效果 | 状态要求 | 证据 |
| --- | --- | --- | --- | --- |
| LoginView | 输入账号、手机号或邮箱和密码后提交 | 通过 Gateway 调用登录接口，成功后保存 Token 并进入地图入口 | 加载中、成功、失败、网络不可用 | 截图、网络请求摘要 |
| RegisterView | 输入账号、手机号、邮箱、密码、昵称后提交 | 通过 Gateway 调用注册接口，成功后提示注册结果，随后可登录 | 加载中、成功、参数错误、账号冲突、失败 | 截图、网络请求摘要 |
| AuthSessionStore | 登录成功、App 启动、退出登录 | Token 写入 Keychain；启动恢复时通过 `/api/users/me` 校验；退出清理 Token 和业务缓存 | 登录态恢复、登录失效、退出登录清理 | 单元测试或手工验证 |
| APIClient | 构建请求和解析响应 | 按 `success/status/code/message/data` 处理；支持 Bearer Token 和链路头 | 401、403、400、409、500/504 分类 | 请求/响应摘要 |

前端必须说明：

- 登录入口、注册入口、当前用户查询触发时机和成功后的导航路径。
- iOS 版本、模拟器或真机型号、Xcode 版本、App 构建号、后端 Base URL。
- 失败时是否保留用户已输入内容。
- 网络不可用、服务不可用、登录失效、权限不足、参数错误时的 UI 表达。
- 是否产生截图、录屏、网络摘要或测试输出。

## 7. 后端职责

| 服务 | 接口 | 方法 | 入参 | 响应 | 错误码 | 日志要求 |
| --- | --- | --- | --- | --- | --- | --- |
| gateway | `/api/auth/login` | POST | 登录标识、密码 | Access Token、Refresh Token、过期时间 | 400/401/500 | 生成或透传 `requestId`、`traceId` |
| gateway | `/api/auth/register` | POST | 账号、手机号、邮箱、密码、昵称、渠道 | 账号业务主键和用户业务主键 | 400/409/500/504 | 生成或透传 `requestId`、`traceId` |
| gateway | `/api/users/me` | GET | Bearer Token | 当前用户资料 | 401/403/404/500 | 覆盖外部伪造身份头，透传可信身份 |
| gateway | `/internal/users/provision` | POST | 用户开通载荷 | 外部访问返回 403 | 403 | 不转发到用户服务 |
| auth-service | `/api/auth/login` | POST | 登录标识、密码 | Token 结果 | 400/401/500 | 不记录密码和 Token 明文 |
| auth-service | `/api/auth/register` | POST | 注册参数 | 注册结果 | 400/409/504/500 | 用户开通失败时回滚 auth 写入 |
| user-service | `/api/users/me` | GET | 当前用户身份 | 用户资料 | 401/403/404/500 | 校验 `userId/accountId` 归属，不记录敏感信息 |

后端必须说明：

- 本地启动 profile、端口、Gateway 路由和 Nacos/直连模式。
- Auth/User 数据库是否已执行 Flyway。
- 登录、注册、查询当前用户是否具备访问日志。
- 响应体是否符合统一结构。
- 是否能通过同一 `requestId` 或 `traceId` 串起 Gateway、认证服务和用户服务日志。

## 8. API 契约

以 `docs/api/auth-user.md` 为准。联调前如发现前后端字段不一致，必须先更新 API 文档和 DTO，再执行联调。

前端必须通过 Gateway 调用外部 API：

```text
POST http://127.0.0.1:18080/api/auth/register
POST http://127.0.0.1:18080/api/auth/login
GET  http://127.0.0.1:18080/api/users/me
```

`/api/users/me` 必须携带：

```http
Authorization: Bearer <access-token>
X-Request-Id: <request-id>
X-Trace-Id: <trace-id>
```

`X-FoodMap-*` 可信身份头由 Gateway 写入，前端不能伪造。

## 9. 测试数据

| 数据类型 | 数据值 | 创建方式 | 使用场景 | 清理方式 |
| --- | --- | --- | --- | --- |
| 新注册账号 | `codex_auth_it_<date>` | iOS 注册页或 Gateway 注册接口 | 注册成功、登录成功、当前用户查询 | 可保留为联调账号 |
| 后端已验证账号 | `codex_b1_gateway_171349` | 后端 Gateway 注册接口 | 后端回归和前端登录复测 | 可保留为联调账号 |
| 错误密码 | 不写入文档 | 手工输入 | 登录失败 | 不记录 |
| Token | 只记录是否存在 | 登录接口返回 | 当前用户接口 | 不记录明文 |

## 10. 联调环境

| 组件 | 地址/配置 | 状态 |
| --- | --- | --- |
| iOS App | `front/FoodMapApp` | 命令行 Debug 构建通过，模拟器手工联调待执行 |
| iOS 版本 / 设备 | 待填写 | 待确认 |
| Xcode / App 构建号 | Xcode 命令行可构建 | 待记录具体版本 |
| Gateway | `http://127.0.0.1:18080`，前端联调唯一入口 | 后端已验证后停止，联调时需重启 |
| Auth Service | `http://127.0.0.1:18081`，本地可用 `AUTH_USER_SERVICE_BASE_URL=http://127.0.0.1:18082` | 后端已验证后停止，联调时需重启 |
| User Service | `http://127.0.0.1:18082` | 后端已验证后停止，联调时需重启 |
| PostgreSQL | `foodmap_auth_db`、`foodmap_user_db` | 已确认 Flyway 和联调数据 |
| Redis | 如认证会话需要 | 待确认 |
| 日志服务 | B1.5 能力可用于排查 | 可选 |

## 11. 证据附件

可选附件目录：

```text
screenshots/
recordings/
network/
logs/
tests/
```

本次 B1 认证联调建议至少保留：

- 注册成功或注册失败截图。
- 登录成功和登录失败截图。
- 当前用户查询成功网络摘要。
- 登录接口、注册接口、当前用户接口的脱敏网络摘要。
- 失败场景对应 `requestId` 和 `traceId`。

## 12. 验收场景

| 编号 | 场景 | 操作步骤 | 预期结果 | 通过情况 | 证据 |
| --- | --- | --- | --- | --- | --- |
| IT-001 | 注册成功 | iOS 通过 Gateway `POST /api/auth/register` 提交合法注册信息 | 后端创建账号和用户资料，返回账号和用户业务主键 | 后端通过，前端未执行 | 后端：`requestId=codex-b1-gateway-register`，`traceId=codex-b1-gateway-trace`，`accountId=100010`，`userId=200010` |
| IT-002 | 登录成功 | iOS 通过 Gateway `POST /api/auth/login` 输入已注册账号和正确密码 | 返回 Token，Keychain 保存 Token，进入地图首页 | 后端通过，前端未执行 | 后端：`requestId=codex-b1-gateway-login`，Token 已脱敏 |
| IT-003 | 登录失败 | iOS 输入错误密码并提交 | 后端返回稳定错误，前端展示明确失败原因并不清空账号 | 未执行 |  |
| IT-004 | 当前用户 | iOS 使用登录 Token 通过 Gateway `GET /api/users/me` | Gateway 覆盖伪造身份头，下游返回 Token 对应用户资料，前端会话使用真实 `userId/accountId` | 后端通过，前端未执行 | 后端：`requestId=codex-b1-gateway-me`，返回 `userId=200010/accountId=100010` |
| IT-005 | 敏感日志检查 | 检查前端日志、后端日志和问题记录 | 不出现 Token、密码明文，手机号和邮箱脱敏 | 后端通过，前端未执行 | 后端 SQL 日志中手机号、邮箱、密码哈希、Token 类字段已脱敏 |
| IT-006 | 内部接口外部拦截 | Gateway `POST /internal/users/provision` | 返回 `403/FORBIDDEN`，不转发到用户服务 | 后端通过 | `requestId=codex-b1-internal-forbidden` |
| IT-007 | accountId 错配 | 直连 User `GET /api/users/me`，userId 正确但 accountId 错误 | 返回 `403/FORBIDDEN` | 后端通过 | `requestId=codex-b1-account-mismatch` |
| IT-008 | 用户开通失败回滚 | Auth 直连坏用户服务 URL 后注册 | 返回 `504/GATEWAY_TIMEOUT`，auth 账号和凭证不落库 | 后端通过 | `requestId=codex-b1-rollback-request`，`auth_accounts=0`，`auth_credentials=0` |

## 13. 日志和排查要求

每个失败场景至少记录：

- `requestId`
- `traceId`
- 前端网络请求摘要
- 后端服务名和接口路径
- 后端日志摘要
- 数据库状态摘要，如适用

禁止记录：

- Access Token / Refresh Token 明文
- 密码、密钥
- 完整手机号、完整邮箱
- 私密推荐正文、评论正文
- SQL 明文敏感参数

## 14. 验收判定口径

- `通过`：必测场景全部执行并有证据，阻塞级和高风险 BUG 已关闭。
- `有条件通过`：主流程可用，仅存在明确记录的非阻塞问题，并有责任人和复测计划。
- `不通过`：主流程失败，或出现权限、隐私、Token、认证状态串读等高风险问题。
- `无法判定`：缺少环境、构建、测试、接口调用、截图/录屏、日志等关键证据。

## 15. 联调结论

| 项目 | 结论 |
| --- | --- |
| 是否通过 | 后端 L2 本地真实联调通过；完整 iOS 前后端联调未达到安全点 |
| 阻塞问题 | 前端缺少 Gateway 默认入口、Bearer 请求、`/api/users/me` 和 Token 恢复真实校验 |
| 高风险问题 | 若继续联调，可能出现会话 `userId/accountId=0`、错误体丢失和当前用户链路无法验证 |
| 已执行验证 | `xcodebuild` Debug 构建、`validate-ios.sh`、后端 Maven 定向测试、`mvn validate`、Gateway/Auth/User curl、本地 PostgreSQL 落库检查 |
| 未执行验证及原因 | iOS 模拟器手工联调、前端页面操作、当前用户查询 UI 和前端错误态未执行；前端代码未到安全点 |
| 后续动作 | 按 `docs/superpowers/plans/2026-06-22-b1-auth-ios-backend-integration.md` 先补前端 P0，再执行完整 iOS L2 联调 |
