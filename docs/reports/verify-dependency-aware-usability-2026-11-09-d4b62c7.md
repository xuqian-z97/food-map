# 方案 D JUnit 写入失败修复复测

测试日期为 2026-11-09（Asia/Shanghai）。实际基线为 `main` 的 `d4b62c77af2040b60aac0a939fd1be9470fba535`，包含实施提交 `0b9a4ac`。被测工具和 harness 与 HEAD 一致。旧基线对照使用 `git archive` 导出到临时目录，原工作区未切换。

**复测结论：原 P2 已修复，新发现 P1 脱敏回归。** JUnit 写入失败时，退出码、stdout JSON 和落盘套件证据均标记失败。若套件使用自定义脱敏规则，失败后的证据重写会把本应遮盖的数据写回套件 JSON。

## 命令与结果

在 macOS 26.6.2、arm64、Node v24.14.0 上执行。以下 `$D` 为 `/tmp/verify-d-retest-d4b62c7.Eslo4u`，该目录内有命令脚本 `run-fixtures.sh`、原始输出、退出码和交叉核对结果。计数为通过 / 失败 / 跳过，单文件模式统计步骤，目录模式统计场景。

| 测试 | 命令或变体 | 退出码 | 计数 | 结果 |
| --- | --- | ---: | --- | --- |
| 原问题复现 | `node tools/verify/cli.mjs run "$D/plain" --dependencies "$D/all-unavailable.json" --json --evidence-dir "$D/original" --junit "$D/absent-parent/report.xml"` | 2 | 1 / 0 / 0 | stdout 和套件 JSON 均为 `ok=false,status=failed,exitCode=2`，均包含 `junitError`。文本证据为 `FAIL`，stderr 说明目录不存在，JUnit 未生成 |
| 单文件错误路径 | 对 `"$D/plain/pass.yml"` 执行同类命令，省略目录专用的 `--dependencies` | 2 | 1 / 0 / 0 | stdout 和单场景 JSON 同步失败，文本为 `FAIL` |
| 正常 JUnit 对照 | 单文件和套件各运行一次，`--junit` 指向存在的父目录 | 均为 0 | 各 1 / 0 / 0 | 退出码与 stdout、落盘 JSON 一致，JUnit XML 均可解析 |
| 静态入口 | `./harness/scripts/verify.sh --dry-run --json`（未设置 `FM_TEST_PW`） | 0 | 5 / 0 / 0（静态校验） | 五个场景通过静态校验 |
| 包装入口跳过对照 | `./harness/scripts/verify.sh --dependencies "$D/all-unavailable.json" --json --evidence-dir <证据目录> --junit <有效路径>`，普通和 `VERIFY_REQUIRE_FULL=1` 各一次 | 普通 0，严格 1 | 各 0 / 0 / 5 | 两次均为 `partial`，每份 JUnit 均有 5 个 `<skipped>`，实际执行 0 步 |
| 包装入口跳过并写入失败 | 同上，普通模式使用 `--junit "$D/absent-parent/report.xml"` | 2 | 0 / 0 / 5 | 运行状态和落盘证据同步为 `failed`，场景计数仍保留 5 个跳过 |
| 回归 | `node --test tools/verify/test/ harness/test/verify-entry.test.mjs` | 0 | 43 / 0 / 0（单测） | 含新增 JUnit 用例。获准在沙箱外运行，允许 HTTP fixture 绑定本地端口。见 `unit.tap` |

场景本身执行成功而 JUnit 写入失败时，场景计数保留通过，运行级状态明确标记失败。

## P1 · 套件证据重写丢失自定义脱敏规则

**复现。** 在临时场景目录写入以下 fixture。标记是合成测试字符串，不含真实敏感信息。

```yaml
name: fixture custom redaction
config:
  redact:
    - "QA_MARKER_CUSTOM_8917"
steps:
  - command: "printf QA_MARKER_CUSTOM_8917"
    expect: { exitCode: 0 }
```

父目录 `$D/absent-parent` 保持不存在，用目录模式执行以下命令。

```sh
node tools/verify/cli.mjs run "$D/redact" --json \
  --evidence-dir "$D/redact-suite-bad" \
  --junit "$D/absent-parent/report.xml"
```

**观察。** 进程退出 2，stdout 和落盘套件 JSON 都标记失败。合成标记未出现在 stdout、stderr 或独立的场景 JSON，但在重写后的套件 JSON 中原样出现在 `scenarios[0].steps[0].display` 和 `actual.stdout`、`actual.value`、`actual.content`。同一 fixture 的三组对照均正确脱敏，分别为旧提交 `0b9a4ac` 的套件写入失败、当前提交的套件写入成功、当前提交的单文件写入失败。见 `redaction-comparison.txt`、`redact-suite-bad/`、`redact-suite-good/`、`redact-file-bad/`、`old-redact-suite-bad/`。

**定位与影响。** `tools/verify/cli.mjs` 首次写套件证据时传入含 `suiteRules` 的脱敏器，JUnit 失败后重写时却取只含默认规则的 `result.__redactor`。因此仅受场景自定义规则保护的内容，可能在 JUnit 路径无效或写入失败后进入落盘套件 JSON。实际泄露范围取决于场景产生的证据，本次只验证了合成标记。

## 范围

本轮复测针对 JUnit 修复和相关回归。未重新执行真实服务场景或 XCUITest，上轮基线的真实全量结果保留在原报告。复测未关闭或重启服务，未操作模拟器，未清理历史数据。仓库内仅新增本报告，保留原报告和工作区已有改动。
