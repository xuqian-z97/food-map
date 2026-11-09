# T-040 Prometheus 指标与就绪探针

- 功能点：F-026｜优先级：P1｜波次：W4｜组：F（pom 链）｜预估：2d｜依赖：建议 T-039 之后

## 1. 任务背景

所有服务 `management.endpoints.web.exposure.include` 只有 `health,info`，没有 Prometheus、探针分组，也看不到 HikariCP、Redis pool、线程池指标，与 CODEX-after.md 的要求不一致。

## 2. 必读文档

`AGENTS.md`、`CODEX-after.md`（连接池与指标条款）、`.agents/backend-agent.md`。

## 3. 允许修改范围

```text
after/foodmap-*-service/pom.xml（仅加 micrometer-registry-prometheus）
after/foodmap-*-service/src/main/resources/application*.yml（仅 management 段）
after/foodmap-*-service/src/test/**（新增指标暴露测试）
```

## 4. 禁止修改范围

```text
after/**/src/main/java、front/、deploy/、status/**
after/pom.xml（如需改请先报告，避免与其他 pom 任务冲突）
```

## 5. 具体交付物

1. 各服务暴露 `/actuator/prometheus`。
2. 开启 `management.endpoint.health.probes.enabled`，区分 liveness 与 readiness。
3. 确认 HikariCP、Lettuce/Redis、Tomcat 线程池、HTTP 请求指标可见。
4. 给出最小告警集清单（5xx 率、P99、Hikari pending、Redis timeout、Kafka lag）写入汇报，不新增监控栈部署。
5. Prometheus 端点不得对外暴露，nginx 层保护由 T-036 负责。

## 6. 验收标准

- [ ] 本地启动服务后 `/actuator/prometheus` 含 `hikaricp_connections` 指标。
- [ ] liveness 与 readiness 端点分离。
- [ ] 指标中不含 Token、密码等敏感标签。
- [ ] `cd after && mvn -q -B test` 通过。

## 7. 汇报格式

同 `harness/agent-task-template.md` 第 8 节。
