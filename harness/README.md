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
- 等后端和 iOS 工程生成后，再逐步增强脚本。

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
    ├── validate-ios.sh
    ├── validate-api.sh
    └── validate-git.sh
```

## 4. 推荐使用流程

1. 主代理根据 `agent-task-template.md` 拆分任务。
2. 子代理按 `file-boundary-rules.md` 执行。
3. 接口任务按 `api-contract-checklist.md` 设计和验收。
4. 迭代完成前按 `acceptance-checklist.md` 检查。
5. 执行：

```sh
./harness/scripts/run-all.sh
```

## 5. 当前脚本策略

当前项目还没有后端和 iOS 代码，因此脚本会：

- 强制检查文档、skills、agents、harness 是否存在。
- 强制检查核心规则是否保留。
- 对尚未生成的 `after/` 和 `front/` 给出提示并通过。
- 在相关目录生成后启用更严格检查。

## 6. 核心规则

harness 必须始终保护以下规则：

- 后端采用 Java 微服务。
- 每个后端服务独立数据库。
- 服务之间不能直接访问彼此数据库表。
- 后端强制校验推荐可见范围。
- 只有 `PUBLIC / 全部公开` 推荐进入全站统计。
- 菜名必须是纯文字。
- 重要迭代需要提交并推送 GitHub。

