# 最小示例项目（跨项目复用模板）

这是一个**与 FoodMap 无关**的最小工程，用来证明 `tools/verify/` 可以脱离本项目使用。
它只用 Python 标准库，不依赖 Node 工程、Docker、数据库或模拟器。

## 结构

```text
minimal-project/
├── server.py              # 20 行风格的 HTTP 服务：/health、/items、/items/<id>
├── deps.json              # 依赖预检清单（服务是否在跑、python3 是否存在）
├── scenarios/smoke.yml    # 一个场景：健康检查 + 创建 + 查询 + 404 + 日志断言
├── logs/                  # 访问日志（运行时生成，不提交内容）
└── README.md
```

## 怎么用

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

## 复制到新项目时要改什么

只需要改这些，**不要改 `tools/verify/core` 和 `adapters`**：

1. `deps.json`：换成新项目的服务地址、命令、容器名或日志路径。
   注意：`http` 步骤会自动推断出 `network` 依赖，清单里必须配置同名条目，否则场景会被跳过（这是有意的保守行为）。
2. `scenarios/*.yml`：`config.baseUrl`、变量、断言。
3. 如果新项目用 iOS/Android，再参考仓库里 `harness/scenarios/` 的 `xcode` / `app` 用法。

## 它验证了什么

- 工具核心不含任何 FoodMap 路径、端口、容器名
- 依赖预检、跳过语义、严格模式在非 Node 项目上同样成立
- 场景只需要 `http` + `logfile` 两个适配器就能表达一条完整链路
