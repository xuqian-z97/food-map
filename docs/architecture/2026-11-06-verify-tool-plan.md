# 通用验证工具（tools/verify）完成计划

- 版本：2026-11-06
- 现状基线：`ea62507 feat: add reusable verify tool and first FoodMap scenarios`（v0.1 已落地）
- 目标读者：主代理、后续接手 `tools/verify/` 的子代理、使用本工具做联调的任何 agent（Codex 等）

---

## 1. 目标与“做完了”的定义

### 1.1 一句话目标

让任意一个 agent（不限于本会话）在**不改工具核心代码**的前提下，用一份 scenario 文件完成
「起服务 → 驱动前端 → 断言后端 → 留证据 → 给结论」的全链路验证，并且结论可被机器读取。

### 1.2 三个使用场景（按优先级）

| 场景 | 谁用 | 期望命令 |
| --- | --- | --- |
| 后端契约回归 | 后端子代理 | `node tools/verify/cli.mjs run harness/scenarios/b1-logout.yml` |
| iOS 交互 → 后端链路 | 前端/QA 子代理 | `node tools/verify/cli.mjs run harness/scenarios/b1-ui-login.yml` |
| 一次跑全套 | 主代理、CI、Codex | `node tools/verify/cli.mjs run harness/scenarios/ --json` |

### 1.3 验收定义（Definition of Done）

全部满足才算“工具完成”：

1. `node --test tools/verify/test/` 全绿，用例数 ≥ 25，覆盖 retry、teardown、suite、脱敏、junit。
2. `run <目录>` 一次跑完全部 scenario，输出**一份**聚合 JSON，退出码能区分：全过 `0`、有失败 `1`、用法/配置错误 `2`。
3. 服务未启动时，**3 秒内**给出可读失败（说明连不上谁、看哪个文件），不打印堆栈、不挂死。
4. 把整个 `tools/verify/` 复制到 `/tmp/newproj` 后，`node --test` 与示例 scenario 仍可运行（零项目耦合）。
5. FoodMap 至少 1 条 **真实 UI 交互** 链路（点登录按钮 → 地图页出现 → 数据库有登录记录）可一键复现。
6. `README.md` 含“给 agent 的 30 秒上手”，`AGENTS.md` §6.3 与实现一致。

---

## 2. 现状盘点（v0.1 已完成）

| 能力 | 位置 | 说明 |
| --- | --- | --- |
| 受限 YAML 解析 | `core/yaml.mjs` | 支持嵌套映射、块列表、流式 `{}`/`[]` |
| 场景加载/校验/变量 | `core/scenario.mjs` | 顶层字段白名单、动作唯一性、`${var}` 递归替换、静态未定义变量检测 |
| 顺序执行/捕获 | `core/runner.mjs` | `capture` → 运行时变量；失败继续跑完剩余步骤 |
| 断言 8 种 | `core/assert.mjs` | status/exitCode/equals/contains/matches/stdoutContains/gte/lte/json |
| 证据 | `core/evidence.mjs` | `<时间>-<name>.txt` + `.json` |
| HTTP/SQL/Redis/命令/日志/设备 6 个适配器 | `adapters/*.mjs` | 全部零依赖，走内置 fetch、psql、redis-cli、sh、simctl/adb |
| CLI | `cli.mjs` | `run`、`--var`、`--config`、`--evidence-dir`、`--json` |
| 示例与落地场景 | `tools/verify/examples/`、`harness/scenarios/` | 2 个示例 + 4 个 FoodMap 场景 |
| 单测 | `test/` | 8 个用例，本地 loopback + 临时文件，不依赖 Docker |

结论：**骨架和项目解耦已经成立**（复制即可用），缺的是“让别的 agent 敢用、能一次跑对”的稳定性与原语。

---

## 3. 差距清单（按“别的 agent 能不能直接用”排序）

### P0：不解决，其他 agent 用一次就会放弃

