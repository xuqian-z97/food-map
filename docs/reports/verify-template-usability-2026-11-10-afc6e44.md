# Verify 跨项目复用模板可用性验证

日期为 2026-11-10（Asia/Shanghai）。本次结论为 **有条件可用**。独立工程可以直接运行模板和新增场景，缺依赖时终端、JSON、JUnit 和证据都把跳过与通过分开。复制说明的示例命令会带走已有证据，整目录也未达到约定的字符串零命中。

## 基线与环境

- `git log -1 --oneline` 实际输出 `afc6e44 文档与示例：新增跨项目复用模板，修复复制即用时的两处耦合`，与指定基线一致。开始时 `git status --porcelain=v1 --untracked-files=all` 输出为空。
- macOS 26.6.2（25G83），arm64，Node.js v24.14.0，Python 3.11.4。验证前 `VERIFY_REQUIRE_FULL` 未设置，19100 无监听进程。模板运行没有安装 npm 包或启动 Docker、数据库、模拟器。
- **用了额外权限。** 默认沙箱第一次启动 Python 服务退出 1，`bind(127.0.0.1:19100)` 返回 `PermissionError: [Errno 1] Operation not permitted`。仓库内和仓库外的 HTTP 验证及额外在线对照使用获准的提权命令，目标均为本机回环地址。纯文件检查和预演在默认权限下完成。
- `/tmp/xproj` 已有文件，本次使用新建的 `/tmp/xproj.UJPN24`（系统解析为 `/private/tmp/xproj.UJPN24`）。仓库内运行用 `--evidence-dir` 将证据写到临时目录，停服后恢复原有的被忽略访问日志并逐字节比对。原有被忽略证据文件的路径和 SHA-256 清单一致。

## 命令与结果

下表的 P/F/S 按场景统计，即 passed / failed / skipped。耗时为子进程墙钟时间，含 CLI 启动。未执行场景的检查用「—」表示。完整命令、CWD、退出码和 stdout/stderr 保存在临时工程的 `commands.jsonl` 和 `captures/`。

下面以两个基准命令缩写表中重复部分。第 1–3 项的 `R` 从仓库根目录执行，第 4–8 项的 `X` 从临时工程根目录执行。严格模式在相应命令前加 `VERIFY_REQUIRE_FULL=1`。

```sh
# R
node tools/verify/cli.mjs run tools/verify/examples/minimal-project/scenarios \
  --dependencies tools/verify/examples/minimal-project/deps.json \
  --evidence-dir /tmp/xproj.UJPN24/ev

# X
node tools-verify/cli.mjs run scenarios --dependencies deps.json --evidence-dir ev

# 第 4 项在仓库根目录执行的三个扫描命令
grep -rn 'foodmap\|FOODMAP_\|:8080' tools/verify
grep -rn 'foodmap\|FOODMAP_\|:8080' /tmp/xproj.UJPN24/tools-verify
grep -rn 'foodmap\|FOODMAP_\|:8080' /tmp/xproj.UJPN24/tools-verify/{cli.mjs,core,adapters}
```

第 1–2 项在 `tools/verify/examples/minimal-project` 执行 `python3 server.py --port 19100`，健康请求返回 200 后再运行 CLI。第 3 项向该服务进程发送 SIGTERM 并等待其退出。第 4–6 项从临时工程根目录启动同一份 `server.py`，第 6 项生成跳过 JUnit 前再次停服。服务是常驻进程，以健康检查和停止结果记录生命周期。额外在线对照测得启动到健康检查就绪为 0.0615 s，SIGTERM 后退出用时 0.0013 s，Python subprocess 记录退出状态 `-15`。

