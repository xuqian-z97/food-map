# FoodMap 项目进度台账与看板

## 1. 目的

把"项目进度"从记忆和散文里搬出来。功能点、任务、测试证据、验收结论集中在一个目录内，**人工和 agent 用同一套数据、同一套写入规则**。

核心主张：**已实现 ≠ 已完成**。代码写完只能标 `implemented`，通过测试并有验收单才能标 `accepted`。

## 2. 非目标

- 不做多人协作、账号体系、权限、通知、附件上传。
- 不替代 `docs/integration/` 的联调过程记录和 BUG 台账。
- 不替代 `CODEX-*.md` 的产品、架构、迭代规则。
- 不承载任何业务代码，不 import `after/` 与 `front/` 的实现。

## 3. 红线

违反任一条即视为设计错误，应退回而不是绕过：

1. **单用户、单机**。服务只绑定 `127.0.0.1`，无登录。
2. **无数据库**。唯一存储是仓库内 `status/data/` 的文件，git 即审计日志。
3. **不落敏感信息**。证据只引用仓库内相对路径，不复制 Token、密码、手机号、私密推荐正文。
4. **生成物不可手改**。`status/dist/index.html` 只能由 `status build` 产出。
5. **单一写入层**。人工界面与 agent 命令行必须调用同一个 core，界面不得直接改文件。

## 4. 目录

```text
status/
├── README.md              # 本文件：目的、红线、契约索引、路线图
├── status                 # 入口脚本：./status/status <command>
├── .gitignore
├── config/
│   └── foodmap.json       # 项目适配：路径、模块映射、测试命令、harness（JSON，不参与 YAML 子集）
├── data/                  # 数据，必须留在 FoodMap 仓库
│   ├── README.md          # 数据契约（字段、状态机、示例、漂移检查）
│   ├── features/          # F-*.yml
│   ├── tasks/             # T-*.yml
│   ├── acceptance/        # A-*.md（带 front-matter 的验收单）
│   ├── integration/       # IT-*.yml（联调链路评审 + 可执行场景）
│   └── runs/              # <时间戳>-<范围>.json（测试运行记录，只追加）
├── tool/                  # 工具代码，可整体搬移
│   ├── package.json       # 零依赖声明（engines: node>=20）
│   ├── cli.mjs            # 唯一写入口：list/check/build/feature/task/test
│   └── core/
│       ├── yaml.mjs       # 受限 YAML 子集解析与序列化
│       ├── project.mjs    # 台账加载与派生视图（含 git 事实）
│       ├── validate.mjs   # 门禁与对账（规则 0–8）
│       ├── git.mjs        # 提交查询
│       ├── surefire.mjs   # Maven 测试报告解析
│       └── render.mjs     # 单文件看板渲染
└── dist/
    └── index.html         # 生成物，提交进 git
```

`data/` 与 `tool/` 分两条路径：数据跟项目同寿，工具将来可能独立成仓库。**不要混在一起**，否则拆分要从历史里拆。

## 4.1 快速开始

```sh
./status/status list                 # 当前状态一行摘要 + 功能点清单
./status/status check                # 门禁与对账；0 错误才算干净
./status/status build                # 生成 status/dist/index.html
open status/dist/index.html          # 直接看板
```

## 5. 两种操作模式

| 模式 | 入口 | 场景 |
| --- | --- | --- |
| agent | `./status/status <command> --json` | 每个迭代实现完成、跑测试、关闭任务、写验收单（T0 已可用） |
| 人工 | `./status/status serve` → `127.0.0.1:<port>` | 看板浏览、**待签字队列**、验收结论确认（T1） |

人工界面按档位交付：

- **H1（先做）**：只读看板 + 签字队列。你点"通过 / 有条件通过 / 退回"，落库为验收结论。
- **H2（后置）**：浏览器内自由编辑功能点与任务。只有 agent 不在场时才需要。

## 6. 命令（规划，T0–T2 随实现补齐）

| 命令 | 状态 | 作用 |
| --- | --- | --- |
| `./status/status list [--json]` | T0 已实现 | 全量状态：功能点、任务、未排期、优先级 |
| `./status/status check [--json]` | T0 已实现 | 门禁与对账，退出码非 0 表示不允许提交 |
| `./status/status build [--check]` | T0 已实现 | 生成 `dist/index.html`；`--check` 只比较摘要 |
| `./status/status feature set <F-001> --status=<s> --priority=<P0>` | T0 已实现 | 改状态/优先级，写后自动校验并回滚非法改动 |
| `./status/status task add <F-001> --title=...` | T0 已实现 | 排任务，自动生成 T-xxx 并回写功能点 |
| `./status/status task done <T-001> [--commit=<sha>]` | T0 已实现 | 关闭任务，写入完成时间（缺省取提交时间） |
| `./status/status test import --covers=F-001,F-002` | T0 已实现 | 读取 surefire 报告并写入 `runs/` |
| `./status/status test run --case IT-004` | T2 计划 | 跑联调场景（需本地环境，缺环境记 skipped） |
| `./status/status accept <F-001> --verdict=...` | T2 计划 | 生成验收单草稿并签字 |
| `./status/status serve` | T1 计划 | 本地看板服务与待签字队列 |