| # | 差距 | 后果 | 方案 |
| --- | --- | --- | --- |
| P0-1 | 没有重试/等待原语 | 服务刚起、模拟器刚启动时必然 flaky，agent 会误判“代码有 bug” | 步骤级 `retry: {max, intervalMs}` + 顶层 `setup` 里的健康等待 |
| P0-2 | 无法一次跑多个场景 | agent 要自己写 shell 循环、自己聚合结果 | `run <目录>` + `--suite` 聚合报告、单一退出码 |
| P0-3 | 证据同分钟覆盖、含密文 | 复跑丢证据；Token/密码被写进 `.txt` 提交 | 文件名加 `runId`；`config.redact` 正则脱敏后再落盘 |
| P0-4 | 没有 setup/teardown 语义 | 造数据、清数据只能塞进 steps，失败后不清理 | 顶层 `setup`/`teardown`，`teardown` 失败也执行 |
| P0-5 | 变量来源单一 | 密码/端口只能写进 scenario，agents 会硬编码 | `${env:NAME}`、`${file:path}`、项目级 `.verify/config.yml` 自动发现 |
| P0-6 | 没有 agent 上手契约 | 每个 agent 都要重读 500 行 README | `tools/verify/AGENT.md`：调用方式、退出码、JSON 结构、权限要求、常见失败对照表 |

### P1：决定“能不能测前端交互到后端”

| # | 差距 | 后果 | 方案 |
| --- | --- | --- | --- |
| P1-1 | iOS UI 只能靠项目探针 | 探针覆盖不到的交互（点击、输入、弹窗）测不了 | 新增可选 `xcode` 适配器：`xcodebuild test` + `xcresulttool` 解析 |
| P1-2 | 工具不知道如何调 XCUITest | agent 得自己拼 `xcodebuild` 命令 | `xcode: test -scheme FoodMapApp -only-testing:FoodMapAppUITests/LoginFlowTests` |
| P1-3 | FoodMap 没有 UI 测试 target | `xcodebuild test` 跑不起来（当前 `Schemes:` 为空） | 新增 `FoodMapAppUITests` target + shared scheme + `accessibilityIdentifier` |
| P1-4 | 文档与实现冲突 | `AGENTS.md` §6.3 写“UI 只能靠探针”，与 P1-1 矛盾 | 改为“两层：探针（无 UITest 项目）/ XCUITest（有 UITest 项目）” |

### P2：体验与长期维护

| # | 差距 | 方案 |
| --- | --- | --- |
| P2-1 | 断言种类少 | 增加 `notContains`、`count`、`gt`/`lt`、`header`、`jsonLength` |
| P2-2 | 无法过滤/预演 | `--only <name>`、`--tag`、`--dry-run`、`--list` |
| P2-3 | 没有 CI 报告格式 | `--junit <file>`，字段对齐 JUnit XML |
| P2-4 | 没有接入脚手架 | `verify init` 生成 `.verify/config.yml` 与示例 scenario |
| P2-5 | Android 未实测 | 用 adb 跑一条冒烟，写进 README |
| P2-6 | 台账未打通 | `harness/scripts/verify-record.sh`：跑场景 → 写 `status test record` |

---

## 4. 目标形态

### 4.1 分层

```text
tools/verify/                 # 与项目无关，可整体复制
├── AGENT.md                  # 给 agent 的契约（新增）
├── cli.mjs
├── core/
│   ├── yaml.mjs  scenario.mjs  runner.mjs
│   ├── assert.mjs  evidence.mjs
│   ├── retry.mjs             # 新增：重试/等待
│   ├── suite.mjs             # 新增：目录聚合
│   └── redact.mjs            # 新增：证据脱敏
├── adapters/
│   ├── http.mjs sql.mjs redis.mjs command.mjs logfile.mjs app.mjs
│   └── xcode.mjs             # 新增：build / test / install / xcresult
├── examples/  test/
└── evidence/                 # 默认输出（已 gitignore 建议）
```

```text
harness/                      # FoodMap 专属
├── scenarios/                # *.yml（已有 4 个，新增 UI 场景）
├── verify.config.yml         # 端口、容器名、账号模板
└── scripts/verify-record.sh  # 场景 → status 台账
```

### 4.2 scenario 语法 v2（增量，向后兼容）

