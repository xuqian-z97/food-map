# B1 认证前后端联调说明

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| 联调名称 | B1 认证 iOS 与后端联调 |
| 迭代编号 | B1 |
| 联调文件夹 | `docs/integration/B1-auth-ios-backend` |
| 创建时间 | 2026-06-13 |
| 主代理 | Codex 主代理 |
| 后端观察子代理 | 后端子代理，观察 `after/foodmap-auth-service`、`after/foodmap-user-service`、`after/foodmap-gateway-service` |
| 前端观察子代理 | 前端子代理，观察 `front/FoodMapApp` 认证页面和网络层 |
| 联调验收子代理 | QA 子代理，按本文档和 `issue-log.md` 判定 |
| 前端提交 | 待联调前填写 |
| 后端提交 | 未提交，当前工作区后端变更已完成本地验证 |
| 文档提交 | 未提交，当前工作区联调文档已更新 |
| 相关文档 | `CODEX-front.md`、`CODEX-after.md`、`CODEX-gen.md`、`docs/api/auth-user.md` |

## 2. 联调目标

本次联调需要验证的功能：

- [ ] iOS 登录页通过真实后端认证接口登录。
- [ ] 登录成功后保存 Token，并进入地图首页或地图占位页。
- [ ] iOS 注册页通过真实后端认证接口注册账号。
- [x] 后端通过 Gateway 验证注册、登录、当前用户和内部接口拦截。
- [ ] 登录失败、参数错误、账号不存在或密码错误时展示明确错误。
- [ ] Token 不出现在前端日志、后端日志或问题记录中。
- [ ] 后端访问日志包含 `requestId`、`traceId`、`serviceName`。

本次不纳入联调范围的功能：

- [ ] 高德地图真实 SDK 渲染。
- [ ] 门店搜索、推荐创建、好友和情侣关系。
- [ ] Refresh Token 自动续期完整闭环，如前端尚未实现，可记录为后续项。

## 3. 联调安全点

本次联调等级：

- [ ] L0 契约联调
- [ ] L1 Mock 联调
- [x] L2 本地真实联调
- [ ] L3 环境联调
- [ ] L4 验收联调

准入检查：

| 检查项 | 前端状态 | 后端状态 | 结论 |
| --- | --- | --- | --- |
| API 契约已确认 | `docs/api/auth-user.md` 待前端复核 | `docs/api/auth-user.md` 已同步 internal 拦截、事务回滚和 accountId 校验 | 后端已确认 |
| 前端可发起真实请求 | LoginView、RegisterView、APIClient 待联调确认 | - | 前端未确认 |
| 后端接口可被 curl/Postman/测试调通 | - | Gateway/Auth/User 已通过 curl 验证 | 后端通过 |
| 测试数据已准备 | 联调账号命名已规划 | 注册接口已创建 `codex_b1_gateway_171349` | 后端通过 |
| `requestId` / `traceId` 可追踪 | 前端需记录脱敏网络摘要 | `codex-b1-gateway-trace` 已串起 Gateway/Auth/User | 后端通过 |
| 必要环境已启动 | iOS 模拟器待启动 | Gateway 18080、Auth 18081、User 18082、PostgreSQL 已启动并完成验证 | 后端通过 |
| 本次范围已冻结 | 登录、注册、当前用户 | 登录、注册、当前用户 | 已确认 |

联调开始前结论：

- [x] 可以开始后端本地真实联调
- [x] 暂不开始，前端需要补齐：确认 LoginView / RegisterView 可通过 APIClient 指向真实后端 Base URL。
- [ ] 暂不开始，后端需要补齐：
- [ ] 暂不开始，环境或测试数据需要补齐：

## 4. 前端职责

