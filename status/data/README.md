# status/data 数据契约

本目录是项目进度状态的**唯一书写位置**。所有文件都是纯文本，随 git 提交，可直接 review diff。

约定：**一个实体一个文件，文件名即 id**。不设索引文件，避免索引与实体不一致。

| 目录 | 实体 | 文件名 | 格式 |
| --- | --- | --- | --- |
| `features/` | 功能点 | `F-012.yml` | 受限 YAML |
| `tasks/` | 任务 | `T-021.yml` | 受限 YAML |
| `acceptance/` | 验收单 | `A-012.md` | Markdown + front-matter |
| `integration/` | 联调链路 case | `IT-004.yml` | 受限 YAML |
| `runs/` | 测试运行记录 | `2026-11-05T14-00-auth.json` | JSON（只追加） |

## 1. 受限 YAML 子集

自写解析器只支持以下写法，写数据时不得越界：

```yaml
# 注释
key: 值                     # 标量
quoted: "含: 冒号的值"       # 需要时用单/双引号
list:                       # 列表
  - 第一项
  - 第二项
records:                    # 列表项内带标量字段
  - id: S1
    name: 场景名
```

不支持：内联映射 `{}`、多行字符串 `|`、锚点、标签、多文档。需要复杂结构时**拆文件**，不要扩语法。

## 2. 功能点 `features/F-*.yml`

| 字段 | 必填 | 类型 | 说明 |
| --- | --- | --- | --- |
| `schemaVersion` | 是 | 整数 | 当前为 `1` |
| `id` | 是 | 字符串 | `F-` + 三位数字，与文件名一致 |
| `title` | 是 | 字符串 | 一句话，用户/开发者都能看懂 |
| `layer` | 是 | 枚举 | `backend` / `frontend` / `fullstack`，决定看板归入哪一栏 |
| `phase` | 是 | 字符串 | 迭代号，如 `B1`、`B1.5`、`B2` |
| `priority` | 是 | 枚举 | `P0` / `P1` / `P2` / `P3`，**P0 最高**，与 issue-log 口径一致 |
| `status` | 是 | 枚举 | 见 §7 状态机 |
| `summary` | 否 | 字符串 | 可见行为描述，用于卡片摘要 |
| `backendModules` | 否 | 列表 | 后端微服务名，如 `foodmap-auth-service` |
| `frontendModules` | 否 | 列表 | 前端模块路径，如 `Features/Auth`、`Core/Networking` |
| `paths` | 否 | 列表 | glob，用于对账"台账说完成、代码有没有提交" |
| `tasks` | 否 | 列表 | 任务 id |
| `integrationCase` | 否 | 字符串 | 关联 `IT-*` |
| `acceptance` | 否 | 字符串 | 关联 `A-*`，`accepted` 状态必须有 |
| `dependsOn` | 否 | 列表 | 前置功能点 id |
| `cancelReason` | 否 | 字符串 | `cancelled` 时必填 |
| `notes` | 否 | 字符串 | 自由文本 |

示例：

```yaml
schemaVersion: 1
id: F-007
title: 退出登录并撤销双 Token
layer: fullstack
phase: B1
priority: P0
status: testing
summary: 退出后旧 Access Token 与 Refresh Token 均不可再用，重启保持未登录
backendModules:
  - foodmap-auth-service
  - foodmap-gateway-service
frontendModules:
  - Features/Auth
  - Core/Auth
paths:
  - after/foodmap-auth-service/src/**
  - after/foodmap-gateway-service/src/**
  - front/FoodMapApp/FoodMapApp/Features/Auth/**
tasks:
  - T-011
integrationCase: IT-004
acceptance: A-007
dependsOn:
  - F-001
notes: BUG-011 待模拟器复测
```

> `backendModules` / `frontendModules` 是**显式声明**（用于汇总与展示）；`paths` 是**事实范围**（用于 git 对账）。二者不自动推导，允许前者比后者宽。

## 3. 任务 `tasks/T-*.yml`