```yaml
name: B1 UI 登录链路
config:
  baseUrl: http://127.0.0.1:8080
  redact:                      # 新增：证据落盘前替换
    - "(?i)bearer\\s+[A-Za-z0-9._-]+"
    - "eyJ[A-Za-z0-9._-]+"
  db: { url: "postgresql://foodmap:${env:FM_PG_PASS}@127.0.0.1:5432/foodmap_auth_db" }
vars:
  account: "zxq_ui_${runId}"   # 已支持
setup:                         # 新增：前置，失败即终止
  - http: GET /internal/auth/health
    retry: { max: 20, intervalMs: 500 }     # 新增：重试
  - http: POST /api/auth/register
    json: { accountName: "${account}", password: "${env:FM_TEST_PW}" }
    expect: { status: 200 }
steps:
  - xcode: test -scheme FoodMapApp -only-testing:FoodMapAppUITests/LoginFlowTests/testLogin
    env: { GATEWAY: ${baseUrl}, ACC: ${account} }
    expect: { failed: 0 }
  - sql: "select count(*) from login_logs where account_id = ${userId}"
    expect: { gte: 1 }
teardown:                      # 新增：无论成败都执行
  - command: "docker exec fm-postgres psql -c \"delete from ... where account_name='${account}'\""
```

新增字段：`setup`、`teardown`、`retry`、`redact`、`env` 变量源。
新增动作：`xcode`（`build` / `test` / `install`）。

### 4.3 xcode 适配器设计

| 子动作 | 实现 | 断言字段 |
| --- | --- | --- |
| `xcode: test -scheme X -only-testing:Y` | `xcodebuild test -destination 'platform=iOS Simulator,id=<udid>' -derivedDataPath .build/dd -resultBundlePath <tmp>` | `failed`（失败用例数）、`passed`、`total`、`stdoutContains` |
| `xcode: build -scheme X` | `xcodebuild build`（同参数；缺省 `CODE_SIGN_IDENTITY="-"`） | `exitCode`、`stdoutContains` |
| `xcode: install -scheme X` | build 产物 → `simctl install` | `exitCode` |

结果解析：`xcrun xcresulttool get test-results summary --path <bundle>`，输出 JSON →
`actual = { total, passed, failed, skipped, tests: [{name, status, durationMs}] }`。
失败时把失败用例的 `failureMessage` 拼进 `hint`，让 agent 一眼看到是断言失败还是超时。

---

## 5. 里程碑

> 估算按“一个 agent 连续工作”计；每个里程碑独立可交付、可提交、可回滚。

### M0 契约先行（0.5 天）

**产出**
- `tools/verify/AGENT.md`：30 秒上手、退出码表、JSON 字段、权限要求、失败对照表（连不上 / 断言不符 / 超时 / 缺工具）
- `README.md` 增加「§0 给 agent 的最小用法」并把 §9 限制逐条标注“已解决/不解决/何时解决”

**验收**：一个从未见过本工具的 agent，只看 `AGENT.md` 能在 5 分钟内跑通 `examples/logout.yml`（用 mock 服务验证）。

### M1 稳定性原语（1 天）

**产出**
- `core/retry.mjs`：`retry: { max, intervalMs, on: [失败类型] }`，只对“步骤执行失败/断言不符”重试，不对场景语法错误重试
- `setup` / `teardown`：`setup` 失败 → 退出码 1 且跳过 steps；`teardown` 永远执行，其失败单独标记
- `core/redact.mjs`：`config.redact` 正则数组，作用于 stdout/stderr/日志正文/JSON 响应后再落证据
- 证据文件名加 `runId`：`2026-11-06T11-20-<runId>-<name>.json`
- 变量源：`${env:NAME}`、`${file:</path>}`

**验收**
- 单测覆盖：retry 成功/耗尽、teardown 异常仍执行、redact 命中/未命中、证据不覆盖
- 手测：把 health 步骤的 `retry.max` 调成 2 且服务未启动 → 3 秒内失败，提示含“连接被拒 + 检查服务是否启动”

### M2 套件与报告（1 天）

**产出**
- `core/suite.mjs`：`run <dir>` 递归收集 `*.yml`，按文件名排序执行，聚合 JSON：
  ```json
  { "suite": "harness/scenarios", "total": 4, "passed": 3, "failed": 1,
    "exitCode": 1, "scenarios": [{ "name": "...", "ok": false, "steps": [...] }] }
  ```
- `--junit <file>`：一个场景 = 一个 `<testsuite>`，一个 step = 一个 `<testcase>`
- `--only <name>` / `--tag <tag>` / `--dry-run` / `--list`
- 失败场景的 `.txt` 仍各自生成，聚合 JSON 里给相对路径

