# T-041 springdoc OpenAPI 与 API 版本策略

- 功能点：F-027｜优先级：P1｜波次：W4｜组：F（pom 链）｜预估：2d｜依赖：建议 T-039 之后

## 1. 任务背景

技术栈文档写了 OpenAPI + Knife4j，但仓库没有 springdoc 依赖。API 路径没有版本号。`docs/api/auth-user.md` 描述了未实现的 `/internal/users/bootstrap` 等接口，refresh 的 Javadoc 写轮换而实现不轮换。网关对 404、405、429、5xx 没有统一 `ApiResponse` 包装。

## 2. 必读文档

`AGENTS.md`、`harness/api-contract-checklist.md`、`skills/foodmap-api-contract/SKILL.md`、`.agents/api-agent.md`、`docs/api/auth-user.md`。

## 3. 允许修改范围

```text
after/foodmap-auth-service、after/foodmap-user-service 的 pom.xml 与 controller 包
after/foodmap-gateway-service/src/main/resources/application.yml（版本路由）
after/foodmap-gateway-service/src/main/java（仅统一错误响应处理，新建类）
docs/api/**
```

## 4. 禁止修改范围

```text
front/、deploy/、status/**
GatewayAuthFilter.java（属 W0、W2 任务）
CODEX-after.md（需要时在汇报中列出要点由主代理补写）
```

## 5. 具体交付物

1. 引入 springdoc + Knife4j，生产环境默认关闭文档端点。
2. 版本策略二选一并在汇报中说明：网关重写 `/api/v1/**`，或 controller 路径迁移；需要同步前端时先停下报告。
3. 网关统一错误响应，覆盖 404、405、429、502、503、504。
4. 修正 refresh Javadoc 与 `docs/api/auth-user.md` 的偏差。

## 6. 验收标准

- [ ] `/v3/api-docs` 可访问（非生产）。
- [ ] 文档含 DTO schema 与错误码。
- [ ] 版本路由行为有测试。
- [ ] 文档与实现 diff 为 0。
- [ ] 现有 `GatewayAuthFilterTest` 不回归。

## 7. 汇报格式

同 `harness/agent-task-template.md` 第 8 节。
