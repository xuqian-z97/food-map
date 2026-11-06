# verify 工具可用性报告（2026-11-06）

结论：**有条件可用**。接口、XCUITest、目录套件、语法校验和失败后执行 teardown 均可用。现阶段适合隔离本地环境中的文本模式验证；将 `--json` 接入 agent、CI 日志或结果归档前，必须修复 **P0-01：stdout 未脱敏 Token**。场景清理不完整和历史证据污染也需要改进。

本轮只评估工具，不重新裁定业务验收。`F-001` / `F-003` 的既有结论仍以 `status/data/features/` 和验收单 `A-003` / `A-002` 为准，未修改。评分采用 1～5 分，5 分最好。

## 1. 环境与边界

- 日期、时间采用 Asia/Shanghai；仓库基线 `101eb09`，分支 `main`。
- 已读 `AGENTS.md` §6.3、`tools/verify/AGENT.md`、`harness/scenarios/README.md`、工具 README，以及相关 CODEX、QA skill、harness 和验收单。
- Node `v24.14.0`；Xcode `27.2 (27B52)`；iOS 26.5 的 iPhone 17 Pro 模拟器已启动，UDID 与现有场景一致。
- `docker ps` 确认 `foodmap-postgres`、`foodmap-redis`、`foodmap-nacos` 正在运行；前两者 healthy。网关 `http://127.0.0.1:8080/internal/auth/health` 返回 **200**。
- 首次沙箱内检查出现 Docker socket `operation not permitted`、HTTP `000` 和 CoreSimulator 连接错误。经权限审批在沙箱外重试后正常，未重启服务。这是运行权限问题，不能据此判断服务未启动。
- 按要求将测试密码通过 `FM_TEST_PW` 注入进程环境。未新增含密码的配置文件。
- 禁改范围 `tools/verify/core`、`tools/verify/adapters`、业务代码和 `status/data/` 均未修改。唯一临时仓库改动为 UI 场景的一处断言，已用 `git checkout -- harness/scenarios/b1-ui-login.yml` 恢复。
- 恢复前后 SHA-256 一致：`a4ffa94c4e398496c2ff7c1f2dc9c5e53600c5285e54baac56f4df4161bdc513`。
- 实际测试全部由 `tools/verify/cli.mjs` 执行；curl 只用于用户指定的前置健康检查。辅助程序只分析输出、生成临时场景和脱敏，没有替代工具发起业务验证。

## 2. 六项实跑结果

以下命令从仓库根目录运行，密码已通过环境变量设置。耗时为 wall time；工具内部 duration 单独列出。

| 顺序 | 命令/动作 | 退出码 | 实际结果 | 耗时 |
| --- | --- | --- | --- | --- |
| 1 | `node tools/verify/cli.mjs run harness/scenarios/ --list` | 0 | 列出 5 个 YAML，与目录实际场景一致 | 0.04 秒 |
| 2 | `node tools/verify/cli.mjs run harness/scenarios/ --dry-run` | 0 | 5 个场景通过；打印 setup / steps / teardown 数量；工作区未变化 | 0.05 秒 |
| 3 | `node tools/verify/cli.mjs run harness/scenarios/b1-logout.yml --evidence-dir /tmp/verify-evidence` | 0 | 12/12 通过 | 0.56 秒；内部 520 ms |
| 4 | `node tools/verify/cli.mjs run harness/scenarios/b1-ui-login.yml --evidence-dir /tmp/verify-evidence` | 0 | 3 setup + 4 step + 5 teardown 全通过；XCUITest 真实执行 1 个用例 | 19.25 秒；内部 19,201 ms |
| 5 | `node tools/verify/cli.mjs run harness/scenarios/ --json --evidence-dir /tmp/verify-evidence` | 0 | 5/5 场景通过，聚合 JSON 可解析，进度输出与 stdout 分离 | 32.89 秒；内部 32,831 ms |
| 6 | 把 UI 场景的 `expect: { equals: 0 }` 临时改为 `expect: { equals: 1 }`，运行第 4 条命令 | **1** | 11/12 通过；仅故意改坏的断言失败；其余步骤及 5 个 teardown 仍执行 | 15.46 秒；内部 15,410 ms |