**验收**
- `run harness/scenarios/` 在 4 个场景下输出单一 JSON，`jq .total` 等于 4
- 故意让 1 个场景失败：退出码 1，JUnit 里对应 `<failure>`
- 语法错误场景：退出码 2，且不执行任何步骤

### M3 xcode 适配器（1.5 天）

**产出**
- `adapters/xcode.mjs`：`build` / `test` / `install`；默认 `CODE_SIGN_IDENTITY="-"`、固定 `-derivedDataPath`
- `xcresulttool` 解析 → `actual.{total,passed,failed,skipped,tests}`
- 失败用例信息进 `hint`；`xcodebuild` 日志落证据（截断到 400 行）
- 无 Xcode 环境时给明确错误（`xcode-select -p` 检查）

**验收**
- 用一个**已有 UITest 的最小工程**（不是 FoodMap，避免耦合）跑通：2 个用例 1 过 1 失败 → `expect: { failed: 1 }` 通过，退出码 0
- 同一场景去掉 `expect` → 退出码 1

### M4 FoodMap 落地（1.5 天）

**产出**
- `front/FoodMapApp`：新增 `FoodMapAppUITests` target + shared scheme（`xcodebuild -list` 能看到 scheme）
- 登录页控件加 `accessibilityIdentifier`：`login.identifier`、`login.password`、`login.submit`；地图页加 `map.home`
- `front/FoodMapAppUITests/LoginFlowTests.swift`：输入账号密码 → 点登录 → 断言 `map.home` 出现
- `harness/scenarios/b1-ui-login.yml`：setup 建账号 → xcode test → SQL 断言登录日志 → teardown 清理
- `harness/scripts/verify-record.sh`：跑场景并调用 `status test record`（`--covers` 由参数传入）

**验收**
- 一条命令跑通登录全链路，证据里同时有 XCTest 结果和 SQL 结果
- 停掉 gateway 后重跑：在 retry 耗尽后失败，`hint` 指向“网关未启动”
- `xcodebuild test` 与原有 `xcodebuild build` 互不影响（DerivedData 分开）

### M5 打磨与迁移（1 天）

**产出**
- 断言扩展：`notContains`、`count`、`gt`/`lt`、`header`、`jsonLength`
- `verify init`：生成 `.verify/config.yml` + 示例 scenario（供其他项目接入）
- Android 冒烟一条（模拟器/真机任选），结论写进 README
- `harness/README.md` 增加 verify 与 scenarios 的说明；`AGENTS.md` §6.3 改为两层 UI 策略
- 把现有 4 个场景里的硬编码账号密码改为 `${env:FM_TEST_PW}`（或 `.verify/config.yml`）

**验收**
- 复制到 `/tmp/otherproj` 后 `node --test` 全绿、`examples/logout.yml` 可跑（对着一个本地 mock server）
- `AGENTS.md` §6.3 与实现逐条对得上

---

## 6. 给其他 agent 的使用契约（Codex 等）

### 6.1 调用方式

```bash
# 单场景，人看
node tools/verify/cli.mjs run harness/scenarios/b1-logout.yml

# 单场景，agent 解析
node tools/verify/cli.mjs run harness/scenarios/b1-logout.yml --json --evidence-dir /tmp/verify

# 全套，退出码即结论
node tools/verify/cli.mjs run harness/scenarios/ --json > /tmp/verify-all.json
```

### 6.2 agent 必须知道的四件事

1. **退出码**：`0` 全过、`1` 有失败、`2` 用法/场景配置错误。**2 不要去修业务代码，先修 scenario。**
2. **stdout 只有 JSON**（`--json` 时），进度在 stderr。
3. **失败详情在 `result.scenarios[i].steps[j].error` 和 `hint`**，不要只看最后一行。
4. **权限**：`xcodebuild`/`simctl` 需要写 `~/Library/Developer`，在受限沙箱里必须放开（本环境为 `danger-full-access`）。

### 6.3 新增场景的流程（写进 skill）

```text
1. 读 harness/scenarios/ 里最相似的场景
2. 复制并改名 → 改 config/vars → 逐步 run，直到退出码 0
3. 让某个断言故意失败一次，确认失败信息可读
4. 再跑一遍确认 0
5. 用 --json 结果写 status 运行记录（harness/scripts/verify-record.sh）
```

---

## 7. 风险与对策

