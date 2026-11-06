# verify：零依赖通用验证工具

## 0. 给 agent 的 30 秒版

```bash
node tools/verify/cli.mjs run harness/scenarios/ --json --evidence-dir /tmp/verify
```

退出码 `0` 全过、`1` 有失败、`2` 场景/配置错误。只读这一条不够时看 `AGENT.md`。

2026-11 新增能力（schemaVersion 2）：

| 能力 | 用法 |
| --- | --- |
| 前置/清理 | 顶层 `setup:` / `teardown:`；setup 失败即停，teardown 永远执行 |
| 重试 | 任意步骤 `retry: { max: 3, intervalMs: 500 }` |
| 套件 | `run <目录>` 聚合执行，输出单一 JSON + 退出码 |
| JUnit | `--junit result.xml` |
| 预演 | `--dry-run`（只校验）、`--list`、`--only <关键字>` |
| 环境变量 | `${env:NAME}`，加载阶段校验是否存在 |
| 证据脱敏 | 内置 Bearer / JWT / 手机号规则，`config.redact` 追加 |
| XCUITest | `xcode: test -only-testing:Bundle/Case`，解析 `.xcresult` 得到 passed/failed |
| 台账 | `harness/scripts/verify-record.sh <场景> --scope=... --covers=F-001` |



`tools/verify/` 是一个可以整体复制的验证工具骨架。它把“HTTP 请求、SQL、Redis、
shell 命令、日志文件、iOS/Android 设备动作、等待”统一成可读的 scenario，
按顺序执行、生成证据，并用退出码给 CI/agent 一个明确结论。

- Node.js >= 20，纯 ESM（`.mjs`）。
- 不依赖任何 npm 包，不需要 `npm install` 或构建步骤。
- HTTP 使用内置 `fetch`，进程使用 `node:child_process`，文件使用 `node:fs`。
- 核心代码不包含任何具体项目的端口、账号、SQL、Redis key 或设备 UDID。
  这些内容全部放在 scenario、`--config`、`--var` 或环境变量里。

## 1. 快速开始

```bash
# 单元测试（只使用本地 loopback HTTP server 和临时文件）
node --test tools/verify/test/

# 运行示例；服务缺失时应该看到逐步失败和退出码 1，而不是堆栈
node tools/verify/cli.mjs run tools/verify/examples/logout.yml

# 用 --var 覆盖 scenario.vars，用 --config 覆盖 scenario.config
node tools/verify/cli.mjs run tools/verify/examples/ios-probe.yml \
  --var udid=AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE \
  --config /tmp/verify-local.yml \
  --evidence-dir /tmp/verify-evidence

# 只取结构化结果（进度输出到 stderr，stdout 是纯 JSON）
node tools/verify/cli.mjs run path/to/scenario.yml --json > result.json
```

退出码：

| 退出码 | 含义 |
| --- | --- |
| `0` | 全部步骤通过 |
| `1` | 至少一个步骤失败（断言不匹配或适配器执行失败） |
| `2` | scenario/配置/CLI 用法错误，或证据写入失败 |

默认证据目录是 **`tools/verify/evidence/`**（相对工具自身，不依赖调用时的工作目录）。
可以用 `--evidence-dir` 改到 `/tmp`、CI artifact 目录等位置。

## 2. 目录

```text
tools/verify/
├── README.md
├── cli.mjs
├── core/
│   ├── yaml.mjs       # 受限 YAML 子集（支持嵌套映射、块列表、流式 {} / []）
│   ├── scenario.mjs   # 加载、校验、config 合并、变量替换
│   ├── runner.mjs     # 顺序执行、capture、结果和退出码
│   ├── assert.mjs     # 断言实现
│   └── evidence.mjs   # .txt / .json 证据
├── adapters/
│   ├── http.mjs
│   ├── sql.mjs
│   ├── redis.mjs
│   ├── command.mjs
│   ├── logfile.mjs
│   └── app.mjs
├── examples/
│   ├── logout.yml
│   └── ios-probe.yml
└── test/
    ├── index.mjs      # 让 node --test test/ 在 Node 24 下可运行
    ├── package.json   # 仅为目录入口，不含依赖
    ├── runner.test.mjs
    └── scenario.test.mjs
```

## 3. Scenario 语法

