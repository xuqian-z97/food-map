---
name: foodmap-integration-coordination
description: Use when starting, planning, executing, recording, or reviewing FoodMap frontend-backend integration work, including creating docs/integration folders, coordinating main/backend/frontend/QA agents, recording integration bugs, collecting requestId/traceId evidence, or deciding whether an integration passes.
---

# FoodMap 前后端联调协调 Skill

## 使用时机

当用户提出以下任务时使用本 skill：

- 开始前后端联调。
- 生成或更新联调说明。
- 记录联调问题或 BUG。
- 复测联调问题。
- 判断某次联调是否通过。
- 判断某个前端或后端功能是否已经到达联调安全点。
- 要求主代理、后端观察子代理、前端观察子代理、联调验收子代理协作。

## 必读文档

- `AGENTS.md`
- `CODEX-gen.md`
- `CODEX-front.md`
- `CODEX-after.md`
- `docs/integration/README.md`
- `docs/integration/templates/integration-plan.md`
- `docs/integration/templates/issue-log.md`
- `harness/acceptance-checklist.md`
- `.agents/backend-agent.md`
- `.agents/frontend-agent.md`
- `.agents/qa-agent.md`

## 工作流程

1. 确认本次联调所属迭代和功能，例如 `B1-auth-ios-backend`。
2. 判断当前业务切片是否到达联调安全点：
   - 后端接口、DTO、错误码、日志和测试数据是否可用。
   - 前端页面、交互、真实请求入口和状态展示是否可用。
   - API 契约、环境、数据和 `requestId/traceId` 是否可验证。
3. 如果只完成了后端或前端一侧，必须说明另一侧需要达到的最小开发程度，再建议继续开发或先联调。
4. 在 `docs/integration/<iteration>-<feature>/` 下创建或更新：
   - `integration-plan.md`
   - `issue-log.md`
5. 在 `integration-plan.md` 写清：
   - 联调目标和不纳入范围。
   - 联调安全点和准入检查。
   - 前端职责：页面、交互、状态、网络请求和证据。
   - 后端职责：服务、接口、入参、响应、错误码和日志。
   - API 契约、测试数据、联调环境、验收场景和提交记录。
6. 按用户要求或任务复杂度启用子代理：
   - 后端观察子代理只读检查后端服务、接口、日志和数据证据。
   - 前端观察子代理只读检查页面、交互、状态和网络证据。
   - 联调验收子代理只读判断通过情况、BUG 风险和复测闭环。
7. 联调失败时，在 `issue-log.md` 新增 BUG 条目，必须包含：
   - 复现步骤。
   - 测试数据。
   - 前端现象和网络请求摘要。
   - 后端日志摘要、`requestId`、`traceId`。
   - 初步归因、建议修复范围、修复提交和复测结论。
8. 修复后按原步骤复测，并更新 `issue-log.md`。
9. 联调结束前运行可用校验：
   - `./harness/scripts/validate-integration.sh`
   - `./harness/scripts/run-all.sh`

## 业务切片推进规则

FoodMap 默认按可联调业务切片推进，而不是前端队列和后端队列各自走到底。

每完成一个前端或后端功能，主代理必须判断：

- 当前功能是否形成用户路径的一部分。
- 是否已经到达联调安全点。
- 是否应该先暂停继续开发下一个强依赖功能，转入联调。
- 如果尚未到达安全点，另一端需要补到什么最小程度。

示例：

- 后端登录接口完成后，应提示前端至少需要 `LoginView + APIClient + Token 保存 + 登录后跳转` 才能进入 B1 认证联调。
- 后端地图视野查询接口完成后，应提示前端至少需要地图首页壳、bbox 获取、真实请求和点位/列表展示，才适合进入门店地图联调。

## 联调安全点

联调安全点表示前后端都到达可观测、可验证、可复测的节点。

准入检查必须覆盖：

- API 契约稳定：路径、方法、入参、响应、错误码明确。
- 后端可调用：接口能通过 curl、Postman 或自动测试调通。
- 前端可触发：页面或 ViewModel 能发起真实请求。
- 环境可运行：必要服务、数据库、中间件和配置可启动。
- 测试数据可准备：账号、门店、推荐、关系或媒体数据有创建和清理方式。
- 日志可追踪：能获得 `requestId`、`traceId`，失败时能查前后端日志。
- 范围已冻结：本次联调验什么、不验什么写入 `integration-plan.md`。

## 联调等级

- `L0 契约联调`：只确认 API、DTO、字段、枚举和错误码。
- `L1 Mock 联调`：前端使用 mock 或后端占位响应验证页面流程。
- `L2 本地真实联调`：前端调用本地真实后端服务。
- `L3 环境联调`：在 OrbStack、测试环境或 prod-like 环境跑完整服务链路。
- `L4 验收联调`：包含日志证据、测试数据、BUG 复测和最终结论。

正式业务联调原则上至少从 `L2` 开始。权限、隐私、Token、可见范围、PUBLIC 统计、上传和支付类高风险能力必须推进到 `L3` 或 `L4` 才能认为完成。

## 验收判定

联调结论只能使用以下四类：

- `通过`：必测场景全部执行并有证据，阻塞级和高风险 BUG 已关闭。
- `有条件通过`：主流程可用，仅存在明确记录的非阻塞问题，并有责任人和复测计划。
- `不通过`：主流程失败，或出现权限、隐私、Token、可见范围、PUBLIC 统计口径等高风险问题。
- `无法判定`：缺少环境、构建、测试、接口调用、截图/录屏、日志等关键证据。

不能把未执行、未观察或缺少证据的场景写成通过。

## 敏感信息规则

联调文档、日志摘要和问题记录禁止保存：

- Access Token / Refresh Token 明文。
- 密码、密钥。
- 完整手机号、完整邮箱。
- 私密推荐正文、评论正文。
- SQL 明文敏感参数。

只允许记录脱敏摘要、业务主键、HTTP 状态、业务错误码、`requestId` 和 `traceId`。