| 风险 | 影响 | 对策 |
| --- | --- | --- |
| 改 `project.pbxproj` 加 UITest target 出错 | 工程打不开 | 单独提交；改完立即 `xcodebuild -list` + `build` 验证；失败即 `git revert` |
| XCUITest 输入不稳定（中文、自动更正） | UI 用例随机失败 | `accessibilityIdentifier` 定位；关闭自动更正；输入前先 `tap()` |
| 模拟器/构建慢（40–60s） | agent 以为卡死 | 固定 `-derivedDataPath` 复用增量；README 写明预期耗时；步骤级超时 |
| 证据含密钥 | 泄漏 | `redact` 默认内置 Bearer/eyJ/手机号规则；证据目录默认 gitignore |
| 场景与产品文案耦合 | 改文案就红 | 断言日志/DB/接口码，不断言 UI 文案；UI 断言用 identifier |
| 工具被改造成“项目专属” | 失去复用价值 | CI 检查：`grep -r "foodmap\|8080\|FOODMAP_" tools/verify/` 必须为空（新增 `tools/verify/test/no-project-coupling.test.mjs`） |

---

## 8. 需要拍板的决策

| # | 决策 | 选项 | 建议 |
| --- | --- | --- | --- |
| D1 | 是否做 XCUITest 层（M3+M4） | 做 / 不做 | **做**。这是唯一能把“点了按钮”变成自动化证据的官方途径；代价是改一次 Xcode 工程 |
| D2 | 工具形态 | 仓内 `tools/verify/` / 独立 repo | **先仓内**，M5 完成后再抽 repo；现在抽会拖慢迭代 |
| D3 | 报告格式 | 仅 JSON / JSON + JUnit | **JSON + JUnit**，JUnit 给 CI 和不懂本工具的 agent 看 |
| D4 | 台账打通方式 | 工具内建 / harness 脚本 | **harness 脚本**，工具保持零项目耦合 |
| D5 | 场景里的测试账号密码 | 明文 / `${env:}` / config | **`${env:}` + 本地 `.env` 忽略提交**，仓库里不留明文 |

---

## 9. 任务清单（可直接分派给子代理）

| ID | 任务 | 写入范围 | 依赖 | 预估 |
| --- | --- | --- | --- | --- |
| V-01 | `AGENT.md` + README §0 | `tools/verify/AGENT.md`、`README.md` | 无 | 0.5d |
| V-02 | retry + setup/teardown | `core/retry.mjs`、`core/runner.mjs`、`core/scenario.mjs` | V-01 | 1d |
| V-03 | 脱敏 + 证据命名 | `core/redact.mjs`、`core/evidence.mjs` | V-01 | 0.5d |
| V-04 | suite + junit | `core/suite.mjs`、`cli.mjs` | V-02 | 1d |
| V-05 | xcode 适配器 | `adapters/xcode.mjs`、`test/xcode.test.mjs` | V-02 | 1.5d |
| V-06 | FoodMap UITest target + scheme | `front/FoodMapApp.xcodeproj`、`front/FoodMapAppUITests/` | 无 | 1d |
| V-07 | UI 场景 + 台账脚本 | `harness/scenarios/b1-ui-login.yml`、`harness/scripts/verify-record.sh` | V-05、V-06 | 0.5d |
| V-08 | 断言扩展 + init | `core/assert.mjs`、`cli.mjs` | V-04 | 0.5d |
| V-09 | 文档收口 | `AGENTS.md` §6.3、`harness/README.md`、`tools/verify/README.md` | 全部 | 0.5d |
| V-10 | 解耦检查测试 | `tools/verify/test/no-project-coupling.test.mjs` | 全部 | 0.5d |

总计约 **8 个 agent-日**；关键路径 V-01 → V-02 → V-05 → V-07。

---

## 10. 建议的执行顺序

```text
第 1 天：V-01 + V-02 + V-03        → 工具"敢用"（M0/M1）
第 2 天：V-04 + V-08               → 一次跑全套 + 报告（M2）
第 3 天：V-06（改 Xcode 工程）      → 先验证假设，风险最大放前面
第 4 天：V-05 + V-07               → UI 全链路打通（M3/M4）
第 5 天：V-09 + V-10 + 回归        → 文档收口与解耦守卫（M5）
```

先做 V-06 的原因：它是整个计划里**唯一没被验证过的技术假设**（XCUITest 能否在这套环境
稳定运行）。假设不成立，V-05 的设计要改；越早发现越好。