第 5 项真实 summary：

```json
{"scenarios":5,"passed":5,"failed":0,"steps":65}
```

这里的 65 包含 setup 和 teardown，不是 65 个独立验收点。

恢复断言后，再次运行第 5 项：退出码 **0**，summary 完全相同，wall time **33.85 秒**，内部 **33,788 ms**。第二次 stdout 仅在内存中检查，脱敏后才保存。

另用 `/tmp/verify-usability/invalid-assert.yml` 写入未知断言 `typoAssertion`，执行 `--dry-run`：退出码 **2**，0.03 秒，提示如下。正、负两种语法校验均得到验证，未修改仓库场景。

```text
错误: steps[0].expect 包含未知断言 typoAssertion
提示: 支持：status, equals, contains, matches, exitCode, stdoutContains, gte, lte, json
```

辅助清理核查场景最初误写成 JSON 内容、扩展名为 `.yml`，工具返回 2 并明确提示“第 1 行不是 key: value”和受限 YAML 要求；改为文档约定的 YAML 后运行正常。这是使用错误，不计为工具缺陷。

## 3. 逐维度评分

| 维度 | 评分 | 观察与回答 |
| --- | --- | --- |
| 上手成本 | 4/5 | 不需要 npm 安装或构建工具本身。首次前置检查至接口场景通过约 1 分钟；文档阅读未单独计时，整体上手估算约 2 分钟，前提是服务和模拟器已准备好。唯一环境卡点是沙箱权限，AGENT.md 已提示。README 漏列 2 个现有场景。 |
| 命令是否直观 | 4/5 | `run`、目录套件、`--list`、`--dry-run`、`--json`、`--evidence-dir` 含义清楚，首次跑通不需要读源码。默认 evidence 目录明确。固定模拟器、固定已有账号和 `/tmp/verify-auth.log` 等场景依赖尚未完整文档化；仅导出 `FM_TEST_PW` 不会覆盖旧场景内的固定密码。 |
| 错误可读性 | 4/5 | 故意失败直接给出动作和 `expected 1 / actual 0`，结合 AGENT.md 中 equals 表示失败用例数的说明，可不看实现定位为期望写错。错误分类 1 / 2 正确。该失败 JSON 的 error、hint 都为空，缺少断言字段的业务含义和 YAML 行号。 |
| 证据是否够用 | 2/5 | 场景 JSON 有 checks、phase、请求/链路 ID、HTTP 状态、XCUITest 用例结果和 xcresult 路径，足够追查本次问题。文本证据简洁，但缺 phase 且长命令/actual 被截断。工具生成的本轮 32 个 `.txt` / `.json` 未发现测试密码、JWT、完整手机号；**stdout JSON 却稳定泄露 Token**，见 P0-01。 |
| 覆盖能力 | 3/5 | 覆盖注册与账号名登录正常路径、真实 UI 登录进入地图、成功登录落库、登出后双 Token 401、REVOKED 落库、Redis denylist 增量和身份双写一致性。不能完整复现两份验收单，缺少错误登录、重复/短密码、重启会话清理、边界鉴权和刷新成功等场景，见第 6 节。 |
| 稳定性 | 3/5 | 两次全套都是 5/5，API 场景共 3 次通过，正常 UI 共 3 次通过，未观察到随机失败；仅两次套件不足以证明长期稳定。固定账号、累计探针日志、全局日志匹配和未清理数据可能掩盖失败或产生环境依赖。 |
| 耗时 | 5/5 | API 单场景小于 1 秒，UI 约 16～19 秒，全套约 33 秒，适合本地反馈。全套约 12.8 秒消耗在探针场景的固定 12 秒等待；未测试冷缓存构建、CI 或其他机器，不外推其耗时。 |

合计 **25/35**。结论以 P0 与使用条件为准，不能用平均分抵消敏感信息问题。

## 4. 故意失败：诊断与 teardown