| 项 | 实际命令 | 退出码 | 秒 | P/F/S | 执行步数或观察 |
| --- | --- | ---: | ---: | --- | --- |
| 1 | `R` | 0 | 0.2135 | 1/0/0 | 5 步，与 `smoke.yml` 的 1 setup + 4 steps 一致 |
| 2 | `VERIFY_REQUIRE_FULL=1 R` | 0 | 0.0758 | 1/0/0 | 5 步，跳过 0 |
| 3 普通 | 停服后 `R` | 0 | 0.0500 | 0/0/1 | 0 步，终端显示部分执行和 SKIP 原因 |
| 3 严格 | 停服后 `VERIFY_REQUIRE_FULL=1 R` | 1 | 0.0526 | 0/0/1 | 0 步，终端显示部分执行和 SKIP 原因 |
| 3 JSON 普通 | 停服后 `R --json` | 0 | 0.0584 | 0/0/1 | `ok:false`，`status:"partial"` |
| 3 JSON 严格 | 停服后 `VERIFY_REQUIRE_FULL=1 R --json` | 1 | 0.0495 | 0/0/1 | `ok:false`，`status:"partial"` |
| 4 复制 | `rsync -a --exclude=/evidence/ --exclude=/examples/minimal-project/logs/access.log tools/verify/ /tmp/xproj.UJPN24/tools-verify/` | 0 | 0.0209 | — | 排除已有证据和访问日志 |
| 4 复制模板文件 | `cp -R tools/verify/examples/minimal-project/{scenarios,deps.json,server.py} /tmp/xproj.UJPN24/` | 0 | 0.0033 | — | 模板三项放到临时工程根目录 |
| 4 外部运行 | `X` | 0 | 0.1033 | 1/0/0 | 5 步，运行时场景目录只包含 `smoke.yml` |
| 4 仓库全量扫描 | 上述第一个 `grep` | 0 | 0.0085 | — | 79 行命中，78 行来自已有证据，1 行来自旧示例 |
| 4 副本全量扫描 | 上述第二个 `grep` | 0 | 0.0076 | — | 复制后、生成新证据前，旧示例第 6 行命中 |
| 4 副本运行代码扫描 | 同一 `grep`，范围为副本的 `cli.mjs`、`core/`、`adapters/` | 1 | 0.0050 | — | 无命中，`grep` 的 1 表示未找到匹配 |
| 5 预演 | `X --only bad-request --dry-run` | 0 | 0.1628 | — | 服务未启动，报告 0 setup / 1 step / 0 teardown |
| 5 首次运行 | `X --only bad-request` | 0 | 0.0584 | 1/0/0 | 1 步，HTTP 400 和 JSON 断言通过 |
| 5 故障注入 | 期望改为 500 后 `X --only bad-request --json` | 1 | 0.0595 | 0/1/0 | 1 步，状态断言失败，JSON 断言仍通过 |
| 5 恢复 | 期望改回 400 后 `X --only bad-request` | 0 | 0.0591 | 1/0/0 | 1 步通过 |
| 6 JSON | `X --json` | 0 | 0.0804 | 2/0/0 | 两个场景，6 步 |
| 6 JSON + JUnit | 停服后 `X --json --junit /tmp/xproj.UJPN24/report.xml` | 0 | 0.0526 | 0/0/2 | 0 步，JUnit 含两个 `<skipped>` |
| 6 解析校验 | `python3 -c`，对两次 stdout 用 `json.loads`，对 XML 用 `xml.etree.ElementTree.parse` | 0 | 0.0234 | — | 校验 JSON 计数、stderr 进度和 XML 节点 |
| 7 文本证据 | `cat` 读取失败场景、失败套件、跳过套件的 `.txt` | 0 | 0.0036 | — | 可定位失败步骤，跳过套件有原因 |
| 7 JSON 证据 | `jq` 提取失败场景 `.json` 的 `source`、`summary` 和首步断言 | 0 | 0.0050 | — | 读到步骤级 expected/actual 和逐条断言 |
| 7 脱敏机制 | `rg -n 'redact\|Bearer\|JWT\|REDACTED' tools/verify/{cli.mjs,core/redact.mjs,core/evidence.mjs,core/suite.mjs,core/junit.mjs}` | 0 | 0.0108 | — | 确认规则和证据写入路径 |
| 8 Python 预检对照 | 服务在线，`VERIFY_REQUIRE_FULL=1 node tools-verify/cli.mjs run scenarios --only smoke --dependencies deps-python-fails.json --json` | 0 | 0.0877 | 1/0/0 | 5 步，只预检 `network`，同时验证默认证据目录 |
| 8 缺 network 清单项 | `VERIFY_REQUIRE_FULL=1 node tools-verify/cli.mjs run scenarios --only smoke --dependencies deps-no-network.json --evidence-dir ev --json` | 1 | 0.0557 | 0/0/1 | 0 步，原因是 `network: 未配置预检，无法确认就绪` |
| 8 复制命令对照 | 在临时工程的 `cp-control/source` 执行 `cp -R tools/verify /tmp/xproj.UJPN24/cp-control/target/tools/verify` | 0 | 0.0040 | — | 人工创建的 `evidence/marker` 被复制 |

