# harness/scenarios

本目录存放 **FoodMap 自己的验证场景**，由通用工具 [`tools/verify`](../../tools/verify/README.md) 执行。
工具本身与项目解耦，所有端口、账号、容器名、模拟器 UDID 都写在这里的 scenario 里。

## 已有场景

| 场景 | 覆盖 | 依赖 |
| --- | --- | --- |
| `b1-logout.yml` | B1 登出链路：注册 → 登录 → `/api/users/me` → 登出 → 旧 Access 401 → 旧 Refresh 401 → Refresh 状态落库 → 拒绝名单只 +1 | Gateway + Auth + Redis + PostgreSQL（`foodmap-redis` / `foodmap-postgres` 容器） |
| `b1-ui-login.yml` | **UI 全链路**：注册 → XCUITest 在模拟器输入账号密码并点击登录 → 断言进入地图页、`login_logs` 有 SUCCESS、认证日志 200 → 清理数据 | 上面的服务 + Xcode + 已启动模拟器；需要 `FM_TEST_PW` 环境变量 |
| `b1-ios-logout.yml` | B1 iOS 端：模拟器启动 Debug 构建，探针完成登录与登出，检查探针日志与数据库 `REVOKED` | 上面的服务 + 已启动的模拟器 + Debug 构建（`#if DEBUG` 探针） |

## 运行

```bash
# 需要密码的场景（b1-ui-login 等），密码从环境变量读，不写进仓库
export FM_TEST_PW='<测试账号密码>'

# 一次跑完全部场景（推荐给 agent）
node tools/verify/cli.mjs run harness/scenarios/ --json --evidence-dir /tmp/verify-evidence

# 单场景（接口级）
node tools/verify/cli.mjs run harness/scenarios/b1-logout.yml --evidence-dir /tmp/verify-evidence

# iOS 端链路（按需修改 scenario.config.udid）
node tools/verify/cli.mjs run harness/scenarios/b1-ios-logout.yml --evidence-dir /tmp/verify-evidence
```

退出码：`0` 全通过，`1` 有步骤失败，`2` scenario/配置错误。
每次运行都会在证据目录生成 `.txt`（人看）和 `.json`（agent 读）。

## 约定

- 场景只断言**可观测事实**：状态码、业务码、Redis key 数量、数据库状态、探针日志。
- 不把 Token、密码、完整手机号写进 scenario 或证据；账号名用 `date +%s` 之类生成，避免脏数据冲突。
- SQL 和 Redis 通过 `docker exec` 执行，避免要求本机安装 `psql` / `redis-cli`；装了的机器可以直接换成
  `sql` / `redis` 适配器。
- 新增联调切片时先补 scenario，再补台账；`status/data/runs/` 里的运行记录应能对应到某个场景。