注入位置为 UI 场景 `steps[0].expect.equals`，将“失败用例数应为 0”改为“应为 1”。XCUITest 自身仍是 **1 passed / 0 failed**，工具正确判定断言失败：

```text
[1/4] xcode test (FoodMapApp) … FAIL
      equals: expected 1 / actual 0
```

退出码应为 **1**：场景语法合法、适配器已运行，只是断言不符。实际为 1，符合契约。临时未知断言返回 2，验证了配置错误与运行失败的区别。

从这段输出即可判断是哪一步、哪个断言和哪个数值不符，不需要读核心或适配器实现；解释 `0` 代表失败用例数需要 AGENT.md 的动作/断言速查。JSON 中保存了 `phase=step`、`index=1`、`action=xcode`、`attempts=1` 及同一组 checks。

失败后的其余 3 个业务步骤和 **5 个 teardown 全部继续执行**，证明工具调度层的清理执行能力可用。但“命令执行成功”不等于“所有测试数据已清除”。

使用验证工具运行临时只读场景 `/tmp/verify-usability/cleanup-observation.yml`，对这次失败运行再次查库，结果为 5/9 通过、4/9 失败，退出码 1：

| 核查项 | 清理后数量 | 口径 |
| --- | --- | --- |
| `auth_accounts` | 0 | 注册返回的 `userId=200047` |
| `auth_identity_bindings` | 0 | 同一 userId |
| `auth_sessions` | 0 | 同一 userId |
| `refresh_tokens` | 0 | 本次场景运行时间窗 |
| `login_logs` | 0 | 本次场景运行时间窗 |
| 孤立 `auth_credentials` | **1** | 本次时间窗内创建、已无对应 auth_accounts 的凭证；基于隔离时间窗归因 |
| `foodmap_user_db.users` | **1** | 同一 userId |
| `foodmap_user_db.user_profiles` | **1** | 同一 userId |
| `foodmap_user_db.user_settings` | **1** | 同一 userId |

本次失败时间窗为 `2026-11-06T06:08:10.793Z` ～ `2026-11-06T06:08:26.203Z`。核查只读取数量，不读取密码哈希、Token 或个人资料正文。现有 teardown 没有删除凭证和用户库记录，且所有删除命令都带 `>/dev/null 2>&1 || true`，因此不能证明 SQL 错误会被报告。未为掩盖这一缺口修改清理逻辑或业务实现；测试数据残留已明确记录。

## 5. 必须修的 P0 问题

### P0-01：`--json` stdout 与落盘证据的脱敏行为不一致

**实际结果：** 两次全套运行都在 stdout JSON 中检出 **12 处未脱敏的 Token 字段**、**20 处 JWT 字符串出现**。后者包含重复序列化的副本，不代表 20 个独立 Token。典型路径：

```text
scenarios[1].steps[11].actual.json.data.accessToken
scenarios[1].steps[11].actual.json.data.refreshToken
scenarios[1].steps[11].actual.value.data.refreshToken
scenarios[1].steps[11].checks[1].actual.data.refreshToken
scenarios[3].steps[2].actual.json.data.refreshToken
```

同次运行由工具生成的场景/套件 evidence JSON 中，对应 Token 已是 `[REDACTED]`。stdout 是合法 JSON，但内容不适合直接保存或写入 CI 日志。

**复现步骤：**

1. 服务就绪、环境变量已设置后，运行第 2 节第 5 条原始命令。
2. 只在内存中解析 stdout，检查上述 `accessToken` / `refreshToken` 字段是否为 `[REDACTED]`；当前可观察到未脱敏字符串。不要为复现而保存原始输出。
3. 对比 `/tmp/verify-evidence/` 中同次生成的场景 JSON：其对应字段已脱敏。
4. 再运行一次，现象相同。本轮第二次在内存中复现了相同的 12 / 20 计数。

**影响：** README 第 6、7 节建议重定向 stdout 并归档 JSON；照做会把 Token 写入文件。第一轮遵循该用法重定向时确实发生了临时落盘，发现后已立即原地脱敏，未打印原值、未提交。当前保留的 `/tmp/verify-suite.json` 是脱敏副本，不能作为“原始 stdout 已安全”的证据。

