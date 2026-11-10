# Verify 跨项目复用模板复测

2026-11-10（Asia/Shanghai）。结论为 **可用**。新版复制命令能排除源项目的运行产物，空项目中的副本可以直接执行，副本整目录扫描零命中。上一轮两项 P1 均关闭，剩余两项为 P2 文档问题。

## 基线与环境

- `git log -1 --oneline` 实际输出 `42aaa44 文档：按 Codex 复测结论修正模板复制与退出码说明`。对照基线为 `afc6e44`。开始时 Git 工作区干净，CLI、核心、适配器和 Python 示例服务在两个提交间一致。
- macOS 26.6.2（25G83），arm64，Node.js v24.14.0，Python 3.11.4，openrsync（协议 29，rsync 2.6.9 兼容）。复测只覆盖该环境。
- 使用了额外权限。仓库内和仓库外的在线测试通过获准的提权命令绑定本机回环地址 `127.0.0.1:19100`，复制、预演、证据解析均在默认权限下完成。
- 空项目为 `/tmp/xproj.retest.vWCaaK/empty-project`。证据、原始输出和命令记录在其父目录。仓库内的证据定向到临时目录，示例服务停服后恢复源目录原有的忽略日志。

## 命令与结果

`P/F/S` 是场景级 passed / failed / skipped，`—` 表示该命令没有执行场景。耗时为子进程墙钟时间。各命令的实际 CWD、参数、退出码和耗时见 `/tmp/xproj.retest.vWCaaK/commands.jsonl`，stdout/stderr 分别保存在 `captures/*.out`、`captures/*.err`。

仓库内在 `tools/verify/examples/minimal-project` 启动 `python3 server.py --port 19100`，仓库外在 `empty-project` 启动同一命令。两次均在健康检查返回 200 后运行在线场景，发送 SIGTERM 并等待服务退出后再运行离线场景。

```sh
# R，在仓库根目录
node tools/verify/cli.mjs run tools/verify/examples/minimal-project/scenarios \
  --dependencies tools/verify/examples/minimal-project/deps.json \
  --evidence-dir /tmp/xproj.retest.vWCaaK/ev

# C，在仓库根目录。仅替换文档中的目标路径
rsync -a --exclude='evidence/' --exclude='examples/minimal-project/logs/*.log' \
  tools/verify/ /tmp/xproj.retest.vWCaaK/empty-project/tools/verify/
cp -R tools/verify/examples/minimal-project/{scenarios,deps.json,server.py} \
  /tmp/xproj.retest.vWCaaK/empty-project/

# G，在外部空项目运行，生成任何证据之前执行
grep -rn 'foodmap\|FOODMAP_\|:8080' tools/verify

# X，在外部空项目运行
node tools/verify/cli.mjs run scenarios --dependencies deps.json --evidence-dir ../ev
```

| 项 | 命令 | 退出码 | 秒 | P/F/S | 结果 |
| --- | --- | ---: | ---: | --- | --- |
| 1 | `R` | 0 | 0.2211 | 1/0/0 | 1 个 setup、4 个步骤，与 smoke 一致 |
| 2 | `VERIFY_REQUIRE_FULL=1 R` | 0 | 0.0903 | 1/0/0 | 5 步，零跳过 |
| 3 | 停服后 `R` | 0 | 0.0494 | 0/0/1 | 部分执行，0 步 |
| 3 | 停服后 `VERIFY_REQUIRE_FULL=1 R` | 1 | 0.0471 | 0/0/1 | 部分执行，0 步 |
| 3 | 停服后 `R --json` | 0 | 0.0452 | 0/0/1 | `ok:false`、`status:"partial"` |
| 3 | 停服后 `VERIFY_REQUIRE_FULL=1 R --json` | 1 | 0.0476 | 0/0/1 | `ok:false`、`status:"partial"` |
| 4 | `C` 的 `rsync` | 0 | 0.0174 | — | 目标初始只有项目根目录，成功创建 `tools/verify/` |
| 4 | `C` 的 `cp` | 0 | 0.0044 | — | 场景、清单、示例服务到达目标根目录 |
| 4 | `G` | 1 | 0.0106 | — | 整个干净副本零命中，1 是 grep 的无匹配返回值 |
| 4 | 仓库 `git grep -n -e foodmap -e FOODMAP_ -e :8080 -- tools/verify` | 1 | 0.0075 | — | 跟踪文件零命中 |
| 4 | 仓库 `grep -rnl`，模式同 `G` | 0 | 0.0076 | — | 只在既有忽略目录 `evidence/` 命中，记录文件名 |
| 4 | `node tools/verify/cli.mjs run scenarios --dependencies deps.json --require-full` | 0 | 0.1018 | 1/0/0 | 在外部空项目独立运行，5 步 |
| 5 | `node tools/verify/cli.mjs run scenarios --dependencies deps.json --only bad-request --dry-run` | 0 | 0.1215 | — | 停服下语法校验通过，1 个步骤 |
| 5 | `X --only bad-request` | 0 | 0.0745 | 1/0/0 | HTTP 400，响应 JSON 符合预期 |
| 5 | 改为期望 500 后 `X --only bad-request --json` | 1 | 0.0674 | 0/1/0 | 状态断言失败，响应 JSON 断言通过 |
| 5 | 恢复期望 400 后 `X --only bad-request` | 0 | 0.0634 | 1/0/0 | 1 步通过 |
| 6 | `X --json` | 0 | 0.0884 | 2/0/0 | 6 步，stdout 可整段解析，进度在 stderr |
| 6 | 停服后 `X --json --junit /tmp/xproj.retest.vWCaaK/report.xml` | 0 | 0.0596 | 0/0/2 | 两个 JUnit 用例均含带原因的 `<skipped>` |
| 6 | 停服后 `X --require-full --json` | 1 | 0.0559 | 0/0/2 | 严格模式命令行选项同样禁止跳过 |
| 7 | `node tools/verify/cli.mjs run scenarios/bad-request.yml --evidence-dir ../ev --json` | 0 | 0.0605 | 1/0/0 | 单场景顶层 `evidence` 指向有效文件 |
| 7–8 | `python3 /tmp/xproj.retest.vWCaaK/check.py` | 0 | 0.0261 | — | 校验 JSON、JUnit、证据路径及预检内容 |

