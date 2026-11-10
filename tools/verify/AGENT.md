# AGENT.md：给 agent 的 30 秒上手

本文件是 `tools/verify/` 的使用契约。**只读这一页就能跑起来**；细节看 `README.md`。

---

## 1. 一条命令

```bash
# 单个场景
node tools/verify/cli.mjs run harness/scenarios/b1-logout.yml

# 整个目录（推荐给 agent）
node tools/verify/cli.mjs run harness/scenarios/ --json --evidence-dir /tmp/verify
```

`--json` 时 **stdout 只有 JSON**，进度和错误都在 stderr。

## 2. 退出码只有三种

| 码 | 含义 | 你该做什么 |
| --- | --- | --- |
| `0` | 无运行失败（依赖感知模式可能部分跳过） | 检查 `ok`、`status` 和 `summary.skipped`，不能只看退出码 |
| `1` | 有失败步骤，或严格模式（`VERIFY_REQUIRE_FULL=1`/`--require-full`）下有跳过 | 看 `scenarios[i].steps[j].error` / `.hint` / `.checks`，判断是代码 bug 还是期望写错 |
| `2` | 场景/配置/用法错误 | **不要改业务代码**，先修 scenario 或命令 |

## 3. 读结果

```jsonc
{
  "ok": false,
  "exitCode": 1,
  "summary": { "scenarios": 3, "passed": 2, "failed": 1, "steps": 24 },
  "scenarios": [
    { "name": "B1 退出登录链路", "ok": false,
      "steps": [
        { "phase": "step", "index": 4, "action": "redis",
          "expected": { "equals": 3 }, "actual": { "value": 2 },
          "ok": false, "attempts": 1,
          "checks": [{ "name": "equals", "expected": 3, "actual": 2, "ok": false }],
          "error": "", "hint": "" }
      ] }
  ]
}
```

- `phase`：`setup` / `step` / `teardown`。
- `attempts` > 1 说明这一步重试过。
- `error` 是适配器没跑起来的错误（连不上、命令不存在）；`checks` 是断言不符。
- 失败步骤的 `hint` 通常直接告诉你原因和解法。

## 4. 环境前提

| 需要 | 检查 |
| --- | --- |
| Node ≥ 20 | `node -v` |
| 被测服务已启动 | 场景的 `setup` 里应有健康检查；起不来会在 setup 阶段失败 |
| `psql` / `redis-cli` | 只有用到 SQL / Redis 步骤才需要 |
| Xcode + 模拟器 | `xcode` / `app` 步骤需要；`xcodebuild` 要能写 `~/Library/Developer` |
| 沙箱权限 | 跑 `xcodebuild`/`simctl` 的 agent 需要放开文件沙箱（本仓库为 `danger-full-access`） |

## 5. 常见失败对照

| 现象 | 判断 |
| --- | --- |
| `setup 1/1 … FAIL` 且 hint 提到连接 | 服务没起或端口不对，不是业务 bug |
| `status: expected 200 / actual 401` | 看是否漏了 `Authorization` 或 Token 已失效 |
| `redis count … expected 3 / actual 2` | 写路径没生效，或上一次运行留下脏数据 |
| `xcode test … FAIL` 且 `failureMessage` 是 `XCTAssertTrue failed` | 是 UI 用例断言失败，看 XCUITest 报错 |
| `exitCode: expected 0 / actual 127` | 命令或工具不在 PATH |
| 退出码 `2` | scenario 写错（未知字段、变量未定义、断言名拼错） |

## 6. 新增/修改场景

```text
1. 抄 harness/scenarios/ 里最像的一个
2. 只改 config/vars 和步骤，不要改 tools/verify/core 与 adapters
3. 先 --dry-run 校验语法，再 --only 跑这一条
4. 故意改坏一个断言，确认失败信息能看懂
5. 用 --json 结果写台账（harness/scripts/verify-record.sh）
```

- 断言只写**可观测事实**：状态码、业务码、Redis key 数量、DB 字段值、探针日志片段。
- 不要写“界面看起来正常”。
- 不要在 scenario 里写明文密码 / Token / 完整手机号；用 `${env:NAME}` 或 `--var`。
- 账号名带 `runId`，避免脏数据：`account: "zxq_${runId}"`。

## 7. 支持的动作与断言（速查）

| 动作 | 写法 |
| --- | --- |
| `http` | `POST /api/auth/login` + `json` / `headers` / `body` |
| `sql` | `select ...`（需要 `config.databaseUrl` 或 `config.psql`） |
| `redis` | `count "pattern"` 或任意 redis-cli 命令 |
| `command` | 任意 shell 命令 |
| `logfile` | 文件或 `*` 通配 |
| `app` | `launch` / `terminate` / `readlog <udid> <bundleId> [path]` |
| `xcode` | `build` / `test` / `install` + xcodebuild 参数 |
| `sleep` | 毫秒数 |

| 断言 | 含义 |
| --- | --- |
| `status` | HTTP 状态码 |
| `exitCode` | 命令/设备命令退出码 |
| `equals` | 标量/结构相等（`xcode test` 时是失败用例数） |
| `gte` / `lte` | 数值范围 |
| `contains` / `matches` / `stdoutContains` | 文本包含 / 正则 / 标准输出包含 |
| `json` | 点分路径子集匹配 |

每个步骤都可以加 `retry: { max, intervalMs }`；场景可以加 `setup:` 和 `teardown:` 两个步骤列表。

## 8. 证据

默认写在 `tools/verify/evidence/`（不提交仓库）。文件名含 runId，不会互相覆盖；
落盘前会按内置规则脱敏 Bearer / JWT / 手机号，可在 `config.redact` 追加正则。

## 9. 可选依赖感知套件

```sh
node tools/verify/cli.mjs run <场景目录> --dependencies <预检清单.json> --json
# 验收禁止跳过（或设置 VERIFY_REQUIRE_FULL=1）
node tools/verify/cli.mjs run <场景目录> --dependencies <预检清单.json> --require-full --json
```

- `requires: [sql, redis]` 显式补充 shell 隐藏的依赖；http/sql/redis/app/xcode 仍补充 network/sql/redis/device 依赖。无预检配置时不自动跳过。
- 缺依赖为 `skipped:true, skipReason:...`，从未执行 setup/steps/teardown，不算 passed。
- 部分执行：`status=partial, ok=false`。普通模式退出 0；严格模式退出 1。真实失败仍返回 1，配置错误 2。
- `--dry-run` 不要求真实环境变量、不执行预检；缺失环境变量仍在真实执行前校验。
- 清单格式和预检类型见 README 的依赖感知章节。预检命令来自受信项目维护者，仅使用只读检查；不读取/打印密码值。