**修复验收建议：** stdout、场景证据、套件证据及 checks/嵌套字符串使用一致脱敏策略；补单场景和目录 `--json` 的敏感字段回归。此为后续修复建议，本轮没有改 CLI、core 或 adapters。未实跑单场景 `--json` 泄露，不能把它记为已复现。

## 6. 建议改进项（均附复现步骤）

| 编号 / 优先级 | 问题与建议 | 复现步骤 |
| --- | --- | --- |
| I-01 / P1 | **清理不完整且吞错。** 场景应清理本轮凭证和注册产生的用户记录，按测试数据归属限定范围；清理步骤保留错误并增加删除后计数断言。API、双写场景也应有 teardown。 | 按第 4 节改坏 UI 断言并运行；从 setup 注册响应取得 userId；用临时核查场景查询上表。可见 5 个 teardown 显示 ok，但仍有 4 项残留。只读打开原场景还可看到 `|| true`，以及 API/双写场景没有 teardown。 |
| I-02 / P1 | **日志证据未限定本轮。** UI 日志断言应匹配本次 requestId/traceId、账号或起始偏移；探针日志应清空或按 runId 分段，并对数据库使用本轮增量。 | 连跑两次目录套件，查看 `b1-ios-logout` 的 readlog 和 revoked 查询：本轮分别读到 4,817 / 5,250 字节，REVOKED 数为 8 / 9，断言仍只要求 `>=1`。查看 UI 最后一个 command，它只 grep 整个 `/tmp/verify-auth.log` 的历史 200。当前证据可证明历史数据累计，不能单独证明每次断言只匹配本轮。 |
| I-03 / P1 | **探针场景不自包含。** 应在 setup 创建带 runId 的账号并在 teardown 清理，避免依赖某个历史账号；增加应用构建/安装前提说明。 | 对比 `--list` 与场景文件：目录先运行 `b1-ios-logout.yml`；该文件使用固定 `zxq_ios_152532`，没有注册 setup。现有其余场景不会创建它。复制场景到新环境并 dry-run 仍可通过，但实际运行需先准备该账号和 Debug App。本轮没有删除既有账号来破坏性复现。 |
| I-04 / P1 | **验收点尚未场景化。** 增补下表缺口，保持工具评估与业务验收分开。 | 将 `status/data/acceptance/A-003.md` 的 C1～C6、`A-002.md` 的 C1～C5 与现有 5 个 YAML 逐项对照；运行目录套件，结果中找不到错误密码、重复注册、短密码、登出重启、未认证/内部边界或刷新成功的相应断言。 |
| I-05 / P2 | **文档漂移。** README 补齐场景、依赖、当前重试和 XCUITest 支持；统一证据命名/脱敏说明。 | 运行 `--list` 得到 5 个场景，而 `harness/scenarios/README.md` 只列 3 个；工具 README 开头写支持 retry/XCUITest，后面的 §8～9 仍称只有探针、没有重试；同一文档描述同分钟覆盖，但本轮同分钟的两个 UI 场景 JSON 带不同 runId 并同时保留。 |
| I-06 / P2 | **环境参数和密码入口不统一。** 旧场景改用环境变量；文档说明 UDID、既有 App、日志文件位置及服务日志如何输出到该文件。 | 打开 `b1-logout.yml`、`b1-ios-logout.yml`、`b1-identity-dual-write.yml`，可见固定密码 vars；仅 UI 场景读取 `FM_TEST_PW`。查看 UI YAML，可见固定 UDID 和 `/tmp/verify-auth.log`，setup 只检查健康状态，没有检查该日志文件。本轮没有增加任何密码文件。 |
| I-07 / P2 | **文本证据缺少 phase，长内容截断。** `.txt` 保留 setup/step/teardown 标签、来源行号和完整命令引用；数值断言增加语义提示。 | 运行第 6 项并打开 `*da93db08*.txt`：表格先后出现三组从 1 开始的编号，没有 phase 标签；长 SQL 及 actual 被截断。对比同名 JSON，可找到 phase 和完整 actual；失败的 hint 为空。 |

