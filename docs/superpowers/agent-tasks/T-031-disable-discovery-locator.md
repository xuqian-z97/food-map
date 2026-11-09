# T-031 关闭 gateway discovery locator

- 功能点：F-025｜优先级：P0｜波次：W2｜组：C（第 1 步）｜预估：0.2d｜依赖：无

## 1. 任务背景

`spring.cloud.gateway.discovery.locator.enabled: true` 会为每个 Nacos 服务自动生成 `/{serviceId}/**` 路由。`GatewayAuthFilter` 只对原始路径以 `/api/` 开头的请求认证、对以 `/internal/` 开头的请求拦截，因此 `/foodmap-log-service/internal/...` 会绕过两道检查后被重写转发。只保留显式路由。

## 2. 必读文档

`AGENTS.md`、`CODEX-after.md`（网关章节）、`harness/file-boundary-rules.md`。

## 3. 允许修改范围

```text
after/foodmap-gateway-service/src/main/resources/application.yml
after/foodmap-gateway-service/src/test/java/com/foodmap/gateway/**
```

## 4. 禁止修改范围

```text
after/foodmap-gateway-service/src/main/java/**（GatewayAuthFilter 由 T-025 / T-027 / T-033 负责，避免冲突）
其他 after 服务、front/、deploy/、status/**
```

## 5. 具体交付物

1. 将 `locator.enabled` 改为 `false`，并删除不再需要的 `lower-case-service-id`。
2. 新增网关测试：确认配置中不存在自动路由；确认显式路由（`/api/auth/**`、`/internal/*/health` 等）仍可匹配。
3. 如 `/internal/**/health` 的验证依赖 locator，改用显式路由验证并在汇报中说明。

## 6. 验收标准

- [ ] `/foodmap-log-service/internal/logs/api-access` 返回 404。
- [ ] `/api/auth/login` 与 `/internal/auth/health` 的路由行为不变。
- [ ] `cd after && mvn -q -B -pl foodmap-gateway-service -am test` 通过。
- [ ] 有本地环境时 `harness/verify-dependencies.json` 中的网关健康检查仍通过。
- [ ] 文件边界已遵守；不涉及可见范围与 PUBLIC 统计。

## 7. 汇报格式

```text
完成内容：
修改文件：
验证结果：
- 已执行：
- 未执行：…，原因：…
风险和后续：
```