| 字段 | 必填 | 类型 | 说明 |
| --- | --- | --- | --- |
| `schemaVersion` | 是 | 整数 | `1` |
| `id` | 是 | 字符串 | `T-` + 三位数字 |
| `feature` | 是 | 字符串 | 所属功能点 id |
| `title` | 是 | 字符串 | 具体动作，不写"优化一下"这类模糊描述 |
| `status` | 是 | 枚举 | `todo` / `doing` / `done` / `dropped` |
| `priority` | 否 | 枚举 | `P0`–`P3`，未完成任务按 P0 → P3 排 |
| `owner` | 否 | 枚举 | `agent` / `human` |
| `startedAt` | 否 | 时间 | ISO-8601 带偏移 |
| `doneAt` | 是（`done` 时） | 时间 | 实际完成时间点；缺省时取 `commit` 的提交时间 |
| `commit` | 否 | 字符串 | 短 sha |
| `planRef` | 否 | 字符串 | 来源计划文档相对路径（任务种子来源） |
| `notes` | 否 | 字符串 | |

示例：

```yaml
schemaVersion: 1
id: T-011
feature: F-007
title: iOS logout 同时携带 Access 与 Refresh Token
status: done
priority: P1
owner: agent
startedAt: 2026-06-30T10:00:00+08:00
doneAt: 2026-06-30T15:20:00+08:00
commit: 5994d70
planRef: docs/superpowers/plans/2026-06-22-b1-auth-ios-backend-integration.md
```

## 4. 验收单 `acceptance/A-*.md`

front-matter 字段：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `schemaVersion` | 是 | `1` |
| `id` | 是 | `A-` + 三位数字 |
| `feature` | 是 | 功能点 id |
| `level` | 是 | `L1`–`L4`，口径见 `docs/integration/README.md` |
| `verdict` | 是 | `pending` / `pass` / `conditional` / `fail` / `blocked` |
| `signedBy` | 否 | `human` / `agent`；`agent` 签署时正文必须写明依据 |
| `signedAt` | 否 | ISO-8601 带偏移 |
| `build` | 否 | 提交号或构建号 |
| `testRuns` | 否 | 关联 `runs/` 的 id 列表 |
| `evidence` | 否 | 仓库内相对路径（截图、日志摘要、联调目录） |
| `openIssues` | 否 | 未关闭的 BUG 编号；`conditional` 时必填 |

正文固定五节，缺节视为未完成草稿：

```markdown
---
schemaVersion: 1
id: A-007
feature: F-007
level: L2
verdict: conditional
signedBy: human
signedAt: 2026-11-05T18:00:00+08:00
build: cb1145e
testRuns:
  - 2026-11-05T14-00-auth
evidence:
  - docs/integration/B1-auth-ios-backend/screenshots/logout-01.png
openIssues:
  - BUG-007
---
# A-007 退出登录与双 Token 失效

## 1 测试说明

范围、环境、账号、数据准备、已知限制。

## 2 用例设计

| 编号 | 场景 | 步骤 | 预期 |
| --- | --- | --- | --- |
| C1 | 有效 Token 退出 | 点退出 → 查后端 | 请求到达，返回 200 |
| C2 | 旧 Access 再用 | 带旧 Token 调 /api/users/me | 401 |
| C3 | 旧 Refresh 再用 | 调 /refresh | 401 |
| C4 | 退出后重启 | 杀进程重开 | 未登录 |

## 3 执行结果

逐条记录实际结果、时间、执行人；失败写现象与 requestId/traceId。

## 4 未覆盖项与风险

明确写出没测什么，以及为什么。

## 5 结论

判定 + 依据 + 未关闭项。
```

## 5. 联调链路 case `integration/IT-*.yml`

用于回答"这个功能是否需要打通链路、是否需要单独评审"，并给出可执行场景。

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `id` | 是 | `IT-` + 三位数字 |
| `title` | 是 | |
| `features` | 是 | 涉及功能点 |
| `services` | 否 | 涉及服务/模块 |
| `needChain` | 是 | `true` 表示必须真实端到端，不能只测单服务 |
| `review` | 是 | `pending` / `approved` / `rejected` |
| `reviewNote` | 否 | 评审结论与理由 |
| `scenarios` | 否 | 列表，每项含 `id` / `name` / `method` / `path` / `auth` / `expectStatus` / `expectCode` |

`auth` 取值：`none` / `accessToken` / `expiredAccessToken` / `refreshToken` / `denylistedAccessToken`。凭据由运行器从本地登录获取，**不写入文件**。

