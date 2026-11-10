# 子代理任务单索引（后端加固）

- 来源方案：[2026-11-09-backend-hardening.md](../plans/2026-11-09-backend-hardening.md)
- 模板：`harness/agent-task-template.md`；边界规则：`harness/file-boundary-rules.md`
- 台账：任务状态只在 `status/data/tasks/` 修改，子代理不得改 `status/**`

## 分组与执行顺序

| 组 | 任务单 | 执行方式 | 说明 |
| --- | --- | --- | --- |
| D（日志链） | [T-028](T-028-logback-json.md) → [T-029](T-029-log-volume-collector.md) → [T-030](T-030-logging-closure-scenario.md) | 严格串行 | 后一个依赖前一个的产物 |
| C（内部信任链） | [T-031](T-031-disable-discovery-locator.md) → [T-032](T-032-internal-service-signature.md) → [T-033](T-033-admin-rbac-server-side.md) | 严格串行 | 同时改 gateway 与 common/security |

## W0 任务单

| 任务单 | 状态 |
| --- | --- |
| [T-025](T-025-token-secret-injection.md) Token 密钥统一注入与 fail-fast | 已派单完成，待提交 |
| T-026 Access/Refresh Token 严格分段校验 | **任务单待补**，规格见方案文档 §6 |
| T-027 登出必须写拒绝名单 | **任务单待补**，规格见方案文档 §6 |

> T-026 / T-027 与 T-025 同属 F-024。T-027 与 T-025 都改 `GatewayAuthFilter.java`，必须串行。

## W3 / W4 任务单

| 组 | 任务单 | 执行方式 | 说明 |
| --- | --- | --- | --- |
| E（部署链） | [T-034](T-034-dockerfile.md) → [T-035](T-035-prod-compose.md)；[T-037](T-037-github-actions-ci.md) | T-034 先行，T-035 与 T-037 可并行 | T-035 要求 T-029 已合并 |
| F（独立项） | [T-036](T-036-nginx-https.md)、[T-044](T-044-exception-mapping.md) | 可并行 | 仅改各自目录 |
| F（pom 链） | [T-039](T-039-mvnw-enforcer-redisson.md) → [T-040](T-040-prometheus-probes.md) / [T-041](T-041-openapi-versioning.md) / [T-043](T-043-archunit.md) | 串行，T-039 最先 | 都会碰 pom，不能真正并行 |
| G（auth 重构链） | [T-038](T-038-testcontainers.md) → [T-042](T-042-authserviceimpl-split.md) → [T-047](T-047-register-outbox.md) | 严格串行 | 同改 auth 服务 |
| 其他 | [T-045](T-045-data-access-hardening.md)、[T-046](T-046-clientip-audit.md) | T-045 建议在 T-038 后；T-046 依赖 T-028 | |

### W3 / W4 额外冲突提醒

1. T-035 与 T-036 都可能改 `deploy/env.example`，由主代理合并。
2. T-038、T-039、T-040、T-041、T-043 都会改 pom，建议先合并 T-039，其余在其后串行或由主代理手工合并 pom。
3. T-042、T-045、T-047 都会改 auth 服务，必须在 T-038 之后串行。
4. T-041 需要网关错误响应新类，但不得改 `GatewayAuthFilter.java`。
5. T-047 会改变注册响应语义，先与主代理确认再动手。

## 跨组冲突提醒（主代理派单前必看）

1. C 组与 W0 的 B 组（T-025 / T-027）都会改 `GatewayAuthFilter.java`，**B 组未合并前不得启动 T-033**；T-031 只改 `application.yml`，可先行。
2. T-032 与 T-026 都在 `common/security` 下，但文件不同（新建 `InternalService*` vs 修改 `HmacTokenCodec`），可并行，合并时主代理检查 `after/foodmap-common` 的 pom 冲突。
3. D 组与 C 组文件边界互不重叠，可同时各派一个子代理。
4. T-029 与 T-035（W3）都会改 `deploy/docker-compose.ecs2.yml`，W3 开始前 D 组必须已合并。

## 通用规则（每张任务单均适用）

- 必读：`AGENTS.md`、`CODEX-after.md`、`.agents/backend-agent.md`、`skills/foodmap-backend-service/SKILL.md`、`harness/*.md`。
- 越界即停：发现需要修改授权范围之外的文件，停止并报告主代理。
- 提交前：改动涉及 `after/` 时按 AGENTS.md 6.3.1 执行 `node tools/verify/cli.mjs run harness/scenarios/`；服务不可用时写明「未执行 + 原因」。
- 不得通过修改断言让验证变绿。