```yaml
name: 退出登录链路
description: 可选说明
config:                      # 可选；--config 会深度覆盖这里的值
  baseUrl: http://127.0.0.1:8080
  redisPrefix: demo:auth
vars:                        # 可选；--var k=v 优先
  account: demo
  password: demo
steps:
  - http: POST /api/auth/login
    json: { accountName: ${account}, password: ${password} }
    headers: { X-Trace: ${runId}, Accept: application/json }
    expect:
      status: 200
      json: { code: OK }
    capture:
      access: data.accessToken
  - redis: count "token-denylist:v1:*"
    expect: { equals: 2 }
  - sql: "select token_status from refresh_tokens where id = 1"
    expect: { equals: REVOKED }
  - command: "echo hi"
    expect: { exitCode: 0, stdoutContains: hi }
  - logfile: "logs/probe.log"
    expect: { contains: ["logout.end"], matches: ["logout\\.end"] }
  - sleep: 200
  - app: launch ${udid} com.example.app
    env: { PROBE: 1 }
  - app: readlog ${udid} com.example.app Documents/probe.log
    expect: { contains: ["logout.end"] }
```

### 3.1 字段与校验

顶层只允许：`name`、`description`、`config`、`vars`、`steps`。
`steps` 必须是非空列表；每一步必须且只能有一个动作：

| 动作 | 值写法 |
| --- | --- |
| `http` | `"METHOD path"`，例如 `POST /api/auth/login` |
| `sql` | SQL 文本 |
| `redis` | `count "<pattern>"` 或普通 redis-cli 命令 |
| `command` | shell 命令文本 |
| `sleep` | 非负毫秒数 |
| `logfile` | 文件路径或简单 glob |
| `app` | `launch|terminate|readlog <udid> <bundleId> [logPath]` |

步骤可用的附加字段：

| 字段 | 适用动作 | 说明 |
| --- | --- | --- |
| `expect` | 全部 | 断言映射；可省略，表示只执行不断言 |
| `capture` | 全部 | `变量名: 点分路径`，把结果留给后续步骤 |
| `json` | `http` | 请求体对象；会自动 `JSON.stringify` 并补 `content-type: application/json` |
| `headers` | `http` | 请求头映射 |
| `body` | `http` | 原始字符串请求体；不能和 `json` 同用 |
| `env` | `app` | iOS `launch` 的环境变量，前缀映射为 `SIMCTL_CHILD_*` |

未知字段、未知断言、多个动作、空步骤列表都会在运行前报 `scenario 错误`（退出码 2），
避免拼写错误被静默忽略。

### 3.2 配置合并

优先级从低到高：

1. scenario 的 `config`
2. `--config` 文件（顶层键深度合并；数组和标量直接替换）
3. 对字符串生效的变量替换

`--config` 文件也使用同一套受限 YAML 解析器，路径相对当前工作目录，例如：

```yaml
baseUrl: http://127.0.0.1:8080
redis:
  host: 127.0.0.1
  port: 16379
  db: 3
```

同一个 `--config` 指定多次时只有最后一次生效。所有连接信息必须由 scenario 或
`--config` 提供；核心代码不会写死 localhost 端口、账号或数据库名。YAML 双引号里的
正则需要写成双反斜杠（`"probe\\.ready"`），单引号里可直接写 `'probe\.ready'`。

### 3.3 变量替换

`${name}` 会递归替换 steps 和 config 中的字符串值，包括 `headers`、`json`、
SQL、命令和期望值。变量来源优先级：

1. `--var name=value`（最高）
2. scenario 的 `vars`
3. 顶层 config 的标量字段（例如 `config.udid` 可直接用 `${udid}`）

`runId` 每次运行自动生成，形如 `run-20261105T080009Z-72221bca`，默认始终可用；
可以在 scenario.vars 或 `--var` 中显式覆盖（config 同名字段不会覆盖它）。

`capture` 产生运行时变量：

- HTTP 的 capture 路径相对**解析后的 JSON 响应体**，例如 `data.accessToken`。
- 其他适配器的 capture 路径相对**适配器 actual 对象**，例如 `value`、`stdout`、`content`。
- capture 变量在产生它的步骤之后才可用；如果引用一个尚未产生的 capture，
  该步骤会失败并指出变量名。
- 静态扫描能发现完全不存在的变量（例如拼错 `${acount}`），运行前直接报退出码 2。
  已声明但尚未产生的 capture 属于运行时依赖，按步骤失败处理（退出码 1）。

## 4. 适配器

### 4.1 `http`：内置 fetch

```yaml
- http: POST /api/items
  json: { name: demo }
  headers: { X-Trace: ${runId} }
  expect: { status: 201, json: { data.id: 1 } }
  capture: { itemId: data.id }
```