## 7. 完成判定（本项目的核心规则）

功能点只有同时满足以下条件才能是 `accepted`：

1. 存在验收单，且 `verdict` ∈ `pass` / `conditional`；
2. 至少一次 `runs/` 记录的 `covers` 包含该功能点；
3. 若关联 `integrationCase` 且 `needChain: true`，其 `review` 必须是 `approved`；
4. `paths` 指向的代码存在提交。

任一不满足时，看板固定显示"待验收"，不允许手工改成已完成。`conditional`（有条件通过）算已完成，但必须列出未关闭项。

## 8. 状态机

```text
todo ──→ planned ──→ in_progress ──→ implemented ──→ testing ──→ accepted
 │           ↑            ↑               │                          │
 └───────────┴────────────┴───────────────┘                          │
                      返工回退                                        │
 任意状态 ──→ cancelled（需 reason，不参与"未完成"统计）  ←───────────┘
```

| 流转 | 前置条件 |
| --- | --- |
| `todo → planned` | 至少关联 1 个任务 |
| `planned → in_progress` | 无 |
| `in_progress → implemented` | 无（代码完成即到达） |
| `implemented → testing` | 验收单草稿存在，含测试说明与用例 |
| `testing → accepted` | 第 7 节四条全部满足 |
| `accepted → testing` | 回归失败返工，必须写原因并保留原验收单 |
| 任意 `→ cancelled` | 必须写 `reason` |

## 9. 优先级口径

- 功能点与任务的 `priority` 取值 **P0 / P1 / P2 / P3**，**P0 最高**，看板按 P0 → P3 排序。
- 与 `docs/integration/*/issue-log.md` 的 BUG 优先级使用同一套口径，**不再需要任何换算**。

## 10. 数据权威归属（避免出现第 4 个真相源）

| 内容 | 唯一权威 |
| --- | --- |
| 功能点状态、任务状态、验收结论、测试证据 | `status/data/` |
| BUG 台账、联调过程、复测结论 | `docs/integration/*/issue-log.md` |
| 产品范围、架构、迭代规则 | `CODEX-*.md`、`AGENTS.md` |

`CODEX-gen.md` 的里程碑表只描述阶段划分，不再逐项维护状态；与 `status/data/` 冲突时以台账为准。

事实层冲突时（台账说完成、代码或测试不支持），**以代码与测试证据为准**，台账必须被修正而不是被解释。

## 11. 技术约束

- Node ≥ 20（当前环境 v24.14.0），**零 npm 依赖**。
- 数据文件使用受限 YAML 子集：`key: value`、`- 列表项`、一层"列表项内标量字段"、`#` 注释、单/双引号。解析器自写，不引入通用 YAML 依赖。
- 配置文件用 JSON（`config/foodmap.json`）：它只有机器读写、几乎不手改，却天然需要嵌套结构，用 YAML 子集会被迫把层级拍平。
- 时间统一 ISO-8601 带时区偏移，例如 `2026-11-05T14:00:00+08:00`。
- 生成物为单文件 HTML：数据内联，可 `file://` 直接打开、可打印、可提交。
- 若后续启用 GitHub Pages，源选 `main / (root)`，看板地址为 `/status/dist/index.html`，产物无需搬到 `docs/`。

## 12. 路线图

| 切片 | 内容 | 出口 |
| --- | --- | --- |
| T0 ✅ | 解析器 + 校验 + CLI + `data/` 种子（从报告矩阵与 3 份计划导入）+ 只读看板 | 已完成 2026-11-05：`status check` 0 错误 / 9 提示；看板 21 个功能点、未排任务高亮、P0→P3 分组 |
| T1 | 本地服务 + 待签字队列（H1） | 能对 `implemented/testing` 的功能点签字，结论写回 `acceptance/` |
| T2 | 测试执行器（surefire / harness / 联调场景） | `status test run` 生成 `runs/` 记录，并驱动"待验收 → 已完成"判定 |
| T3 | 自由编辑（H2）+ 漂移检测 + 接入 harness | `harness/scripts/run-all.sh` 包含 `validate-status.sh` |

## 13. 相关文档

- 数据契约：[`data/README.md`](data/README.md)
- 项目适配：[`config/foodmap.json`](config/foodmap.json)
- 协作与边界：[`../AGENTS.md`](../AGENTS.md) §6.2
- 联调分级与通过标准：[`../docs/integration/README.md`](../docs/integration/README.md)
- 现状基线报告：[`../docs/reports/foodmap-project-status-and-roadmap-2026-11-05.html`](../docs/reports/foodmap-project-status-and-roadmap-2026-11-05.html)