验收覆盖对照如下；“缺失”指现有场景没有相应断言，不表示业务未实现或既有验收失败。

| 功能 | 当前场景覆盖 | 缺失/不足 |
| --- | --- | --- |
| F-001 / A-003 C1～C3 | 经网关注册与账号名登录、登录成功 UI 到地图、登录成功落库；探针有 `login.ok` | 注册 userId 与登录后 userId 的相等断言缺失；UI 注册流程未覆盖；手机号/邮箱登录入口未形成场景 |
| F-001 / A-003 C4～C6 | 无对应负向场景 | 错误密码 401 及提示、重复手机号 409、短密码前端拦截；Keychain 也没有独立自动断言 |
| F-003 / A-002 C1～C3 | 接口登出、旧 Access 401、旧 Refresh 401、REVOKED 和 denylist 增量；iOS 探针触发登出 | iOS 请求双 Token 的证据未在此场景中直接断言；探针日志/历史数据未限定本轮 |
| F-003 / A-002 C4～C5 | 有运行时 `sessionNil=true` | 缺少登出后重启再查 `hasSession=false`；无 Token 的 401、非健康内部接口 403 未形成场景 |
| F-003 刷新能力 | 只验证撤销后的 Refresh Token 被拒绝 | 缺少有效 Refresh Token 刷新成功与过期等边界；401 自动刷新已拆到 F-022，不计为本轮 F-003 的业务缺陷 |

## 7. 证据索引与收尾

证据在本机 `/tmp/verify-evidence/`，未提交仓库，属于临时文件。文件名均以 `2026-11-06T` 开头；下列短 ID 可定位同名 `.txt` / `.json`：

| 运行 | 时间（北京时间） / 证据标识 |
| --- | --- |
| 接口单场景 | 14:06 / `4051fa91` |
| UI 单场景 | 14:06 / `80dbaa37` |
| 首次套件 | 14:07 / `suite-harness_scenarios`；五场景 `6405a67b`、`8b09543a`、`1655ceb0`、`227e91cd`、`f39cb8e7` |
| 故意失败 | 14:08 / `da93db08` |
| 失败后只读清理核查 | 14:10 / `c30ddc55` |
| 恢复后套件 | 14:11 / `suite-harness_scenarios`；五场景 `aa5ad02e`、`dfc49dec`、`c1aef0e7`、`f20dbc98`、`036972b3` |

- 本轮共保留 **32 个工具生成的文本/JSON 证据文件**。对它们及两份脱敏 stdout 副本扫描，没有发现测试密码、JWT 或完整手机号；扫描范围不包含已有历史证据和 xcresult 二进制内部，因此不宣称所有构建产物均已完成敏感信息审计。
- JSON 中保留 HTTP `x-request-id` / `x-trace-id` / `x-span-id`、XCUITest 名称/结果及 xcresult 本地路径；`.txt` 用于速览，复杂问题需要同名 JSON。
- 临时辅助场景位于 `/tmp/verify-usability/`；其中只包含错误断言、数据库计数和非敏感运行标识。
- 场景文件已恢复；未停服务，未更改业务代码、工具实现、验收结论、台账或看板。既有其他工作区报告不属于本轮交付。
- `./status/status check`：退出码 0，**0 错误 / 6 提示**，均为现有未排期或提交时间提示；`./harness/scripts/validate-docs.sh` 通过。
- 未执行 `run-all.sh` 的业务构建，也未调用 `verify-record.sh`、更新 `status/data/` 或重建看板：本轮是工具可用性评估，业务并无实现变更，且用户要求保留既有验收结论。

当前使用条件：本地文本模式可继续用于人工核查；在修复 P0-01 前，不把原始 `--json` stdout 直接写入日志、文件或上传。完整清理和本轮证据隔离补齐后，才适合把这些现有场景作为可移植、可重复的自动验收套件。
