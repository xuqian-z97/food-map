# T-033 后台 RBAC 交付风险与后续事项

- 日期：2026-11-11
- 关联：[T-033 设计稿](../superpowers/specs/2026-11-10-t033-admin-rbac-implementation.md) · [任务卡 T-033](../superpowers/agent-tasks/T-033-admin-rbac-server-side.md) · [验收单 A-009](../../status/data/acceptance/A-009.md) · [后端加固计划](../superpowers/plans/2026-11-09-backend-hardening.md)
- 交付范围：后台登录 / 登出 / 日志只读 RBAC / 网关 `/api/admin/**` 收口 / 受控初始化 / 审计
- 状态基线：分支 `feature/t033-admin-rbac`（1389aa2 实现 + c4871d4 台账），本地 L2 验收通过

本文档沉淀 T-033 交付时点遗留的风险、测试覆盖边界和后续工作项，作为后续切片与部署验收的输入。每项标注建议承接方与优先级。

## 1. 安全与生产准入（生产开放 `/api/admin/**` 路由前必须完成）

| 编号 | 事项 | 说明与影响 | 建议承接 | 优先级 |
| --- | --- | --- | --- | --- |
| S-1 | 生产密钥注入 | 网关与管理后台必须通过 `ADMIN_TOKEN_PUBLIC_KEY_LOCATION` / `ADMIN_TOKEN_PRIVATE_KEY_LOCATION` 注入外部密钥文件，缺失或无法解析时启动失败（已实现 fail-fast）。`deploy/env.example` 尚未补这两个变量 | T-035（生产 compose） | P0 |
| S-2 | 仓库内开发密钥边界 | `after/*/resources/keys/admin-token-dev-*.pem` 仅为 local/orbstack 开发密钥，已随仓库公开。生产必须使用独立密钥对，且不得复用开发私钥 | T-035 部署检查单 | P0 |
| S-3 | HTTPS 与后台入口限制 | 生产路由开放前需 HTTPS、来源 IP 限制与更严格限流（设计稿 §3.3 与 CODEX-after §11.5 要求） | T-036（nginx） | P0 |
| S-4 | 登录限流升级 | 当前 `AdminLoginRateLimiter` 为单实例内存实现（默认 5 次 / 5 分钟，键为请求体账号名，不信任客户端头）。多实例部署时各实例独立计数，防爆破强度下降 | 引入 Redis 统一封装实现（复用 `foodmap-common` 设施） | P1 |
| S-5 | 密钥轮换流程 | 首版单密钥：轮换时需两端（admin + gateway）同步切换配置并让全部管理员重新登录；无 kid 多密钥并行验签 | 首次轮换前在部署文档中固化操作步骤 | P2 |
| S-6 | Bootstrap 生产使用条件 | 受控初始化入口（`ADMIN_BOOTSTRAP_ENABLED=true` + `ADMIN_BOOTSTRAP_PASSWORD` 等环境变量）只应用于一次性初始化；生产常驻开启会带来密码环境暴露面 | 部署检查单约定：初始化完成后关闭 | P1 |

## 2. 功能缺口（后续后台切片）

| 编号 | 事项 | 说明 | 建议切片 |
| --- | --- | --- | --- |
| F-1 | 在线角色与权限管理接口 | `PUT /api/admin/roles/**`、账号启停等接口未实现。注意：任何角色 / 关联 / 权限变更必须与受影响管理员的 `permission_version` 递增在同一本地事务完成（批量变更按固定 ID 顺序锁定），该语义已在 V2 表结构与授权查询中预留，但尚无在线入口 | 后台阶段 A 第二批 |
| F-2 | 后台账号管理页面 / 接口 | `GET/POST /api/admin/users` 未暴露 HTTP 接口（`AdminUserService.createAdminUser` 目前只由 Bootstrap 调用） | 后台阶段 A 第二批 |
| F-3 | `GET /api/admin/me` 与 `/me/permissions` | CODEX-after §10.8 已规划，未实现；当前客户端只能通过 401/403 感知权限 | 后台阶段 A 第二批 |
| F-4 | 后台 Refresh Token | 当前 Access Token 15 分钟，到期需重新登录；后台刷新 Token 未实现 | 视运营反馈排期 |
| F-5 | 审核 / 举报 / 门店合并 / 用户管理 | CODEX-after §5.10 阶段 B/C 能力，均未开始 | 后台阶段 B/C |