普通与严格模式在停服时都输出 `0 通过 / 0 失败 / 1 跳过`，原因是 `network: 示例服务未启动（先运行 python3 server.py）`，JSON 的 `steps` 为空。没有出现跳过计为通过。复制前确认仓库实际存在历史证据与示例访问日志，复制后、运行前确认目标没有这两种产物。修复后的旧示例端口为 9000。

## 新场景与证据

依据工具 README §4.1 和 §5 重新在临时项目创建 `scenarios/bad-request.yml`。从重读相关写法到第一次运行通过为 **35.15 秒**，到恢复 400 并再次通过为 **35.36 秒**。这段墙钟时间包含两地启动、命令调度和其他复测步骤，且已熟悉上一轮流程。

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

服务关闭时先预演，再在线执行。把 400 改成 500 后，stderr 在步骤 `[1/1] http POST /items` 下显示 `status: expected 500 / actual 400`。恢复 400 后通过。YAML 一次校验成功，无需看源码，唯一需要补的是根据文档的外部文件布局拼出执行命令。

失败场景的 `ev/2026-11-10T10-42-1c99f67f-Reject an item with an empty name.{txt,json}` 可定位步骤 1，分别显示期望 500、实际 400 和实际响应 `{"error":"name_required"}`。套件没有顶层 `evidence`，`scenarios[i].evidence` 的文件都存在，套件证据可按 runId 找到。跳过理由也出现在场景证据和 JUnit 中。脱敏实现及 CLI 调用与上一轮一致，样本没有密钥，本轮沿用上一轮对机制存在的核查。

## 文档核对与问题分级

**P0、P1 均无。** 上一轮 P1 的复制命令与退出码表已经修复，且实测结果与修订说明一致。旧示例端口字面命中已消除，证据路径的层级说明经单场景和套件分别验证。模板新增运行时、端口和日志相对路径说明与实测一致。移动端指引现在区分 iOS 与 Android，并说明原仓库场景目录不会复制，本轮未执行移动端场景。

| 等级 | 文档怎么写 | 实际是什么 | 复现步骤 |
| --- | --- | --- | --- |
| P2 | 模板 README「结构」一节仍称 `deps.json` 检查「服务是否在跑、python3 是否存在」 | 清单已删除 `python`，在线 JSON 的 `preflight` 只有 `network`。Python 3 是启动模板服务的前提，且已在后文正确列出 | 查看复制后的 `deps.json` 并运行 `X --json`，读取 `.preflight[].name` |
| P2 | 模板 README 的「复制到新项目（完整命令）」给出了两条复制命令。「怎么用」的验证命令指向原仓库中的示例目录 | 按复制命令生成的项目根目录只有 `scenarios/`、`deps.json`、`server.py` 和工具目录。目标中的验证命令需要采用新布局 | 执行 `C`，在目标根目录启动服务。对照「怎么用」的路径和第 4 项已通过的 `node tools/verify/cli.mjs run scenarios --dependencies deps.json --require-full` |

## 放行与收尾

使用新版 `rsync` 规则复制，在目标项目根目录按实际布局运行，严格模式下确认 `ok=true`、`summary.failed=0`、`summary.skipped=0`。两个 P2 不阻断本次放行。

本轮仅新增本报告。源访问日志逐字节比对一致，源证据文件路径和 SHA-256 清单一致，服务已停止。未运行 FoodMap 业务 harness，未修改工具、业务代码、场景或台账，未提交。临时工程、证据和 JUnit 保留在 `/tmp/xproj.retest.vWCaaK`。