| 页面/模块 | 交互 | 预期效果 | 状态要求 | 证据 |
| --- | --- | --- | --- | --- |
| LoginView | 输入账号、手机号或邮箱和密码后提交 | 调用登录接口，成功后进入地图入口 | 加载中、成功、失败、网络不可用 | 截图、网络请求摘要 |
| RegisterView | 输入账号、手机号、邮箱、密码、昵称后提交 | 调用注册接口，成功后提示注册结果 | 加载中、成功、参数错误、失败 | 截图、网络请求摘要 |
| AuthSessionStore | 保存登录态 | Token 写入 Keychain，不写日志 | 登录态恢复、退出登录清理 | 单元测试或手工验证 |
| APIClient | 解析统一响应 | 按 `success/status/code/message/data` 处理 | 401、403、400、500 分类 | 请求/响应摘要 |

前端必须说明：

- 登录入口、注册入口和成功后的导航路径。
- iOS 版本、模拟器或真机型号、Xcode 版本、App 构建号、后端 Base URL。
- 失败时是否保留用户已输入内容。
- 网络不可用、服务不可用、登录失效时的 UI 表达。
- 是否产生截图或录屏证据。

## 5. 后端职责

| 服务 | 接口 | 方法 | 入参 | 响应 | 错误码 | 日志要求 |
| --- | --- | --- | --- | --- | --- | --- |
| gateway | `/api/auth/login` | POST | 登录标识、密码 | Access Token、Refresh Token、过期时间 | 400/401/500 | 透传 `requestId`、`traceId` |
| gateway | `/api/auth/register` | POST | 账号、手机号、邮箱、密码、昵称、渠道 | 账号业务主键和用户摘要 | 400/409/500 | 透传 `requestId`、`traceId` |
| gateway | `/internal/users/provision` | POST | 用户开通载荷 | 外部访问返回 403 | 403 | 记录 `requestId`、`traceId` |
| auth-service | `/api/auth/login` | POST | 登录标识、密码 | Token 结果 | 400/401/500 | 不记录密码和 Token 明文 |
| auth-service | `/api/auth/register` | POST | 注册参数 | 注册结果 | 400/409/500 | 不记录密码明文 |
| user-service | `/api/users/me` | GET | 当前用户身份 | 用户资料 | 401/404/500 | 不记录敏感信息 |

后端必须说明：

- 本地启动 profile 和端口。
- 网关路由是否启用。
- 认证服务和用户服务数据库是否已执行 Flyway。
- 登录、注册、查询当前用户是否具备访问日志。
- 响应体是否符合统一结构。
- 是否能通过同一 `requestId` 或 `traceId` 串起网关、认证服务和用户服务日志。

## 6. API 契约

以 `docs/api/auth-user.md` 为准。联调前如发现前后端字段不一致，必须先更新 API 文档和 DTO，再执行联调。

## 7. 测试数据

| 数据类型 | 数据值 | 创建方式 | 使用场景 | 清理方式 |
| --- | --- | --- | --- | --- |
| 新注册账号 | `codex_auth_it_<date>` | 注册接口 | 注册成功、登录成功 | 可保留为联调账号 |
| 错误密码 | 不写入文档 | 手工输入 | 登录失败 | 不记录 |
| Token | 只记录是否存在 | 登录接口返回 | 后续用户接口 | 不记录明文 |

## 8. 联调环境

| 组件 | 地址/配置 | 状态 |
| --- | --- | --- |
| iOS App | `front/FoodMapApp` | 待启动 |
| iOS 版本 / 设备 | 待填写 | 待确认 |
| Xcode / App 构建号 | 待填写 | 待确认 |
| Gateway | `http://127.0.0.1:18080`，本地直连 auth/user 路由覆盖 | 已启动并验证后停止 |
| Auth Service | `http://127.0.0.1:18081`，`AUTH_USER_SERVICE_URL=http://127.0.0.1:18082` | 已启动并验证后停止 |
| User Service | `http://127.0.0.1:18082` | 已启动并验证后停止 |
| PostgreSQL | `foodmap_auth_db`、`foodmap_user_db` | 已确认 Flyway version 3，联调数据已落库 |
| Redis | 如认证会话需要 | 待确认 |
| 日志服务 | B1.5 能力可用于排查 | 可选 |

## 9. 证据附件

可选附件目录：

```text
screenshots/
recordings/
network/
logs/
tests/
```

本次 B1 认证联调建议至少保留：