示例：

```yaml
schemaVersion: 1
id: IT-004
title: 登录 → 网关 → 当前用户
features:
  - F-001
  - F-002
services:
  - gateway
  - auth
  - user
needChain: true
review: approved
reviewNote: 必须经真实网关路由，服务直连不算通过
scenarios:
  - id: S1
    name: 有效 Token 查询当前用户
    method: GET
    path: /api/users/me
    auth: accessToken
    expectStatus: 200
    expectCode: OK
  - id: S2
    name: 退出后旧 Token 失效
    method: GET
    path: /api/users/me
    auth: denylistedAccessToken
    expectStatus: 401
    expectCode: UNAUTHORIZED
```

## 6. 测试运行记录 `runs/*.json`

只追加，不修改历史。文件名 `<时间戳>-<范围>.json`（时间戳 `YYYY-MM-DDTHH-MM`）。

```json
{
  "schemaVersion": 1,
  "id": "2026-11-05T14-00-auth",
  "at": "2026-11-05T14:00:12+08:00",
  "kind": "unit",
  "command": "cd after && mvn -pl foodmap-auth-service -am test",
  "env": "local",
  "result": { "total": 10, "passed": 10, "failed": 0, "skipped": 0 },
  "perModule": { "foodmap-auth-service": 10 },
  "covers": ["F-007"],
  "evidence": ["after/foodmap-auth-service/target/surefire-reports"]
}
```

`kind` 取值：`unit`（surefire）、`harness`（`harness/scripts/*`）、`integration`（`IT-*` 场景）、`manual`（人工证据，必须写明执行人）。

**环境缺失时记 `kind: integration` + `result.skipped` 与 `reason`，不要记 `failed`**——否则看板会变成一片红灯，逼人靠记忆解释。

## 7. 功能点状态机

```text
todo ──→ planned ──→ in_progress ──→ implemented ──→ testing ──→ accepted
```

| 流转 | 前置条件 |
| --- | --- |
| `todo → planned` | `tasks` 非空 |
| `planned → in_progress` | 无 |
| `in_progress → implemented` | 无 |
| `implemented → testing` | `acceptance` 指向的验收单存在，且含 §1/§2 两节 |
| `testing → accepted` | 见 §8 四条 |
| `accepted → testing` | 必须写 `notes` 说明返工原因，保留原验收单 |
| 任意 `→ cancelled` | 必须写 `cancelReason` |

## 8. `accepted` 的四条硬门禁

1. 验收单 `verdict` ∈ `pass` / `conditional`；
2. 至少一次 `runs/` 记录的 `covers` 包含该功能点；
3. 若 `integrationCase` 且 `needChain: true`，则其 `review` 为 `approved`；
4. `paths` 指向的代码有提交。

违反任一条时 `status check` 失败，看板显示"待验收"。

## 9. 对账与漂移检查

`status check` 执行以下检查，前 5 条为错误（阻断提交），其余为警告：

| # | 检查 | 级别 |
| --- | --- | --- |
| 0 | 文件可解析、`id` 与文件名一致、`schemaVersion` 已知 | 错误 |
| 1 | 引用完整性：`tasks` / `acceptance` / `integrationCase` / `dependsOn` 均指向存在实体 | 错误 |
| 2 | 状态机合法性（含 `accepted` 四条门禁） | 错误 |
| 3 | `accepted` 的功能点必须有验收单 | 错误 |
| 4 | `dist/index.html` 与 `data/` 不同步 | 错误 |
| 5 | 状态 ∈ `implemented/testing/accepted` 但 `paths` 近 30 天无提交 | 警告 |
| 6 | 未完成功能点但 `tasks` 为空 → 界面标"未排期" | 警告 |
| 7 | 任务 `done` 但缺 `doneAt` 且无 `commit` | 警告 |
| 8 | `conditional` 验收单 `openIssues` 为空 | 警告 |

## 10. 写入规则

- 只能通过 `status` 命令或人工界面写入；不鼓励手改文件，手改后必须跑 `status check`。
- 一个实体一个文件；**不要**为了避免多个文件而把多个功能点塞进一个 yml。
- 提交时与 `dist/index.html` 同批提交，保证看板与数据同步。
