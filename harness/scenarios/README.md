# harness/scenarios

本目录存放 **FoodMap 自己的验证场景**，由通用工具 [`tools/verify`](../../tools/verify/README.md) 执行。
工具本身与项目解耦，所有端口、账号、容器名、模拟器 UDID 都写在这里的 scenario 里。

## 已有场景

| 场景 | 覆盖 | 依赖 |
| --- | --- | --- |
| `b1-identity-consistency.yml` | 全库身份绑定/会话不变量只读核验；历史不一致也会失败，不自动清理 | PostgreSQL |
| `b1-identity-dual-write.yml` | 注册绑定双写、登录创建会话、登出撤销及脱敏字段 | Gateway/Auth/User + PostgreSQL + Redis |
| `b1-logout.yml` | B1 登出链路：注册 → 登录 → `/api/users/me` → 登出 → 旧 Access 401 → 旧 Refresh 401 → Refresh 状态落库 → 拒绝名单只 +1 | Gateway + Auth + Redis + PostgreSQL（`foodmap-redis` / `foodmap-postgres` 容器） |
| `b1-ui-login.yml` | **UI 全链路**：注册 → XCUITest 在模拟器输入账号密码并点击登录 → 断言进入地图页、`login_logs` 有 SUCCESS、认证日志 200 → 清理数据 | 上面的服务 + Xcode + 已启动模拟器；需要 `FM_TEST_PW` 环境变量 |
| `b1-ios-logout.yml` | B1 iOS 端：模拟器启动 Debug 构建，探针完成登录与登出，检查探针日志与数据库 `REVOKED` | 上面的服务 + 已启动的模拟器 + Debug 构建（`#if DEBUG` 探针） |

## 运行

推荐入口和验证纪律见 [harness 使用说明](../README.md#7-本地依赖感知验证方案-d) 与 [AGENTS.md](../../AGENTS.md#631-验证纪律)。

```sh
./harness/scripts/verify.sh                     # 按依赖就绪情况运行，跳过不算通过
./harness/scripts/verify.sh --dry-run           # 纯静态检查，不需要真实密码
VERIFY_REQUIRE_FULL=1 ./harness/scripts/verify.sh  # 验收：不允许跳过
```

五个场景均声明 `requires`，避免把通过 `command` 执行的数据库/Redis 操作误认为无依赖。
预检清单位于 [verify-dependencies.json](../verify-dependencies.json)；换机器应核对其中的模拟器 UDID、App 和日志路径，与场景配置一致。


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

## 凭据、配置与数据清理约定

- **凭据单一来源**：所有需要密码的场景统一读 `${env:FM_TEST_PW}`，仓库内不写明文密码；
  缺该变量时依赖预检标记 `test-password` 不就绪，场景跳过而不是用默认密码跑。
- **配置与预检一致**：容器名（`foodmap-postgres` / `foodmap-redis`）、模拟器 UDID、认证日志
  路径必须与 [verify-dependencies.json](../verify-dependencies.json) 保持一致；换机器时两处一起改。
- **只清理本轮数据**：teardown 按本轮注册返回的 `userId` / 查询到的 `accountId` 精确删除，
  删除后断言相关表计数为 0；**禁止按 `zxq_%` 之类前缀批量删库**，避免误删其他会话或历史数据。
- **全库审计与回归分开**：`b1-identity-consistency.yml` 是**只读全库审计**，用于发现历史不一致；
  它失败时说明库里有真实问题，场景**不会**自动修复或清理。其余四个场景是**本轮回归**，只断言
  本轮产生的数据。
- **历史基线**：本机当前存在约 39 个历史 `zxq_*` 账号（早期手工/联调遗留），不属于任何场景的
  清理范围；新增残留应以“运行前后计数不变”判断，而不是要求这类账号为零。

## 约定

- 场景只断言**可观测事实**：状态码、业务码、Redis key 数量、数据库状态、探针日志。
- 不把 Token、密码、完整手机号写进 scenario 或证据；账号名用 `date +%s` 之类生成，避免脏数据冲突。
- SQL 和 Redis 通过 `docker exec` 执行，避免要求本机安装 `psql` / `redis-cli`；装了的机器可以直接换成
  `sql` / `redis` 适配器。
- 新增联调切片时先补 scenario，再补台账；`status/data/runs/` 里的运行记录应能对应到某个场景。

## 当前验收点对应关系（不是完整验收覆盖）

| 验收点 | 已有可执行场景 | 当前边界 |
| --- | --- | --- |
| A-003 注册/登录正常路径、UI 进入地图 | [b1-ui-login.yml](b1-ui-login.yml)、[b1-logout.yml](b1-logout.yml) | 未覆盖 UI 注册及全部登录入口 |
| A-002 登出、旧双 Token 拒绝、REVOKED/denylist | [b1-logout.yml](b1-logout.yml)、[b1-ios-logout.yml](b1-ios-logout.yml) | 探针不是按钮交互；缺独立重启断言 |
| F-004/F-023 身份一致性与双写 | [b1-identity-consistency.yml](b1-identity-consistency.yml)、[b1-identity-dual-write.yml](b1-identity-dual-write.yml) | 全库异常须调查，不自动清理；不是仅核验本轮数据 |
| 错误密码、重复注册、短密码 UI 拦截、刷新成功、401/403 边界 | 尚未完整场景化 | 属后续场景覆盖计划，不因 5/5 通过而宣称已覆盖 |

旧接口/双写场景仍含固定测试凭据与数据留存约定，迁移为统一环境变量和完整 teardown 属后续工作；依赖感知入口不自动修改这些业务断言或清理历史账号。
