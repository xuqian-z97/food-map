# 跨项目复用验收报告（2026-11-10）

## 结论

**通过。** `tools/verify/` 复制到一个与 FoodMap 完全无关的目录后，可以独立运行：单测全绿、
正常与严格模式均通过、缺依赖时如实跳过并拒绝假绿。本轮发现并修复两个会影响“复制即用”的缺陷。

## 方法

在 `/tmp/xproj` 构造一个最小工程（不依赖 Node 工程结构、不用 Docker、不用数据库）：

```text
/tmp/xproj
├── server.py          # Python 标准库 HTTP 服务：/health、POST /items、GET /items/<id>
├── deps.json          # 依赖预检：network（服务健康）、python
├── scenarios/smoke.yml
├── logs/              # 访问日志
└── tools/verify/      # 从仓库整体复制（不含 evidence/）
```

场景只使用 `http` 和 `logfile` 两个适配器，覆盖：健康检查（带 retry）、创建条目、
按 id 查询、404 负向、访问日志断言。选择 Python 是为了证明工具不挑被测项目的语言。

## 结果

| # | 验证项 | 结果 |
| --- | --- | --- |
| 1 | 独立目录运行单测 | `node --test tools/verify/test/` **42/42 通过** |
| 2 | 解耦检查 | 核心与文档中 `foodmap` / `FOODMAP_` / `:8080` / `com.foodmap` **0 命中** |
| 3 | 正常模式 | 1/1 场景通过、5 步、退出码 0 |
| 4 | 严格模式（服务在跑） | 1/1 通过、0 跳过、退出码 0 |
| 5 | 服务停掉·普通模式 | `ok=false, status=partial, skipped=1`、退出码 0，跳过原因：「示例服务未启动」 |
| 6 | 服务停掉·严格模式 | 退出码 **1**，不把跳过当通过 |
| 7 | 场景只需改配置 | 未修改 `core/` 与 `adapters/` 一行代码 |

## 本轮发现并修复的问题

**1. 复制工具会带上本机证据**

`tools/verify/evidence/` 是运行产物（已被 `.gitignore` 忽略），但直接 `cp -R` 会把它带进新项目，
里面含 FoodMap 的数据库查询与场景名。已在 README 复制说明中明确“不要复制 `evidence/`”，
并保留忽略规则。

**2. 文档示例里残留 FoodMap 端口**

`README.md` 与 `core/scenario.mjs` 的示例提示使用了 `127.0.0.1:8080`。虽然只是示例，但会让
“解耦检查”误报，也容易误导复用者照抄 FoodMap 端口。已统一改为中性的 `9000`。

**3. 依赖名必须与动作推断匹配**

场景含 `http` 步骤会自动推断出 `network` 依赖。模板最初只配置了 `demo-service`，
结果整条场景被跳过。这说明预检是保守的（宁可跳过也不乱跑），但也要求清单命名一致。
已在模板 README 写明这条规则，模板清单改用 `network`。

## 新增的模板

`tools/verify/examples/minimal-project/` 已提交进仓库，包含：

- `server.py`：可独立运行的 Python 最小服务
- `scenarios/smoke.yml`：5 步完整链路示例
- `deps.json`：依赖预检清单示例
- `README.md`：如何运行、复制到新项目后要改哪三处、为什么不能改 `core/`

## 已知边界

- 只验证了 `http` + `logfile` 两个适配器；`sql` / `redis` / `app` / `xcode` 的跨项目用法
  已由 FoodMap 场景覆盖，但不在本模板内演示。
- 未在第二种操作系统上验证（本轮为 macOS）。
- 模板假设被测服务由使用者手动启动，不包含自动拉起逻辑（这是工具既定的边界）。

## 复现命令

```sh
# 仓库内
cd tools/verify/examples/minimal-project && python3 server.py --port 19100

# 另开终端（仓库根目录）
node tools/verify/cli.mjs run tools/verify/examples/minimal-project/scenarios \
  --dependencies tools/verify/examples/minimal-project/deps.json

# 复制到其他项目（排除 evidence）
cp -R tools/verify /path/to/project/tools/verify
rm -rf /path/to/project/tools/verify/evidence
```