第 3 项的跳过原因为 `network: 示例服务未启动（先运行 python3 server.py）`。终端汇总显示「部分执行（非全量通过）：0 通过 / 0 失败 / 1 跳过」。两次 JSON 均记录 `skipped:true`、相同的 `skipReason` 和空 `steps`。**没有出现跳过计为通过。**

第 4 项的临时工程没有 Git 元数据。整目录匹配来自 `examples/logout.yml:6` 的 `baseUrl: http://127.0.0.1:8080`。仓库扫描中的历史证据只在本地保留命中文件名和计数。

## 第 5 项：只读文档新增场景

从开始阅读复制后的工具 README 写法章节，到首次业务断言通过用时 **33.25 秒**，到故障注入并恢复通过用时 **33.43 秒**。计时包括写 YAML、预演、启动外部示例服务和命令等待，早先指定三处文档的初读在计时之前。

写法来自 README §3、§4.1、§5 和 AGENT.md §6–7。用目录模式配合 `--only bad-request` 保留清单预检。该场景未显式写 `requires`，HTTP 动作推断的 `network` 已在清单中配置。

```yaml
name: Reject an item with an empty name
config:
  baseUrl: http://127.0.0.1:19100
steps:
  - http: POST /items
    json: { name: "" }
    expect:
      status: 400
      json: { error: name_required }
```

1. 服务关闭时执行 `--dry-run`，语法首次校验通过。
2. 服务就绪后单独运行，返回 HTTP 400 和 `{"error":"name_required"}`。JSON 断言按文档采用子集匹配，证据中的实际 JSON 恰好只有该字段。
3. 仅把 `status: 400` 改成 `status: 500`。退出码为 1，stderr 在 `[1/1] http POST /items … FAIL` 下给出 `status: expected 500 / actual 400`。失败步骤的 `checks` 分别记录状态断言失败、JSON 断言通过。
4. 恢复 `status: 400`，同一场景再次通过。临时工程保留的是恢复后的文件。

没有语法或断言卡点，也没有为了写场景、理解失败或恢复通过去读源码。实际准备摩擦来自模板 README 只给了原仓库路径，需自行把命令改成复制后的目录布局，以及本机沙箱需要允许绑定回环端口。第 7 项的脱敏源码检查发生在本项完成之后。

## 输出与证据

- 第 6 项两次 stdout 都能整段交给 `json.loads`。预检和步骤进度出现在 stderr。停服时 JUnit 根节点为 `tests="2" failures="0" skipped="2"`，两个 `testcase` 都含带跳过原因的 `<skipped>`。
- 场景 `.txt` 含场景路径、runId、步骤表和 `[1] http POST /items` 的 expected/actual。场景 `.json` 的 `steps[0]` 含 `phase:"step"`、`index:1`、`expected.status:500`、`actual.status:400` 和 `checks`。套件 `.txt` 指出失败场景及 `[step 1]`，断言值在场景证据中。跳过场景和套件证据均可查到原因。
- 检查 `core/redact.mjs` 和调用点确认 Bearer、JWT、手机号和常见密码/Token 字段的内置规则，以及 `config.redact` 扩展机制。CLI 的进度和 JSON 输出、场景及套件证据均有脱敏调用。本次样本没有密钥，脱敏结论限于确认机制存在。
- 可复核的样本在 `/tmp/xproj.UJPN24`。失败步骤为 `ev/2026-11-10T10-34-9d606f64-Reject an item with an empty name.{txt,json}`，失败套件为 `ev/2026-11-10T10-34-suite-d211abbb-bf0a-4627-b6d5-29cc3b547fd0-scenarios.{txt,json}`。另有 `captures/05-fail.{out,err}`、`captures/06-down-junit.{out,err}` 和 `report.xml`。

## 文档核对与问题分级

模板 README 关于 Python 标准库示例、无需 Node 工程或 npm 安装、HTTP 和日志动作覆盖完整冒烟链路、原仓库运行命令、严格模式遇到跳过退出 1、清单缺少 `network` 时跳过的说法，与本次结果相符。运行代码的字面扫描和仓库外运行也支持模板关于运行时可移植性的说法。Node 版本下限和移动端适配器没有跨版本或设备实测。

### P0

无。模板主链路和新增负向场景均可运行，未观察到跳过误报为通过。

### P1

