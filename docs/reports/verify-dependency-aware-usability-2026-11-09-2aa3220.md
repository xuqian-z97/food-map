# 方案 D 自定义脱敏修复与全量复测

测试日期为 2026-11-09（Asia/Shanghai）。实际基线为 `main` 的 `2aa322031afe9b92e3d2eb654724f83caba00c74`，包含 `0b9a4ac` 和 `d4b62c7`。工具和场景目录与 HEAD 一致。

**结论：可用。** P1 自定义脱敏回归已修复，P2 的运行状态和证据一致性通过复测。严格模式五个真实场景通过，原始 XCUITest 结果确认一个用例执行并通过。本轮覆盖范围内未发现新问题。

## 环境与证据

- macOS 26.6.2（arm64），Node v24.14.0，Xcode 27.2，系统 Bash 3.2.57 与 `/bin/sh`。回归测试和真实验证获准在沙箱外运行，分别使用本机临时端口、现有 OrbStack 服务及 CoreSimulator。全量预检的七类依赖均就绪。
- `D=/tmp/verify-d-retest-2aa3220.9o0ZL6`。目录内保留每次运行的退出码、stdout、stderr、套件证据与 JUnit，以及 `fixture-checks.txt`、`live-checks.txt`、`xcresult-summary.json`、`unit.tap`。模拟缺依赖使用临时清单，脱敏测试仅使用合成标记。真实验证的测试密码仅由进程环境 `FM_TEST_PW` 注入，已检查的证据中未检出本次生成密码的特征。
- 原工作区未切换，已有改动和历史报告保留。未关闭或重启服务，未停止模拟器，未删除历史数据。原 iOS 场景只通过自身 teardown 清理本次生成的测试账号。仓库内仅新增本报告。

## P1 与 P2 复测

将 [P1 报告中的同一临时场景](/Users/zhangxuqian/myPrjDev/food-map/docs/reports/verify-dependency-aware-usability-2026-11-09-d4b62c7.md:26) 放在 `$D/redact`。JUnit 目标的父目录保持不存在。

```sh
node tools/verify/cli.mjs run "$D/redact" --json \
  --evidence-dir "$D/redact-suite-bad" \
  --junit "$D/absent-parent/report.xml"
```

- 进程退出 `2`，stdout JSON 和落盘套件 JSON 同为 `ok=false,status=failed,exitCode=2`，并包含相同的 `junitError`。文本证据为 `FAIL`，stderr 有写入错误，JUnit 未生成。场景计数为 1 通过、0 失败、0 跳过。
- 上轮出现原始标记的 `steps[0].display` 和 `actual.stdout`、`actual.value`、`actual.content` 已遮盖。检查 stdout、stderr、套件与单场景 JSON 和文本证据，均未发现原始标记。套件 JSON 保留 `[REDACTED]`。套件 runId 为 `suite-1fa9edb1-3aa3-432b-aaa4-85df92c315b2`。
- 对照测试覆盖单文件和套件模式的有效与无效 JUnit 路径。有效路径退出 `0` 并生成可解析 JUnit，无效路径退出 `2`。使用两条不同规则的额外套件含一项真实断言失败，写入失败后两个标记仍均被遮盖，套件保留 1 通过、1 失败、0 跳过。见 `redact-file-*`、`redact-suite-*`、`multi-redact-result/`。
- 使用无外部依赖的原 P2 fixture 单独复现，退出 `2`，stdout、落盘 JSON 和文本均标记运行失败，保留 1 通过、0 失败、0 跳过。见 `original.stdout.json`、`original/`。

## 方案 D 回归

以下命令从仓库根目录执行。计数为通过 / 失败 / 跳过。第 2–4 项注入本次的 `FM_TEST_PW`，普通模式清除 `VERIFY_REQUIRE_FULL`。

