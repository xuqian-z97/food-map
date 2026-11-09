# 本地依赖感知验证（方案 D）实施与验证

## 范围

新增本地统一入口，按依赖就绪情况执行现有场景；不重裁 B1 验收，不修改业务实现，不删除历史数据。

原失败原因：macOS Bash 3.2 在 `set -u` 下展开空 `ARGS` 数组报 unbound variable。包装改用 POSIX sh + `exec node ... "$@"`，预检由通用工具按受信项目 JSON 清单执行，不再依赖 Bash 数组。

## 实现

- `harness/scripts/verify.sh` 与 `harness/scripts/run-all.sh` 统一入口。
- `requires` 补充 command 隐藏的数据库/Redis/日志/密码等依赖；动作推断只补充通用依赖，不解析任意 shell。
- `harness/verify-dependencies.json`：认证/用户网关路由、数据库只读查询、Redis PING、指定模拟器/已安装 App、Xcode、日志可读、环境变量非空。
- 缺依赖明确 SKIP，不执行该场景任何阶段。summary 分别统计通过/失败/跳过，部分执行为 `ok=false,status=partial`。
- 普通模式无真实失败可退出 0；严格模式 `VERIFY_REQUIRE_FULL=1` 或 `--require-full` 时任何跳过退出 1。
- 预检及跳过原因保存到 JSON/文本证据，JUnit 使用 `<skipped>`。配置错误不静默跳过。
- `--dry-run` 不预检、不访问服务、不要求真实密码；真实执行仍校验运行时变量。
- Xcode 失败进程（如退出码65）或没有执行任何可判定用例不能再显示为通过。

## 实测

| 验证 | 结果 |
| --- | --- |
| `node --test tools/verify/test/ harness/test/verify-entry.test.mjs` | 42/42 通过，其中工具40项、项目入口2项 |
| macOS `/bin/bash` 与 `/bin/sh` 包装参数测试 | 空参数及含空格参数均通过 |
| 无 FM_TEST_PW 的静态校验 | 五场景通过，不要求密码 |
| 真实环境普通模式、未设置 FM_TEST_PW | 3通过/0失败/2跳过，`ok=false,status=partial,exitCode=0` |
| 相同条件严格模式 | 3通过/0失败/2跳过，退出码1，保留原因 |
| 全依赖就绪的严格套件 | 5/5通过，80步骤，0跳过；耗时44.7秒 |
| 集成 `run-all.sh` 严格模式 | 所有基线检查及5/5场景通过，套件37.6秒 |
| fixture缺全部依赖/真实断言失败/非法配置 | 分别准确记录全跳过/退出1/退出2；没有假绿 |
| fixture Xcode退出65或零执行 | 明确失败，不再以0 failed判绿 |

实际 XCUITest 首次在受限环境报 `Pseudo Terminal Setup Error / Operation not permitted`，严格套件和统一入口都返回1；获准扩大本次命令权限后相同命令通过。预检通过不保证测试执行阶段无权限限制，运行失败不能被预检结果覆盖。

## 证据

- 单测输出：`/tmp/verify-d-unit.txt`
- 严格全量 JSON：`/tmp/verify-d-strict.json`（runId `suite-6585c300-ad29-4130-bdc7-9ceaff7b7ef5`）
- 部分执行与严格缺依赖：`/tmp/verify-d-partial.json`、`/tmp/verify-d-required.json`
- 统一入口输出：`/tmp/verify-d-harness.txt`
- 统一入口场景证据：`/tmp/verify-evidence/2026-11-09T12-11-suite-08c9ea5f-7be2-411a-81ed-5cf2d60576d1-harness_scenarios.json`

临时目录证据不作为永久归档；本报告保存关键命令、结果及已知边界，不包含密码或 Token。

## 已知边界

- 只读预检为瞬时快照，不保证测试时服务不掉线或伪终端权限可用。发生实际执行错误必须按失败报告。
- 原有接口/双写场景含固定测试凭据且没有完整 teardown；本轮不扩展业务场景或删除历史数据。这些问题不因依赖感知功能完成而消失。
- 模拟器 UDID、App安装与日志输出配置需与本机一致；不自动启动/安装/清库。
- 普通模式退出0不等于业务验收完成；必须检查 skipped=0、ok=true，并按 AGENTS.md 记录真实测试证据。