## 3. 运维与数据治理

| 编号 | 事项 | 说明 | 建议承接 | 优先级 |
| --- | --- | --- | --- | --- |
| O-1 | `admin_access_sessions` 过期登记清理 | 已按 `expires_time` 建索引，但分批清理任务未实现（设计稿 §7 决策点 2 要求定稿清理策略）。过期行不影响正确性（授权查询同时校验时间），仅持续增长 | 参照 log-service 15 天清理任务的实现模式补一个后台定时任务 | P1 |
| O-2 | `admin_operation_logs` 保留策略 | 审计表无清理与归档策略；按当前量级增长缓慢，但需在运营报表切片（阶段 D）前定稿 | 后台阶段 D | P2 |
| O-3 | 每请求会话查询的负载 | 设计已接受「每受保护后台请求查询一次后台库」的代价（换取消除的确性与重启安全）。后台 QPS 极低；如未来出现高频后台轮询，再评估缓存 | 观察项 | P3 |
| O-4 | Hikari 池化与短事务观测 | 登录短事务内含 PBKDF2 复核（约 50ms 级 CPU），行锁持有时间可控；上线后关注 `foodmap-admin-hikari` 连接等待指标 | T-040（Prometheus） | P2 |

## 4. 测试覆盖边界（本次未验证项及原因）

| 编号 | 事项 | 覆盖方式与原因 |
| --- | --- | --- |
| T-1 | 数据源故障 503 | 单元测试覆盖（`AdminAuthServiceImplTest` / `AdminAuthorizationServiceImplTest` 注入 `DataAccessResourceFailureException`）；不对共享 PostgreSQL 停库注入 |
| T-2 | 过期 Token / 过期会话 | 单元测试覆盖（签发负 TTL Token、过期快照）；e2e 因 15 分钟 TTL 不便构造 |
| T-3 | 登录限流 429 | 单元测试覆盖；e2e 会污染同账号名限流窗口 |
| T-4 | `b1-logging-closure` 场景 | 本次未运行：需专用日志验证认证实例 18081，与 T-033 无关，已按日常模式记录 SKIP；下次日志链路验证时补跑 |
| T-5 | Redis 故障注入 | 沿用 A-008 结论：内部签名 nonce store fail-closed 由单测覆盖，不停止共享 Redis |

## 5. 环境注意事项（本地联调经验）

| 编号 | 事项 | 说明 |
| --- | --- | --- |
| E-1 | 多网卡机器的 Nacos 注册 IP | 本机存在多个网段接口时，服务可能注册到不可达 IP（本次 user-service 注册到 `192.168.49.91` 导致网关 lb 超时）。本地单机验证建议显式指定 `--spring.cloud.nacos.discovery.ip=127.0.0.1`；生产部署在 T-035 中约定固定注册 IP |
| E-2 | `spring-boot:run` 孤儿进程 | `pkill` mvn 包装进程后 fork 的 java 子进程仍占用端口，重启前先按端口清理：`lsof -nP -iTCP:<port> -sTCP:LISTEN -t \| xargs kill` |
| E-3 | 场景对 auth 日志路径的期望 | `b1-ui-login` 期望 auth 日志追加写入 `/tmp/verify-auth.log`；本地起 auth 验证时用 `mvn spring-boot:run 2>&1 \| tee -a /tmp/verify-auth.log`，否则该场景第 4 步断言失败（本次已按此方式复跑通过 17/17） |

## 6. 结论

T-033 交付切片本身无已知阻塞缺陷；上述事项均为显式边界或后续排期。生产开放后台路由前，S-1 ~ S-3 为硬性前置；F-1 在实现时必须遵守「授权变更与 `permission_version` 递增同事务」的既定语义。