| 项目 | 命令 | 退出码 | 计数与观察 |
| --- | --- | ---: | --- |
| 1 静态校验 | `(unset FM_TEST_PW VERIFY_REQUIRE_FULL; ./harness/scripts/verify.sh --dry-run --json)` | 0 | 5 个静态校验通过。用临时 `dry-sentinel.json` 复查，预检标记和运行证据目录均未生成 |
| 2 真实全量 | `VERIFY_REQUIRE_FULL=1 ./harness/scripts/verify.sh --json --evidence-dir "$D/full" --junit "$D/full.xml"` | 0 | 5 / 0 / 0，80 步，`ok=true,status=passed`，约 38.5 秒 |
| 3 缺设备普通模式 | `./harness/scripts/verify.sh --dependencies "$D/device-unavailable.json" --json --evidence-dir "$D/device-normal" --junit "$D/device-normal.xml"` | 0 | 3 / 0 / 2，46 步，`ok=false,status=partial` |
| 4 缺设备严格模式 | `VERIFY_REQUIRE_FULL=1 ./harness/scripts/verify.sh --dependencies "$D/device-unavailable.json" --json --evidence-dir "$D/device-strict" --junit "$D/device-strict.xml"` | 1 | 3 / 0 / 2，46 步，`ok=false,status=partial` |
| 5 全缺依赖 | `./harness/scripts/verify.sh --dependencies "$D/all-unavailable.json" --json --evidence-dir "$D/wrapper-normal" --junit "$D/wrapper-normal.xml"`，严格模式加 `VERIFY_REQUIRE_FULL=1` 并改用 `wrapper-strict` 证据路径 | 普通 0，严格 1 | 两次均为 0 / 0 / 5，0 步，`ok=false,status=partial` |
| 6 断言失败与跳过并存 | `node tools/verify/cli.mjs run "$D/mixed" --dependencies "$D/all-unavailable.json" --json --evidence-dir "$D/mixed-result"` | 1 | 0 / 1 / 1，`status=failed`，跳过场景三个阶段的标记均未生成 |
| 7 非法配置 | `node tools/verify/cli.mjs run "$D/invalid-requires" --dependencies "$D/all-unavailable.json" --json --evidence-dir "$D/invalid-requires-result"`，再将场景目录和证据名改为 `invalid-assertion` | 两次均为 2 | 无场景执行。分别报告 `requires 必须是依赖名字符串列表` 和 `steps[0].expect 包含未知断言 bogusAssertion`。前序合法场景的标记未生成 |
| 8 统一入口 | `(unset FM_TEST_PW; VERIFY_EVIDENCE_DIR="$D/run-all" VERIFY_REQUIRE_FULL=1 ./harness/scripts/run-all.sh)` | 1 | 3 / 0 / 2。实际进入方案 D，因密码缺失跳过两个 iOS 场景，外层透传退出码 1 |
| 8 兼容与报告 | 使用临时 `node` 回显参数，分别以 `/bin/bash` 和 `/bin/sh` 执行 `./harness/scripts/verify.sh`，分别使用空参数和 `--only 'contains space'`。用解析器检查运行输出 | 包装四次均为 0 | 空参数及含空格参数正常转发。stdout 为单份 JSON。缺设备和全缺依赖的 JUnit 分别有 2 个和 5 个 `<skipped>`。对应 JSON 和套件文本都保留跳过原因，被跳过场景无执行步骤 |
| 9 回归测试 | `node --test tools/verify/test/ harness/test/verify-entry.test.mjs` | 0 | 44 / 0 / 0（单测），含 JUnit 状态和脱敏回归 |

真实全量 runId 为 `suite-4c31644c-1047-4424-b1d8-e0248b999c7f`。xcode 步骤记录 `LoginFlowTests/testLoginThroughGateway()` 通过。另以 `xcrun xcresulttool get test-results summary --path <resultBundle> --format json` 读取原始结果，确认 `totalTestCount=1,passedTests=1,failedTests=0,skippedTests=0`。

必测项目无未执行项。模拟缺依赖所产生的跳过已单独计数，未观察到跳过计为通过、断言或配置错误被隐藏，或未执行却报成功。