- `baseUrl` 与路径拼接；路径也可以是绝对 URL（此时不需要 `baseUrl`）。
- 非 2xx **不会**抛异常，作为 `actual.status` 参与 `status` 断言。
- `actual` 包含 `status`、`statusText`、`headers`、`body`、`json`、`value`、`content`。
- 网络错误、超时、URL 构造失败会给出 `expected: 步骤可执行` + 实际错误 + `hint`。
- 配置：`baseUrl`、`httpTimeoutMs`（默认 10000）。

### 4.2 `sql`：通过 psql

```yaml
- sql: "select token_status from refresh_tokens where id = 1"
  expect: { equals: REVOKED }
```

- 使用 `psql -X -q -A -t -v ON_ERROR_STOP=1`，返回 stdout 的 trim 结果作为标量。
- 必须显式配置连接，不会悄悄连接默认数据库：
  - `databaseUrl: postgresql://user:pass@host:5432/db`
  - 或 `psql: { host, port, user, database, passwordEnv }`
  - 或 `psql: { url: ... }`
- 密码建议用 `passwordEnv`；也支持 `psql.password`，但不建议把密码写进文件。
- 其他 `psql` 键：`command`（psql 可执行文件）、`args`（附加参数数组）。
- 配置：`databaseUrl`、`psql`、`sqlTimeoutMs`（默认 15000）。
- `actual` 包含 `exitCode`、`stdout`、`stderr`、`value`、`content`。

### 4.3 `redis`：通过 redis-cli

```yaml
- redis: count "token-denylist:v1:*"
  expect: { equals: 2 }
- redis: get session:1
  expect: { equals: expected-value }
```

- `count "<pattern>"` 使用 `redis-cli --scan --pattern`，统计匹配 key 的数量，返回数字。
- 如果配置了 `redisPrefix`，`count` 会自动把前缀补到 pattern 前；
  已经带完整前缀（`prefix:` 开头）则不重复补。普通 Redis 命令不会自动补前缀。
- 其他写法按 redis-cli 参数逐项执行，返回 stdout trim 结果。
- `SCAN` 是遍历式统计，适合验证 key 数量；大规模 Redis 上应控制 pattern 范围。
- 连接配置：
  - `redis: { host, port, db, passwordEnv }`
  - 或 `redisUrl: redis://...`
  - 可选键：`command`、`user`、`tls`、`password`、`passwordEnv`。
- 配置：`redis`、`redisUrl`、`redisPrefix`、`redisTimeoutMs`（默认 10000）。
- Redis 客户端的 `(error)` 输出或非零退出码会转成可读失败。

### 4.4 `command`：任意 shell 命令

```yaml
- command: "printf 'hello'"
  expect: { exitCode: 0, stdoutContains: hello }
```

- POSIX 下使用 `/bin/sh -c`，Windows 下使用 `cmd.exe /d /s /c`；也可用 `shell` 覆盖。
- 非零退出码是正常 `actual`，交给 `exitCode` 断言；启动失败或超时才是步骤错误。
- `actual` 包含 `exitCode`、`signal`、`stdout`、`stderr`、`value`、`content`、`cwd`。
- `cwd` 相对 scenario 文件所在目录；未配置时也使用 scenario 文件所在目录，和 `logfile` 的默认根目录保持一致。
- 配置：`commandTimeoutMs`（默认 30000）、`shell`、`cwd`。
- 命令文本来自受信的 scenario 作者，等价于直接执行 shell；不要把不可信输入拼进命令。

### 4.5 `logfile`：文件或 glob

```yaml
- logfile: "logs/probe.log"
  expect:
    contains: ["probe.ready"]
    matches: ["probe\\.ready"]
```

- 相对路径默认相对 scenario 文件目录；`logRoot` 可改为另一个根目录。
- 支持 `*`、`?`、`**` 简单 glob；不支持 brace、字符类等完整 glob 语法。
- 多个匹配文件按路径排序，UTF-8 读取后用换行拼接为 `content`；文件列表在 `files`。
- 找不到文件是步骤失败，信息包含 pattern 和实际解析根目录。
- `actual` 包含 `pattern`、`root`、`files`、`content`、`value`、`bytes`。
- 配置：`logRoot`。

### 4.6 `app`：iOS simctl / Android adb

```yaml
config:
  platform: ios                 # ios（默认）或 android
steps:
  - app: launch ${udid} com.example.app
    env: { PROBE: 1 }
  - sleep: 1000
  - app: readlog ${udid} com.example.app Documents/probe.log
    expect: { contains: ["probe.ready"] }
  - app: terminate ${udid} com.example.app
```

iOS：

- `launch`：`xcrun simctl launch <udid> <bundleId>`。
- `terminate`：`xcrun simctl terminate <udid> <bundleId>`。
- `readlog`：先 `xcrun simctl get_app_container <udid> <bundleId> data`，再读取
  data 容器内的相对路径（如 `Documents/probe.log`）；路径不能越出容器。
