# 方案 D 本地依赖感知验证入口独立可用性测试

测试日期为 2026-11-09（Asia/Shanghai）。

**结论：有条件可用。** 九项必测验证符合预期。依赖齐全时严格模式通过五个真实场景，XCUITest 实际运行并通过一个用例。部分或全部缺依赖时，跳过和通过分别计数，严格模式拒绝跳过，断言失败与场景配置错误均返回预期退出码。附加测试发现 JUnit 写入失败时进程和结构化证据的状态不一致。使用 `--junit` 的调用方需同时检查进程退出码和 JUnit 文件是否生成。

## 基线、环境与边界

- 分支为 `main`，实际 HEAD 为 `0b9a4ac274c36a4465f3a7a3a39bda2edaf47764`。执行 `git merge-base --is-ancestor 0b9a4ac HEAD` 返回 0，HEAD 就是指定提交。开始和结束时被测目录 `harness/`、`tools/verify/` 均与 HEAD 一致。
- 开始时已有其他会话的台账、看板及文档改动。测试在原工作区执行，未切分支或回滚。新增报告前，状态快照和已跟踪文件的 diff 均与开始时一致。
- macOS 26.6.2（25G83，arm64），Node v24.14.0，系统 Bash 3.2.57，系统 `/bin/sh`，Xcode 27.2（27B52）。Docker 连接到 OrbStack，认证日志文件可读。
- 初始沙箱中 Docker socket 返回 `operation not permitted`，CoreSimulator 连接不可用并报告日志目录权限错误，本地网关探测也无法连接。获准在沙箱外重做只读探测后，两条网关路由均为 HTTP 200、两个 PostgreSQL 库的 `select 1` 均返回 1、Redis 返回 `PONG`，配置指定的模拟器可查询已安装 App，`launchctl list` 探测成功。真实场景与 XCUITest 在获准的沙箱外进程中完成。
- 测试密码为当次生成的测试账号密码，仅通过进程环境变量 `FM_TEST_PW` 注入。证据目录中未检出本次生成密码的特征。未关闭或重启服务，未停止模拟器，未清理历史数据。两个 iOS 场景保留原有 teardown，只针对本次生成的测试账号。接口退出和双写场景按原流程运行，其本次生成的记录未额外清理。
- 所有模拟、故障注入和包装测试使用临时文件。未修改工具、业务代码、原场景、原断言、业务验收结论或测试台账。

## 命令与结果

以下命令从仓库根目录运行。计数列按 `passed / failed / skipped` 排列，默认单位为场景。静态校验和回归单测单独标注。证据文件名均相对于 `/tmp/verify-d-usability`，复现时先设置 `D=/tmp/verify-d-usability`。第 2–4 项运行前注入 `FM_TEST_PW`，普通模式清除 `VERIFY_REQUIRE_FULL`。