| 问题 | 文档怎么写 | 实际是什么 | 复现步骤 |
| --- | --- | --- | --- |
| 复制命令会带入旧证据 | 工具 README §10 要求排除 `evidence/`，紧接着给出 `cp -R tools/verify /path/to/other-project/tools/verify` | `cp -R` 递归复制整个目录，没有排除项。已有证据时，照抄会将历史运行产物带入新项目。本次使用带排除规则的 `rsync` | 在两个临时目录中先创建源 `tools/verify/evidence/marker`，按 README 的 `cp -R` 复制并检查目标中的 `evidence/marker` |
| 退出码速查表容易被误作验收条件 | 工具 README §1 写 `0` 为「全部步骤通过」。模板 README 只说明严格模式，AGENT.md §2 又把 `1` 简写成「至少一个步骤失败」 | 第 3 项普通模式跳过全部步骤仍退出 0，严格模式可在失败步骤为 0 时退出 1。工具 README §0、§11 和 AGENT.md §9 对这两种情况有正确说明 | 按第 3 项停服，分别执行普通和严格命令，对照退出码、`ok`、`status`、`summary.skipped` |

### P2

| 问题 | 文档怎么写 | 实际是什么 | 复现步骤 |
| --- | --- | --- | --- |
| 整目录扫描未达零命中 | 模板 README §「它验证了什么」说工具核心没有 FoodMap 路径、端口、容器名。工具 README §10 建议整体复制，未区分随附的历史示例 | 运行代码范围无命中，排除证据后的整个副本仍在 `examples/logout.yml:6` 命中 `:8080`。模板自己的 19100 冒烟场景不依赖该示例 | 按第 4 项复制后，对整个副本及仅运行代码分别执行同一个 `grep` |
| Python 检查定义了但没有执行 | 模板 README 的结构表称 `deps.json` 检查「服务是否在跑、python3 是否存在」 | 冒烟场景只需要 `network`，即使清单中的 `python` 检查注定失败，在线严格运行也只有 `network` 进入 `preflight`。Python 是启动示例服务的先决条件 | 用 `jq '.python.checks[0].argv=["/nonexistent/verify-python-probe"]' deps.json > deps-python-fails.json`，保持服务在线，按第 8 项严格运行并查看 `preflight` |
| 外部目录布局、路径与前提缺一处可照抄说明 | 模板 README 给了原仓库启动命令，迁移部分只列需要改的配置、变量和断言，未列 Node 运行时，也未说明日志相对路径 | 外部工程要按实际 CLI 位置调用。模板冒烟把 `logRoot: ../logs` 相对 `scenarios/smoke.yml` 解析，服务运行会生成相邻 `logs/access.log`。示例服务只用 Python 标准库，验证 CLI 仍需 Node.js | 按第 4 项把三个模板文件放在临时工程根目录，运行 `X`，检查 `scenarios/smoke.yml` 与生成的 `logs/access.log` |
| 套件 JSON 的证据路径层级不清 | 工具 README §6 写证据路径在 `result.evidence`，未区分单场景与目录模式 | 第 6 项的目录模式 stdout 没有顶层 `evidence`。场景证据路径在 `scenarios[i].evidence`，套件证据在指定目录中，可按本次 runId 查找 | 用 `X --json` 保存 stdout，以 `jq 'has("evidence"), .scenarios[0].evidence'` 核对 |
| 移动端指引混写 | 模板 README 让 iOS/Android 一起参考仓库 `harness/scenarios/` 的 `xcode` / `app` 用法 | 按工具 README §4.6 和 XCUITest 说明，`xcode` 面向 iOS。Android 参考 `app` 的 adb 用法。外部副本也未带原仓库的 `harness/scenarios/` | 对照复制后的目录树、工具 README §4.6 和 §0 的 XCUITest 条目 |

## 放行条件与收尾

**本轮可按已验证的方式接入**。复制时排除 `evidence/` 和运行日志，按外部目录调用 CLI，确认 Node.js 20+、Python 3 和回环端口可用。验收使用严格模式，同时检查 `ok=true`、`summary.failed=0`、`summary.skipped=0`。

发布为可照抄的跨项目模板前，应修正 P1 的复制命令与退出码说明，提供一条包含外部布局和运行前提的完整命令。如果整目录零命中仍是放行门槛，需处理旧示例的 `:8080` 命中并用原范围复测。

本轮仅新增本报告。未执行 FoodMap 业务 harness，原因是验证对象为独立复用模板。未改工具、业务场景、业务代码或台账，也未提交。原工作区日志已恢复，测试服务已停止。临时工程和本次证据保留在 `/tmp/xproj.UJPN24`。