- `env` 只在 `launch` 生效，映射为 `SIMCTL_CHILD_<KEY>`。
- 配置：`platform`、`xcrunPath`（默认 `xcrun`）、`appTimeoutMs`。

Android：

- `launch`：`adb -s <udid> shell monkey -p <bundleId> -c android.intent.category.LAUNCHER 1`。
- `terminate`：`adb -s <udid> shell am force-stop <bundleId>`。
- `readlog`：`adb -s <udid> exec-out run-as <bundleId> cat <logPath>`，要求 App 可调试。
- Android 不支持 `app.env`，传入会明确报错，而不是静默忽略。
- 配置：`platform: android`、`adbCommand`（默认 `adb`）、`appTimeoutMs`。

`app` 适配器只做设备进程操作和读取文件；它不会点击 UI、等待 UI 元素或解析页面。

### 4.7 `sleep`

```yaml
- sleep: 200
```

等待毫秒数；本身不需要断言，耗时会计入该步骤和总耗时。

## 5. 断言

`expect` 可以同时写多个断言；全部通过该步骤才算通过。

| 断言 | 含义 | 取值来源 |
| --- | --- | --- |
| `status` | HTTP 状态码相等 | `actual.status` |
| `exitCode` | 命令/设备命令退出码相等 | `actual.exitCode` |
| `equals` | 标量/结构相等 | `actual.value` |
| `contains` | 字符串包含子串；actual 是数组时检查元素 | `actual.content`，没有则 `actual.value` |
| `matches` | 正则匹配（可写字符串或字符串数组） | 同 `contains` |
| `stdoutContains` | 标准输出包含子串（可数组） | `actual.stdout` |
| `gte` / `lte` | 数值大于等于 / 小于等于 | `actual.value`；HTTP 非数值时回退 `actual.status` |
| `json` | 点分路径浅层匹配 | `actual.json` |

约定：

- 字符串比较一律 `trim()`。
- `contains` / `stdoutContains` 支持单个字符串或字符串数组；数组表示“全部包含”。
- `matches` 用 JavaScript 正则；支持 `/pattern/flags` 字面量写法（如 `/ready/i`）。
- `json` 的键是点分路径，例如 `{ data.items.0.id: 7 }`；值是对象时做递归子集匹配，
  不是要求整个对象完全相等。
- `equals` 对对象要求键集合和值都相等（字符串仍 trim）；不要用它做“部分 JSON 匹配”，
  那应该用 `json`。
- 数值断言 `gte` / `lte` 会把数字字符串转成数字；`equals` 不做隐式类型转换。

失败时 CLI 输出：

```text
[2/3] http GET /health … FAIL (8ms)
      status: expected 200 / actual 503
      json: expected {"code":"OK"} / actual {"code":"DOWN"}
```

## 6. 证据与 `--json`

每次运行结束会写两个同名文件到证据目录：

```text
<YYYY-MM-DDTHH-mm>-<name>.txt    # 可读步骤表 + 失败详情
<YYYY-MM-DDTHH-mm>-<name>.json   # 结构化结果
```

JSON 顶层包含 `name`、`runId`、`source`、`ok`、`exitCode`、`startedAt`、
`finishedAt`、`durationMs`、`summary`、`variableNames`、`steps`。每个 step 包含：

```json
{
  "index": 1,
  "action": "http",
  "display": "http POST /api/auth/login",
  "expected": { "status": 200 },
  "actual": { "status": 200, "json": {} },
  "checks": [],
  "captures": ["access"],
  "ok": true,
  "durationMs": 12,
  "error": "",
  "hint": ""
}
```

`--json` 时进度和错误写到 stderr，stdout 只有一份 JSON，便于 agent 用
`JSON.parse`。证据路径写在 `result.evidence`。

注意：`--json` 的 stdout、场景证据、套件证据使用同一套脱敏（Bearer / JWT / 手机号 / 密码字段），
Token 会显示为 `[REDACTED]`。但响应体、stdout/stderr 和日志正文仍可能含业务数据，不要默认把证据提交到仓库；CI 应只作为短期 artifact
保留，敏感场景应先脱敏或改写到 `/tmp`。

## 7. 与 CI 配合

