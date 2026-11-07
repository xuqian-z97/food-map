# verify 工具修复复测（2026-11-07）

**有条件可用。** 上轮 FoodMap 登录 JWT 经 `--json` stdout 泄露的问题未再复现。UI 场景正常结束及断言失败后，数据库清理均通过了 9 项独立核查。通用脱敏仍有两个阻塞归档的路径，分别是不透明 Token 和密码字段进入 JSON，以及失败断言把原始响应打印到 stderr。当前环境的全套结果为 3/5 通过。

复测基线为 `e4bcaf1`，包含 stdout 脱敏修复 `c7b1996` 和 UI 清理修复 `e4bcaf1`。环境为 Node 24.14.0、运行中的 PostgreSQL / Redis / Nacos、返回 200 的认证健康检查和已启动的 iPhone 17 Pro（iOS 26.5）。对照 [11 月 6 日可用性报告](verify-tool-usability-2026-11-06.md)。

## 实跑结果

| 项目 | 退出码 | 结果 | 墙钟耗时 |
| --- | --- | --- | --- |
| 1. 目录 `--list` | 0 | 列出 5 个 YAML | 0.10 秒 |
| 2. 目录 `--dry-run` | 0 | 5 个通过，UI 场景为 3 setup、5 step、8 teardown | 0.04 秒 |
| 3. `b1-logout.yml --json` | 0 | 12/12 通过，stdout 的 JWT、测试密码、未遮盖敏感字段均为 0 | 0.65 秒 |
| 4. `b1-ui-login.yml --json` | 0 | 16/16 通过，XCUITest 实际执行 1 个并通过 | 28.88 秒 |
| 5. 目录 `--json` | 1 | 3/5 通过，stdout 的 JWT、测试密码、未遮盖敏感字段均为 0 | 33.64 秒 |
| 6. UI 故意失败 | 1 | 15/16 通过，只有目标断言失败，8 个 teardown 均通过 | 16.13 秒 |

全套 stdout 返回以下 `summary`。

```json
{"scenarios":5,"passed":3,"failed":2,"steps":69}
```

工具单元测试 **27/27 通过**，包含新增的 JWT stdout 回归测试。把临时场景的断言写为 `typoAssertion`，`--dry-run` 返回 **2**，并指出 `steps[0].expect` 中的未知断言。

故意失败使用临时 UI 场景副本。将 XCUITest 的期望失败数从 0 改为 1，绝对路径指向原 iOS 工程。XCUITest 仍是 1 个用例通过，工具输出 `equals: expected 1 / actual 0`。该消息足以定位断言和数值，退出码 1 符合契约。

## UI 清理和套件失败

正常 UI 运行的注册用户 ID 为 `200066`，故意失败运行为 `200070`。两次均用独立只读 verify 场景查询。按 userId 检查账号、绑定、会话、用户、资料和设置，并在各次运行时间窗核对孤立凭证、Refresh Token 和登录日志。**两次各 9 项计数均为 0**。上轮检出的 1 条孤立凭证及 3 条用户库记录残留未再出现。

| 套件场景 | 实际失败原因 |
| --- | --- |
| `b1-identity-consistency.yml` | 预期绑定 19 条，实测 83 条，另有 64 条孤立绑定和 43 条孤立会话。只读复查确认这 107 条记录的创建时间均早于本轮复测。 |
| `b1-ios-logout.yml` | 固定测试账号在认证库中的计数为 0，本轮探针日志记录登录返回 401，REVOKED 计数为 0。历史日志仍含 `login.ok` 和登出标记，因此日志步骤通过，数据库断言发现了失败。 |

其余三个场景为身份双写、接口退出和真实 UI 登录，均通过。复现两项套件失败可直接运行对应 YAML，再按证据中的计数与日志时间复核。

## 脱敏边界

真实 API 与全套的 stdout/stderr 在内存中检查。上轮全套 stdout 检出的 12 处未脱敏 Token 字段、20 处 JWT 字符串出现，本轮分别降为 **0 和 0**。本轮工具生成的 24 个真实场景、套件及只读核查的 `.txt` / `.json` 文件也未匹配测试密码或 JWT。该扫描限于文本与 JSON。

补测用本机 HTTP fixture 在内存中构造响应，包含无 JWT 格式的 `accessToken`、形似 JWT 的 `refreshToken`、`password` 和由 `config.redact` 匹配的 `note`。全部为合成标记，临时证据已清理。

| 测试 | stdout | stderr | 证据 JSON |
| --- | --- | --- | --- |
| 单场景通过 | 不透明 Token、密码原文 | 四种标记均未出现 | 场景证据保留不透明 Token、密码原文 |
| 目录套件通过 | 不透明 Token、密码原文 | 四种标记均未出现 | 套件证据额外保留了场景自定义规则匹配的 `note` |
| 单场景 JSON 断言失败 | 不透明 Token、密码原文 | **四种标记均以原值出现，包括 JWT** | 场景证据保留不透明 Token、密码原文 |

**P0-R1．结构化对象中的不透明 Token 和密码原文仍会输出。** 让本机 fixture 返回 HTTP 200 和 `{ code: "OK", data: { accessToken, refreshToken, password, note } }`。令 accessToken 和 password 为不同的普通合成标记，令 refreshToken 为合成 JWT，再用 verify 执行带 `expect.json: { code: OK }` 的 HTTP 场景并加 `--json`。在 `steps[0].actual.json.data` 可见前两种普通标记，JWT 已遮盖。把场景放入目录运行可复现同样的泄露。在同一场景配置 `config.redact: ["fixture-custom-[A-Z]+"]`，让 note 匹配该规则，即可对比套件 stdout 与套件证据，后者仍保留自定义标记。

**P0-R2．JSON 断言失败时 stderr 可输出原始 JWT。** 保持上述响应，把 `expect.json` 的 `code` 改成不匹配的值，运行同一条 `--json` 命令。CLI 返回 1，stderr 的 `json: expected … / actual …` 包含合成 JWT、普通 Token、密码和自定义标记。应把这条失败分支纳入归档前的脱敏回归。

## 尚未解决的场景问题

| 问题 | 复现方式 |
| --- | --- |
| 清理命令的退出码仍需显式断言 | 写一个含 `sleep: 0` 的临时场景，teardown 加 `command: "exit 7"` 且省略 `expect`。本轮工具输出 `ok exit 7`，场景退出 0。UI 的多个删除步骤仍无 `expect.exitCode`，最后只断言账号数。正常执行和故意业务断言失败时的数据清理均已验证。 |
| UI 认证日志仍可能匹配历史请求 | UI 场景捕获了 `logStart`，但随后的 grep 仍从整个日志文件搜索 HTTP 200。运行 UI 场景，查看步骤 4 和步骤 5 即可确认两条命令的输入没有连接。 |
| 旧版探针依赖固定账号和累计日志 | 运行 `b1-ios-logout.yml`，对比证据的本轮 `login.failed`、历史日志断言通过及最终的 `0 >= 1` 失败。当前环境已实际复现。 |

## 证据位置

本机证据目录为 `/tmp/verify-retest-2026-11-07.6n8tax3y/`。子目录 `api`、`ui`、`suite`、`intentional-failure` 分别保留实跑结果，`cleanup-after-pass` 与 `cleanup-after-fail` 分别保留 9 项计数，`suite-diagnosis` 保留套件失败的只读查证。

复测使用现有 verify CLI，故意失败和只读核查场景放在临时目录。`status check` 为 0 错误、6 条既有提示，文档校验通过。
