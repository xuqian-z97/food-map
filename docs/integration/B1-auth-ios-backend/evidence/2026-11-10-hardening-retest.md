# T-026 / T-027 / T-032 缺陷修复复测

## 范围与基线

- 审查基线：`f570464`；修复对应 BUG-013～015，具体行为与验收结论见 A-007/A-008。
- 代码及场景精确 SHA-256 清单见同名 JSON 的 `build`；该指纹标识已测试工作树，不把历史基线 SHA 冒称为已包含修复的提交。
- 不执行 T-033、不修改前端、不修改生产配置、不清理历史数据。

## 修复

1. Token 按类型保留末尾空段分割并拒绝空片段，阻断原 Access Token 追加 `.` / `..` 后改变哈希绕过 denylist。
2. 登出仅在 Authorization 完全缺少时走兼容路径。已提供但无效/过期时，在会话写操作之前返回 401，不撤销 Refresh/Session，不写 denylist。
3. signer 不再受 Feign 类路径条件限制；Feign 拦截器独立条件装配。admin 无 Feign 的上下文可正常创建 Java HTTP 日志客户端。

## 红绿回归

先补测试再修实现：修复前 common 2、gateway 2、auth 7、admin 1 个回归失败；为一次收集所有失败使用 `maven.test.failure.ignore=true`，只作为红灯证明，绝不当作通过依据。

修复后执行不带忽略失败参数的 `cd after && mvn -o -B test`：**212 tests / 0 failures / 0 errors / 0 skipped**。相对原 193 个增加 19 次测试执行。

## 真实链路

- 扩展 `b1-logout`：36/36（含 setup/teardown），验证无效 Authorization 401、Refresh 仍 ACTIVE、有效登出 denylist +1、登出后尾空段 Token 401、无头兼容分支。
- 新增 `b1-internal-service-auth`：verify 1/1，其探针 13 个 HTTP 检查，包括 locator 404、缺头/过期/重放/篡改 401、越权 403、health 200，以及真实 admin 8088 → log 8089 200、随机 requestId 筛选透传。
- admin 调用仍使用现有请求头权限骨架，仅验证 T-032 出站签名，**不构成 T-033 管理员身份安全验收**。
- 日志专用 Auth 18081 原是旧实例，调用 provision 401、注册 502；重新以本次 classes 启动并显式连接 user 8082 后，文件/Kafka/ES/日志库闭环通过。主 Auth 8081 保留 Nacos 服务发现。

## 严格套件与环境权限

`VERIFY_REQUIRE_FULL=1 ./harness/scripts/verify.sh` 最终 **11 passed / 0 failed / 0 skipped**，suite ID 与时间见同名 JSON。

默认 workspace-write 沙箱无法分配伪终端，两个 XCUITest 运行器报错；明确审批后在扩大权限下原样执行，登录 UI 与注册短密码拦截均真实各运行 1 个测试通过，未修改断言、未标记 SKIP。普通日常模式退出码不用于替代严格全量结果。

## 运行环境变化

- 重启本机主 Auth 8081、Gateway 8080、日志专用 Auth 18081，日志继续分别写 `/tmp/verify-auth.log`、`/tmp/verify-gateway.log`、`/tmp/verify-auth18081.log`。
- 新启 admin 8088，日志 `/tmp/verify-admin.log`，不注册 Nacos，不改原有 user 8082/log 8089 进程。
- 18081 JSON 文件继续落到 `/tmp/foodmap-orbstack-logs` 供既有采集链路使用；其余新启服务 JSON 文件落 `/tmp/foodmap-hardening-logs`。
- 服务由本会话后台任务管理；用户若需停止可按本轮端口核对后停止，不批量杀 Java 进程。

## 未覆盖边界

生产镜像/网络/Compose、真实管理员 RBAC、共享 Redis 停机故障注入、完整 prod 配置启动仍不在本次范围。已有两处 filter 输入流 read 方法 Javadoc 提示未作为本次功能缺陷处理。
