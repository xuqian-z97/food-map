# FoodMap Harness

## 1. 目的

`harness/` 是 FoodMap 多代理开发模式下的约束和验收脚手架。

它用于帮助主代理和子代理在同一套规则下工作，避免：

- 子代理修改不该修改的文件。
- 文档和代码漂移。
- 前后端 API / DTO 不一致。
- 权限、隐私、可见范围规则遗漏。
- PUBLIC 统计口径错误。
- 迭代完成但未统一验收。

## 2. 使用原则

当前阶段采用轻量 harness：

- 先用文档和清单约束开发。
- 再用脚本做基础自动检查。
- 后端和 iOS 工程已经生成部分代码，脚本会按当前落地范围逐步增强。
- 对已落地模块执行更具体检查，对尚未形成稳定构建命令的模块保留提示型检查。

## 3. 文件说明

```text
harness
├── README.md
├── agent-task-template.md
├── acceptance-checklist.md
├── file-boundary-rules.md
├── api-contract-checklist.md
└── scripts
    ├── run-all.sh
    ├── validate-docs.sh
    ├── validate-backend.sh
    ├── validate-integration.sh
    ├── validate-ios.sh
    ├── validate-api.sh
    └── validate-git.sh
```

## 4. 推荐使用流程

1. 主代理根据 `agent-task-template.md` 拆分任务。
2. 子代理按 `file-boundary-rules.md` 执行。
3. 接口任务按 `api-contract-checklist.md` 设计和验收。
4. 前后端联调前，在 `docs/integration/<iteration>-<feature>/` 下生成 `integration-plan.md` 和 `issue-log.md`。
5. 联调完成前按 `integration-plan.md`、`issue-log.md` 和 `acceptance-checklist.md` 检查。
6. 执行：

```sh
./harness/scripts/run-all.sh
```

## 5. 当前脚本策略

当前项目已经生成部分后端和 iOS 代码，因此脚本会：

- 强制检查文档、skills、agents、harness 是否存在。
- 强制检查核心规则是否保留。
- 对 `after/` 后端工程执行基础 Maven、模块、日志平台和联调文档检查。
- 对 `front/FoodMapApp` iOS 工程执行轻量结构检查；项目 scheme、签名和可重复构建命令稳定后，再增强为真实 iOS build/test。
- 对 `docs/integration/` 执行联调说明、问题记录、联调安全点和 `requestId/traceId` 基线检查。
- 对尚未实现的业务模块保持轻量检查，避免把未开始的功能误判为失败。

## 6. 核心规则

harness 必须始终保护以下规则：

- 后端采用 Java 微服务。
- 每个后端服务独立数据库。
- 服务之间不能直接访问彼此数据库表。
- 后端强制校验推荐可见范围。
- 只有 `PUBLIC / 全部公开` 推荐进入全站统计。
- 菜名必须是纯文字。
- 重要迭代需要提交并推送 GitHub。