| 项目 | 实际命令 | 退出码 | 计数 | 观察与证据 |
| --- | --- | ---: | --- | --- |
| 1 静态校验 | `(unset FM_TEST_PW VERIFY_REQUIRE_FULL; ./harness/scripts/verify.sh --dry-run)` | 0 | 5 / 0 / 0（静态校验） | 五个场景均打印 `ok`。实际执行 0 步。见 `dry-run.stdout`、`dry-run.stderr` |
| 2 真实全量 | `VERIFY_REQUIRE_FULL=1 ./harness/scripts/verify.sh --json --evidence-dir "$D" --junit "$D/full.xml"` | 0 | 5 / 0 / 0 | `ok=true,status=passed`，七类依赖就绪，80 步，约 46.8 秒。见 `full.stdout.json`、`full.stderr`、`full.xml` |
| 3 缺设备，普通 | `./harness/scripts/verify.sh --dependencies "$D/device-unavailable.json" --json --evidence-dir "$D" --junit "$D/device-normal.xml"` | 0 | 3 / 0 / 2 | `ok=false,status=partial`，46 步。预检仅 `device` 不就绪。见 `device-normal.stdout.json`、`device-normal.stderr`、`device-normal.xml` |
| 4 缺设备，严格 | `VERIFY_REQUIRE_FULL=1 ./harness/scripts/verify.sh --dependencies "$D/device-unavailable.json" --json --evidence-dir "$D" --junit "$D/device-strict.xml"` | 1 | 3 / 0 / 2 | `ok=false,status=partial`，46 步，两项跳过原因保留。见 `device-strict.stdout.json`、`device-strict.stderr`、`device-strict.xml` |
| 5a 全缺依赖，普通 | `./harness/scripts/verify.sh --dependencies "$D/all-unavailable.json" --json --evidence-dir "$D" --junit "$D/all-normal.xml"` | 0 | 0 / 0 / 5 | `ok=false,status=partial`，0 步，七类依赖均不可用。见 `all-normal.stdout.json`、`all-normal.xml` |
| 5b 全缺依赖，严格 | `VERIFY_REQUIRE_FULL=1 ./harness/scripts/verify.sh --dependencies "$D/all-unavailable.json" --json --evidence-dir "$D" --junit "$D/all-strict.xml"` | 1 | 0 / 0 / 5 | `ok=false,status=partial`，0 步。见 `all-strict.stdout.json`、`all-strict.xml` |
| 6 真实断言失败和跳过并存 | `node tools/verify/cli.mjs run "$D/mixed" --dependencies "$D/all-unavailable.json" --json --evidence-dir "$D"` | 1 | 0 / 1 / 1 | `ok=false,status=failed`，断言期望退出 23、实际为 0。跳过场景三个阶段的标记文件均未生成。见 `mixed.stdout.json` |
| 7a 非法 requires | `node tools/verify/cli.mjs run "$D/invalid-requires" --dependencies "$D/all-unavailable.json" --json --evidence-dir "$D"` | 2 | 0 / 0 / 0（运行前拒绝） | `ok=false`，报 `requires 必须是依赖名字符串列表`。见 `invalid-requires.stdout.json`、`invalid-requires.stderr` |
| 7b 未知断言 | `node tools/verify/cli.mjs run "$D/invalid-assertion" --dependencies "$D/all-unavailable.json" --json --evidence-dir "$D"` | 2 | 0 / 0 / 0（运行前拒绝） | `ok=false`，报 `steps[0].expect 包含未知断言 bogusAssertion`。见 `invalid-assertion.stdout.json`、`invalid-assertion.stderr` |
| 8a run-all 失败透传 | `(unset FM_TEST_PW; VERIFY_EVIDENCE_DIR="$D" VERIFY_REQUIRE_FULL=1 ./harness/scripts/run-all.sh)` | 1 | 3 / 0 / 2 | 七个基础检查后进入真实方案 D 入口。仅因缺测试密码跳过两项 iOS 场景。套件和外层进程均退出 1，未打印末尾完成提示。见 `run-all.stdout`、`run-all.rc` |
| 8b 包装兼容 | `PATH="$D/bin:$PATH" /bin/bash ./harness/scripts/verify.sh` 和 `PATH="$D/bin:$PATH" /bin/sh ./harness/scripts/verify.sh`，各再带 `--only 'contains space'` 执行一次 | 四次均为 0 | 不适用（参数转发） | 临时 `node` 仅回显 argv。空用户参数时正常传入默认清单及证据目录，带空格值保持单一参数，stderr 均为空。见 `{bash,sh}-{empty,spaced}.args` 和对应 `.rc` |
| 8c JSON 和 JUnit | 解析第 2–7 项的 stdout 及第 2–5 项的 JUnit，分别用 JSON 解析器和 XML 解析器 | 解析为 0 | 见对应运行 | 各 stdout 均为一份 JSON，预检和进度在 stderr。两次缺设备各有 2 个 `<testcase><skipped …/></testcase>`，两次全缺依赖各有 5 个，跳过节点数与各套件的 `skipped` 属性一致 |
| 9 回归 | `node --test tools/verify/test/ harness/test/verify-entry.test.mjs` | 0（沙箱外重跑） | 42 / 0 / 0（单测） | 沙箱内首次退出 1，37 通过、5 失败、0 跳过，失败为本地 HTTP fixture 绑定 `127.0.0.1` 时的 `listen EPERM`。获准沙箱外重跑全通过。见 `unit.tap`、`unit-escalated.tap` |

### 关键证据交叉核对

