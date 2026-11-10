# 最小示例项目（跨项目复用模板）

这是一个**与 FoodMap 无关**的最小工程，用来证明 `tools/verify/` 可以脱离本项目使用。
它只用 Python 标准库，不依赖 Node 工程、Docker、数据库或模拟器。

## 结构

```text
minimal-project/
├── server.py              # 20 行风格的 HTTP 服务：/health、/items、/items/<id>
├── deps.json              # 依赖预检清单（只检查示例服务是否在跑）
├── scenarios/smoke.yml    # 一个场景：健康检查 + 创建 + 查询 + 404 + 日志断言
├── logs/                  # 访问日志（运行时生成，不提交内容）
└── README.md
```

## 怎么用

### 情况 A：在本仓库内试跑

```sh
# 1. 起服务（在 minimal-project 目录内）
python3 server.py --port 19100

# 2. 另开一个终端，在仓库根目录执行
node tools/verify/cli.mjs run tools/verify/examples/minimal-project/scenarios \
  --dependencies tools/verify/examples/minimal-project/deps.json

# 3. 严格模式：任何跳过都算失败
VERIFY_REQUIRE_FULL=1 node tools/verify/cli.mjs run tools/verify/examples/minimal-project/scenarios \
  --dependencies tools/verify/examples/minimal-project/deps.json
```

### 情况 B：已经复制到新项目（按复制后的布局执行）

复制后的目录是 `scenarios/`、`deps.json`、`server.py` 与 `tools/verify/` 同级，
所以命令里的路径要跟着变：

```sh
cd /path/to/other-project
python3 server.py --port 19100          # 另开终端

node tools/verify/cli.mjs run scenarios --dependencies deps.json

VERIFY_REQUIRE_FULL=1 node tools/verify/cli.mjs run scenarios --dependencies deps.json
```

上面两条就是复制后应该照抄的命令；如果只照抄“情况 A”的路径，会指向原仓库的示例目录。

## 复制到新项目（完整命令）

```sh
# 1. 只复制工具本体，排除本机证据与运行日志
rsync -a --exclude='evidence/' --exclude='examples/minimal-project/logs/*.log' \
  tools/verify/ /path/to/other-project/tools/verify/

# 2. 把模板的场景/清单/服务放到新项目根目录（按需改名）
cp -R tools/verify/examples/minimal-project/{scenarios,deps.json,server.py} \
  /path/to/other-project/
```

运行前提：

- Node.js >= 20（工具本体运行需要）；示例服务需要 Python 3（仅本模板）。
- 回环端口可绑定：示例固定 19100，换端口要同时改 `server.py --port`、`deps.json` 和 `scenarios/smoke.yml`。
- `scenarios/smoke.yml` 里 `logRoot: ../logs` 是相对**场景文件目录**解析的，所以日志落在
  `minimal-project/logs/access.log`；移动文件时记得同步改这个相对路径。

## 复制到新项目时要改什么

只需要改这些，**不要改 `tools/verify/core` 和 `adapters`**：

1. `deps.json`：换成新项目的服务地址、命令、容器名或日志路径。
   注意：`http` 步骤会自动推断出 `network` 依赖，清单里必须配置同名条目，否则场景会被跳过（这是有意的保守行为）。
2. `scenarios/*.yml`：`config.baseUrl`、变量、断言。
3. 如果新项目用 iOS/Android：iOS 参考 `xcode`（需自带 UI 测试 target 与 scheme）或 `app`（simctl），
   Android 用 `app` 的 adb 分支；仓库里的 `harness/scenarios/` 不会随工具复制，需照 README 自己写。

## 它验证了什么

- 工具核心不含任何 FoodMap 路径、端口、容器名
- 依赖预检、跳过语义、严格模式在非 Node 项目上同样成立
- 场景只需要 `http` + `logfile` 两个适配器就能表达一条完整链路