- 登录成功和登录失败截图。
- 注册成功或注册失败截图。
- 登录接口和注册接口脱敏网络摘要。
- 失败场景对应 `requestId` 和 `traceId`。

## 10. 验收场景

| 编号 | 场景 | 操作步骤 | 预期结果 | 通过情况 | 证据 |
| --- | --- | --- | --- | --- | --- |
| IT-001 | 注册成功 | Gateway `POST /api/auth/register` 提交合法注册信息 | 后端创建账号和用户资料，返回账号和用户业务主键 | 后端通过，前端未执行 | `requestId=codex-b1-gateway-register`，`traceId=codex-b1-gateway-trace`，`accountId=100010`，`userId=200010` |
| IT-002 | 登录成功 | Gateway `POST /api/auth/login` 输入已注册账号和正确密码 | 返回 Token，后端不打印 Token 明文 | 后端通过，前端未执行 | `requestId=codex-b1-gateway-login`，Token 已脱敏 |
| IT-003 | 登录失败 | iOS 输入错误密码并提交 | 后端返回稳定错误，前端展示明确失败原因 | 未执行 |  |
| IT-004 | 当前用户 | Gateway `GET /api/users/me` 携带登录 Token，并同时伪造身份头 | Gateway 覆盖伪造身份头，下游返回 Token 对应用户资料 | 后端通过，前端未执行 | `requestId=codex-b1-gateway-me`，返回 `userId=200010/accountId=100010` |
| IT-005 | 敏感日志检查 | 检查后端日志和问题记录 | 不出现 Token、密码明文，手机号和邮箱脱敏 | 后端通过，前端未执行 | SQL 日志中手机号、邮箱、密码哈希、Token 类字段已脱敏 |
| IT-006 | 内部接口外部拦截 | Gateway `POST /internal/users/provision` | 返回 `403/FORBIDDEN`，不转发到用户服务 | 后端通过 | `requestId=codex-b1-internal-forbidden` |
| IT-007 | accountId 错配 | 直连 User `GET /api/users/me`，userId 正确但 accountId 错误 | 返回 `403/FORBIDDEN` | 后端通过 | `requestId=codex-b1-account-mismatch` |
| IT-008 | 用户开通失败回滚 | Auth 直连坏用户服务 URL 后注册 | 返回 `504/GATEWAY_TIMEOUT`，auth 账号和凭证不落库 | 后端通过 | `requestId=codex-b1-rollback-request`，`auth_accounts=0`，`auth_credentials=0` |

## 11. 日志和排查要求

每个失败场景至少记录：

- `requestId`
- `traceId`
- 前端网络请求摘要
- 后端服务名和接口路径
- 后端日志摘要
- 数据库状态摘要，如适用

禁止记录 Token、密码、密钥、完整手机号、完整邮箱。

## 12. 验收判定口径

- `通过`：必测场景全部执行并有证据，阻塞级和高风险 BUG 已关闭。
- `有条件通过`：主流程可用，仅存在明确记录的非阻塞问题，并有责任人和复测计划。
- `不通过`：主流程失败，或出现权限、隐私、Token、认证状态串读等高风险问题。
- `无法判定`：缺少环境、构建、测试、接口调用、截图/录屏、日志等关键证据。

## 13. 联调结论

| 项目 | 结论 |
| --- | --- |
| 是否通过 | 后端 L2 本地真实联调通过；完整 iOS 前后端联调未通过/未判定 |
| 阻塞问题 | 后端阻塞问题已修复：internal 外部拦截、auth 失败回滚、accountId 归属校验 |
| 高风险问题 | 当前后端已无已知高风险阻断；前端真实请求和 UI 状态未复测 |
| 已执行验证 | Maven 定向测试、`mvn validate`、Gateway/Auth/User curl、本地 PostgreSQL 落库检查 |
| 未执行验证及原因 | iOS 模拟器、前端页面操作、登录失败 UI 展示未执行；本次用户要求先完成后端修复和后端联调 |
| 后续动作 | 前端将 Base URL 指向 Gateway 后执行完整 iOS 前后端联调，并补充截图/网络摘要 |