- **静态校验未触发预检。** 额外执行 `(unset FM_TEST_PW; ./harness/scripts/verify.sh --dry-run --dependencies "$D/dry-sentinel.json" --json --evidence-dir "$D")`，退出 0，五个静态结果，`dryRun=true`。临时清单中的 device 检查若运行会生成 `dry-preflight.ran`，该文件未生成。见 `dry-sentinel.stdout.json`。
- **XCUITest 实际执行。** `full.stdout.json` 的 xcode 步骤记录 `exitCode=0, total=1, passed=1, failed=0, skipped=0`，测试明细为 `LoginFlowTests/testLoginThroughGateway()`，状态 `Passed`。使用 `xcrun xcresulttool get test-results summary --path <xcode 步骤的 resultBundle> --format json` 独立读取原始 `.xcresult`，同样得到 1 项执行并通过。套件 runId 为 `suite-d971aee5-858c-46fd-80af-eb91ecac4da0`。
- **缺设备时跳过两个 iOS 场景。** 普通、严格两次只将项目清单的 `device.checks` 替换为 `/usr/bin/false`。身份一致性、认证双写、接口退出三个场景执行并通过。两个 iOS 场景的 `steps=[]`，对应套件文本无其 setup、step、teardown 记录。两份 JSON 与对应套件文本都包含相同的 `device: fixture: device check intentionally exits 1` 原因。runId 分别为 `suite-a5ca6da8-b1a1-408c-86ae-92f9f929e3a1`、`suite-3b98909f-c86e-4063-933e-6fb4eb7aa0aa`。
- **全跳过和配置错误均无业务步骤。** 全缺依赖副本把七个检查组全部替换为 `/usr/bin/false`，两次总步数均为 0，每个场景的跳过理由均出现在 JSON 与对应套件文本。两个非法配置目录都把带 setup、step、teardown 标记命令的合法场景排在非法场景之前，所有标记均未生成，配置错误 JSON 无套件汇总。第 6 项独立用临时场景的三阶段标记验证跳过行为。
- **统一入口使用真实场景。** `run-all.stdout` 引用的套件证据 runId 为 `suite-6d964526-90df-4118-b727-fb588ab57959`。输出保留两个 iOS 场景及缺少 `FM_TEST_PW` 的原因。该轮用于验证退出码透传，真实 iOS 执行的证据见第 2 项。

## 问题与影响

### P2 · JUnit 写入失败后 JSON 和已保存证据仍标记通过

**复现**

1. 在临时目录 `$D/junit-write-error` 创建一个无外部依赖的场景，用 `command: "exit 0"` 和 `expect: { exitCode: 0 }` 使唯一业务步骤通过。
2. 确保 `$D/absent-parent` 不存在，并运行以下命令。

   ```sh
   node tools/verify/cli.mjs run "$D/junit-write-error" \
     --dependencies "$D/all-unavailable.json" --json --evidence-dir "$D" \
     --junit "$D/absent-parent/report.xml"
   ```

**实测**。场景计数为 1 / 0 / 0。JUnit 未生成，stderr 明确报告 `JUnit 写入失败`（`ENOENT`），进程退出 2。stdout JSON 同时含 `exitCode=2, ok=true, status=passed`。已落盘的套件 JSON 仍含 `exitCode=0, ok=true, status=passed`。证据为 `junit-write-error.stdout.json`、`junit-write-error.stderr`、`junit-write-error.rc` 和 runId `suite-878b404a-7811-40eb-8584-d48250b3a93b` 对应的套件证据。

**影响**。只采集已落盘证据或只判断 JSON 的 `ok/status` 的消费者，可能将缺失指定 JUnit 产物的运行记为成功。进程退出码和 stderr 已给出失败信号。该问题未影响本轮指定有效 JUnit 路径的结果，也未改变场景的实际通过计数。

## 假通过检查与未执行项

- 指定必测路径未观察到跳过计入通过、断言或场景配置错误被隐藏、业务步骤未执行却记为通过。附加的 JUnit 故障中，场景实际执行成功，未生成的 JUnit 产物与结构化运行状态存在上述不一致。
- 必测项目无未执行项。第 3–5、6、8a 项的有意跳过已经分列。沙箱内被权限阻止的探测及单测已获准重试，真实 iOS 场景和 XCUITest 也已完成。
- 临时证据目录保留 stdout、stderr、套件 JSON/文本、JUnit、回归结果及故障配置，用于本轮复核。仓库内仅新增本报告。
