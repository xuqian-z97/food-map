# verify 工具第三轮复测（2026-11-07）

**有条件可用。** 上轮自定义规则与 3 字符密码的脱敏样例均已通过。新补测确认，带双引号的密码可从 HTTP 文本响应和失败日志泄露后缀。全套仍为 4/5 通过，唯一失败项是 7 个历史账号缺少身份绑定。iOS 登出探针通过后仍留下 3 条用户库记录。

基线为 `72aa55f`，包含脱敏修复 `0323f20` 和逐账号身份绑定断言。对照 [第二轮复测报告](verify-tool-usability-2026-11-07-b25a3be.md)。本地 Node 为 24.14.0，PostgreSQL、Redis 和 Nacos 在运行，网关认证健康检查返回 200，iPhone 17 Pro 模拟器已启动（iOS 26.5）。

## 真实场景

| 项目 | 退出码 | 结果 | 墙钟耗时 |
| --- | --- | --- | --- |
| 目录 `--list` | 0 | 列出 5 个场景 | 0.04 秒 |
| 目录 `--dry-run` | 0 | 5 个场景通过 | 0.04 秒 |
| `b1-logout.yml --json` | 0 | 12/12 通过 | 0.59 秒 |
| `b1-ui-login.yml --json` | 0 | 16/16 通过，XCUITest 1/1 | 17.38 秒 |
| 目录 `--json` | 1 | 4/5 场景通过，共 79 步 | 34.93 秒 |
| 临时 UI 场景故意改坏断言 | 1 | 15/16 通过，8 个 teardown 均通过 | 15.90 秒 |

套件 stdout 汇总如下。

```json
{"scenarios":5,"passed":4,"failed":1,"steps":79}
```

工具单元测试 **28/28** 通过。未知断言 `typoAssertion` 在临时场景的 `--dry-run` 返回 2。故意失败把临时 UI 副本的 XCUITest 失败数期望改为 1，实际用例仍通过，CLI 输出 `equals: expected 1 / actual 0`。

唯一失败的 `b1-identity-consistency.yml` 为 13/15 通过。新版逐账号查询发现 7 个账号缺少所需绑定，另一步也检出 7 个完全没有绑定的账号。按注册时间做只读查询，7 个均早于本轮开始，复测开始后创建的账号缺绑定计数为 0。孤立绑定和孤立会话计数均为 0。身份双写、iOS 登出、接口退出和真实 UI 登录均通过。

## 脱敏复测

在内存中扫描真实场景的 stdout 和 stderr，未检出原始 JWT 或测试密码。JSON stdout 中未遮盖敏感字段的计数为 0。真实场景、套件及只读查证共生成 26 份 `.txt` / `.json` 证据，未匹配原始 JWT 或测试密码。

用本机 HTTP fixture 分别测试 3 种响应，每种执行单场景通过、单场景断言失败、套件通过和套件断言失败。临时 fixture 与证据已经清理，值均为合成标记。

| 响应样例 | 结果 |
| --- | --- |
| 上轮的 JWT、不透明 Token、普通密码及 `config.redact` 自定义标记 | 4/4 组合均未在 stdout、stderr、场景或套件证据中找到原始标记。上轮的自定义规则漏用未再复现。 |
| 3 字符密码 | 4/4 组合均未检出原值。 |
| 带双引号的密码 | 4/4 组合的 stdout 和场景 JSON 都含原始后缀。套件 JSON 也含后缀。两个故意失败组合还在 stderr 和文本证据中检出后缀。 |

**P0-R4 带引号的密码在文本响应中只被部分遮盖。** 让本机 fixture 返回 HTTP 200 和 JSON，`data.password` 使用包含双引号的合成值，双引号后接可单独搜索的标记。用 JSON 序列化器生成响应后执行带 `--json` 的 HTTP 场景。结构化 `actual.json.data.password` 已被遮盖，`actual.body`、`actual.content` 和相应 JSON 证据仍包含双引号后的标记。把 `expect.json.code` 改成不匹配的值，stderr 和失败的文本证据也会保留该后缀。当前文本正则在 JSON 转义引号处结束匹配。

## 清理结果

- UI 正常和故意失败后，按注册返回的 userId 及运行时间窗分别复查 9 项，两次均为 0。UI 单场景、套件和故意失败三次各在新增认证日志行内匹配到 1 条登录 HTTP 200。
- iOS 登出探针的 17 步全部通过。运行时间窗内出现新的登录、登出开始和登出完成记录，REVOKED 计数为 1。按 `userId=200099` 独立复查，认证库 6 项为 0，用户库 `users`、`user_profiles`、`user_settings` 各残留 1 条。运行 `b1-ios-logout.yml` 后按当次注册返回的 userId 查这三张表可复现。探针仍使用整段累计 App 日志做断言。
- 清理命令的非零退出码仍需要显式断言。用临时场景执行未加 `expect` 的 `teardown: [{ command: "exit 7" }]`，输出 `ok exit 7`，CLI 退出 0。UI 清理结果通过上述独立查库验证。

证据位于 `/tmp/verify-retest-72aa55f.6lffcljv/`。其中 `manifest.json` 汇总真实运行，`suite-diagnosis` 保存逐账号只读查证，`cleanup-ui-pass`、`cleanup-ui-fail` 和 `cleanup-ios-probe` 保存清理计数。`status check` 为 0 错误、6 条既有提示，文档校验通过。