```yaml
# .github/workflows/verify.yml 片段
jobs:
  verify:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
      # 这里启动被测服务、PostgreSQL、Redis，或把连接信息写入 scenario
      - name: Run tool tests
        run: node --test tools/verify/test/
      - name: Run scenario
        env:
          VERIFY_DB_PASSWORD: ${{ secrets.VERIFY_DB_PASSWORD }}
        run: |
          node tools/verify/cli.mjs run ci/verify.yml \
            --evidence-dir "$RUNNER_TEMP/verify-evidence" \
            --json > "$RUNNER_TEMP/verify-result.json"
      - name: Upload evidence
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: verify-evidence
          path: |
            ${{ runner.temp }}/verify-evidence/
            ${{ runner.temp }}/verify-result.json
```

CI 集成要点：

1. **先准备依赖**：scenario 只验证，不负责启动 PostgreSQL/Redis/微服务。用 docker compose、
   testcontainers 或 CI service 先启动，再运行。
2. **密码放 CI secret**：scenario 里写 `passwordEnv: VERIFY_DB_PASSWORD`，CI 注入。
3. **退出码即门禁**：直接让 CI 步骤失败；不要吞掉 `1`/`2`。
4. **证据永远上传**：失败时的 actual 比日志更有诊断价值。
5. **不要用 `--json` 再接 `tee` 污染 stdout**；需要保存时用 shell 重定向，
   进度本来就在 stderr。
6. 场景拆分：冒烟场景用短超时，设备/长链路场景单独 job，避免一个失败掩盖其他结果。

## 8. 为什么 iOS UI 自动化只能靠项目自己的探针

`simctl` 能启动、终止模拟器 App，能定位 App data 容器，能读文件；它**不能**：

- 找到 SwiftUI 的按钮、输入框或导航栈；
- 可靠判断某个页面已经出现；
- 点击、输入、滚动、处理系统弹窗；
- 知道业务接口是否成功、Token 是否写入 Keychain；
- 等待“UI 已就绪”这种项目特有状态。

真正的 UI 自动化需要项目自己的 XCUITest target（或 Appium/Maestro 等额外工具）：
它依赖 App 的 target/build settings、签名、accessibility identifier、测试 bundle 和
启动参数。这些都不是一个通用外部工具能从 `simctl` 推断出来的。

因此本工具只做稳定、跨项目的部分：

1. 通过 `simctl` 启动 App；
2. 等 App 执行；
3. 读取 App 在调试构建里主动写出的探针日志（例如启动完成、登录成功、退出清理完成）；
4. 对日志做 `contains` / `matches` 断言；
5. 终止 App。

UI 是否正确，仍应由项目内的 XCUITest/人工验收负责；探针日志负责证明“设备上确实发生了
这一步”。探针建议只在 Debug/测试构建启用，并避免写入密码、完整 Token 和私密内容。
Android 同理：`adb` 能启动和读私有目录，但不能替代项目自己的 UI 测试。

## 9. 已知限制

- YAML 是受限子集：不支持块标量（`|`、`>`）、锚点/别名、多文档、复杂键。
- 没有重试、并行、依赖图、超时后自动补偿；步骤严格顺序执行，失败后继续跑完剩余步骤。
- SQL 只通过 psql 取标量，不内置连接池、事务或结果集表格断言。
- Redis 只通过 redis-cli；`count` 基于 `SCAN`，大库上可能较慢。
- `json` 只做点分路径子集匹配，不支持 JSONPath、通配符或数组过滤。
- HTTP 只做单次请求；302 默认跟随；没有 cookie jar、OAuth 流程或多步会话封装。
- `logfile` 的 glob 是简化实现，不支持完整 shell glob 语义。
- `app readlog` 要求项目提供探针日志；Android 还要求 App 可调试（`run-as`）。
- `app.env` 只在 iOS launch 可用；Android 无法给已安装应用注入环境变量。
- 证据默认保留完整 actual，可能包含敏感数据；请按项目策略清理。
- 默认证据目录位于工具自身，不会自动加入 `.gitignore`；提交前请检查。
- 同一分钟、同一 scenario name 的证据文件会互相覆盖；需要保留多次运行时请用不同
  `--evidence-dir` 或重命名 scenario。
- 没有 TypeScript 类型检查或构建步骤；所有代码按纯 ESM 直接执行。

## 10. 复制到其他项目

直接复制整个 `tools/verify/` 即可：

```bash
cp -R tools/verify /path/to/other-project/tools/verify
```

需要保证：

- Node.js >= 20；
- `psql` / `redis-cli` / `xcrun` / `adb` 按场景需要存在于 PATH，或通过 config 指定路径；
- 新建该项目的 scenario 和 config，不修改 `core/` 和 `adapters/`。

工具内部不 import 任何外部路径，不读取 food-map 或 `status/`，也没有硬编码的项目常量。
