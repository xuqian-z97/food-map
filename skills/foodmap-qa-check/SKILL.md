---
name: foodmap-qa-check
description: Use when validating FoodMap iterations, reviewing acceptance criteria, running builds or tests, checking documentation-code consistency, privacy rules, visibility rules, community statistics, Git status, commits, or GitHub synchronization.
---

# FoodMap QA 验收 Skill

## 使用时机

当任务涉及验收、测试、构建、检查或发布前确认时使用本 skill。

## 必读文档

- `CODEX-product.md`
- `CODEX-front.md`
- `CODEX-after.md`
- `CODEX-gen.md`
- `AGENTS.md`
- `.agents/qa-agent.md`
- `harness/acceptance-checklist.md`
- `harness/scripts/run-all.sh`

## 核心检查项

产品：

- 菜名是否必填且为纯文字。
- 可见范围是否清晰。
- 只有全部公开内容进入全站统计。

前端：

- 登录后是否进入地图。
- 页面是否有加载、空、错误、无权限、网络不可用状态。
- Token 是否存储在 Keychain。
- 私密内容是否避免日志输出。

后端：

- 服务是否独立数据库。
- 是否存在跨服务直接查表。
- 写接口是否从 Token 获取用户身份。
- 权限、归属、可见范围是否后端校验。
- PUBLIC 统计口径是否正确。

联调：

- 地图能查询当前视野门店。
- 能搜索或创建门店。
- 能创建推荐菜单。
- 好友/情侣只看到授权内容。
- 公开社区只展示 PUBLIC 推荐。

Git：

- 工作区状态清楚。
- 重要迭代已提交。
- 提交后推送到 `origin/main`。

Harness：

- `harness/` 文件结构是否完整。
- 重要迭代是否执行 `harness/scripts/run-all.sh`。
- 未执行的 harness 检查是否说明原因。

## 工作流程

1. 读取相关文档和当前任务范围。
2. 检查文档和代码是否一致。
3. 执行可用的构建或测试命令。
4. 执行可用的 harness 检查。
5. 检查权限、隐私、统计口径风险。
6. 输出通过项、失败项、未执行项和原因。

## 验收标准

- 明确说明执行了哪些命令。
- 明确说明哪些检查未执行及原因。
- 权限、隐私、PUBLIC 统计问题必须高亮。
- 不把“未测试”描述为“已通过”。
