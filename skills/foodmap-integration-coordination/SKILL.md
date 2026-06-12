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
2. 在 `docs/integration/<iteration>-<feature>/` 下创建或更新：
   - `integration-plan.md`
   - `issue-log.md`
3. 在 `integration-plan.md` 写清：
   - 联调目标和不纳入范围。
   - 前端职责：页面、交互、状态、网络请求和证据。
   - 后端职责：服务、接口、入参、响应、错误码和日志。
   - API 契约、测试数据、联调环境、验收场景和提交记录。
4. 按用户要求或任务复杂度启用子代理：
   - 后端观察子代理只读检查后端服务、接口、日志和数据证据。
   - 前端观察子代理只读检查页面、交互、状态和网络证据。
   - 联调验收子代理只读判断通过情况、BUG 风险和复测闭环。
5. 联调失败时，在 `issue-log.md` 新增 BUG 条目，必须包含：
   - 复现步骤。
   - 测试数据。
   - 前端现象和网络请求摘要。
   - 后端日志摘要、`requestId`、`traceId`。
   - 初步归因、建议修复范围、修复提交和复测结论。
6. 修复后按原步骤复测，并更新 `issue-log.md`。
7. 联调结束前运行可用校验：
   - `./harness/scripts/validate-integration.sh`
   - `./harness/scripts/run-all.sh`

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

