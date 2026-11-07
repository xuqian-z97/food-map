# verify 工具第二轮修复复测（2026-11-07）

**有条件可用。** 上轮样例中的不透明 Token、普通长度密码和 JWT 已通过 stdout、stderr 及 JSON 证据的复测。场景自定义规则在失败 stderr 和套件证据中仍会漏用，短密码在文本响应副本中仍会泄露。全套结果从 3/5 提升到 4/5，iOS 登出已通过，唯一失败项是全库身份一致性。

测试基线为 `b25a3be`，包含脱敏修复 `bc5980d` 和 iOS 场景修复 `b25a3be`。对照 [上一轮复测报告](verify-tool-usability-2026-11-07.md)。本地运行环境为 Node 24.14.0，PostgreSQL、Redis 和 Nacos 均运行，认证健康检查返回 200，iPhone 17 Pro 模拟器已启动（iOS 26.5）。

## 实跑结果

| 项目 | 退出码 | 结果 | 墙钟耗时 |
| --- | --- | --- | --- |
| 目录 `--list` | 0 | 列出 5 个场景 | 0.04 秒 |
| 目录 `--dry-run` | 0 | 5 个场景校验通过 | 0.04 秒 |
| `b1-logout.yml --json` | 0 | 12/12 通过 | 0.73 秒 |
| `b1-ui-login.yml --json` | 0 | 16/16 通过，实际运行 1 个 XCUITest | 18.23 秒 |
| 目录 `--json` | 1 | 4/5 场景通过，共 80 个步骤 | 35.23 秒 |
| 临时 UI 场景故意改坏断言 | 1 | 15/16 通过，8 个 teardown 均通过 | 16.33 秒 |

全套 stdout 返回以下 `summary`。

```json
{"scenarios":5,"passed":4,"failed":1,"steps":80}
```

单元测试 **28/28 通过**。临时场景的未知断言 `typoAssertion` 仍在 `--dry-run` 阶段返回 **2**。故意失败仅修改临时副本中的 XCUITest 期望值和工程路径，CLI 输出 `equals: expected 1 / actual 0`，实际 XCUITest 用例通过。

## 脱敏复测

真实 API、UI、全套和故意失败的 stdout/stderr 均在内存中扫描，检出的原始 JWT、测试密码和未遮盖的敏感字段均为 0。本轮保留的 26 份真实场景及只读核查 `.txt` / `.json` 证据也未匹配原始 JWT 或测试密码。扫描范围为这些文本和 JSON 输出。

用上一轮的本机 HTTP fixture 方法复测，响应包含不透明 `accessToken`、合成 JWT `refreshToken`、普通长度 `password` 和由场景 `config.redact` 匹配的 `note`。表格只列检出的原始字段。所有值均为合成标记，临时 fixture 文件和证据已清理。

| 场景 | 退出码 | stdout | stderr | 场景 JSON | 套件 JSON |
| --- | --- | --- | --- | --- | --- |
| 单场景通过 | 0 | 无 | 无 | 无 | — |
| 套件通过 | 0 | 无 | 无 | 无 | `note` |
| 单场景 JSON 断言失败 | 1 | 无 | `note` | 无 | — |
| 套件 JSON 断言失败 | 1 | 无 | `note` | 无 | `note` |
| 3 字符合成密码，单场景通过 | 0 | `password` | 无 | `password` | — |
| 3 字符合成密码，单场景失败 | 1 | `password` | `password` | `password` | — |

**P0-R1 / R2 剩余项。** 在上一轮 fixture 的 `config.redact` 中保留 `fixture-custom-[A-Z]+`，让 `note` 匹配该规则。目录运行后比较场景证据和套件证据，只有套件证据保留原值。把 JSON 断言的 `code` 改成不匹配的值后，单场景和套件的失败 stderr 都保留原值。CLI 的进度输出及套件证据写入使用默认规则，缺少该场景的自定义规则。

**P0-R3 短密码仍可从文本响应副本泄露。** 让本机 fixture 返回 HTTP 200 和 JSON，在 `data.password` 放入 3 字符合成值。运行带 `--json` 的 HTTP 场景，在 stdout 与场景证据中搜索该值即可复现。再改坏 `expect.json.code`，同样的值也会进入 stderr。结构化字段已有按键脱敏，文本内的 JSON 正则仍要求值长度至少为 4，HTTP 响应文本副本会绕过这层保护。

## 场景修复和清理复查

- **UI 登录日志切片已生效。** 正常单场景、套件和故意失败的副本都在 setup 采集日志行数，再从偏移后的新增行中各匹配到 1 条登录 HTTP 200。窗口仍可包含同一时间内其他请求。
- **UI 清理继续通过。** 正常和故意失败分别用用户 ID `200082`、`200087` 查库，两次均为 9/9 项计数为 0。
- **iOS 登出已通过，但新增探针数据残留。** 探针自行注册临时账号，本轮时间窗内出现 `login.ok`、`logout.begin` 和 `logout.end`，REVOKED 计数为 1，场景 17/17 通过。独立查询注册返回的 `userId=200084`，认证库 6 项均为 0，用户库的 `users`、`user_profiles`、`user_settings` 各有 1 条。复现时运行 `b1-ios-logout.yml`，再用注册返回的 userId 分别查询这三张表。
- **App 探针断言仍读取累积日志。** 查看 `b1-ios-logout.yml` 的 `app: readlog` 步骤和本轮 JSON。断言检查整段日志，setup 捕获的 `logStart` 未用于该步骤。本轮通过另外核对时间戳确认了确实有新登录和登出。
- **UI teardown 命令失败仍可能显示为通过。** 临时场景用 `teardown: [{ command: "exit 7" }]` 且省略 `expect`，输出 `ok exit 7`、CLI 退出 0。UI 的删除步骤仍缺显式退出码断言，独立的 9 项查库能验证本次实际清理。

唯一失败的 `b1-identity-consistency.yml` 为 14/16 通过。有效绑定实测 19 条、期望 32 条，7 个有效账号没有绑定。按创建时间复查得到 7 个均早于本轮，复测开始后创建的账号缺绑定计数为 0。上轮的孤立绑定、孤立会话，本轮均为 0。可直接运行该场景并用 `identity-observation.yml` 的只读查询复核。

## 证据位置

证据根目录为 `/tmp/verify-retest-b25a3be.f0s69j9n/`。其中 `manifest.json` 汇总耗时、退出码、扫描计数和清理结果，`suite` 保留五个场景及套件证据，`cleanup-ui-pass`、`cleanup-ui-fail` 和 `cleanup-ios-probe` 保存独立查库结果，`suite-diagnosis` 保存历史账号缺绑定的查证。

`status check` 为 0 错误、6 条既有提示，文档校验通过。
