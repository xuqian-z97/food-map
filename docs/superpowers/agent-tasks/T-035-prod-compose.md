# T-035 生产 compose 补齐应用服务

- 功能点：F-021｜优先级：P0｜波次：W3｜组：E（第 2 步）｜预估：1.5d｜依赖：T-034 已合并；T-029 已合并（同改 ecs2 compose）

## 1. 任务背景

`deploy/docker-compose.ecs2.yml` 只有 nginx、gateway、nacos、postgres、redis、rabbitmq，应用服务仍是占位注释；`deploy/env.example` 缺少 `ADMIN_DB_USERNAME`、`ADMIN_DB_PASSWORD`、`ADMIN_LOG_SERVICE_BASE_URL` 等变量，admin 的 prod profile 会启动失败。

## 2. 必读文档

`AGENTS.md`、`CODEX-after.md`、`deploy/README.md`、`deploy/checklists/`、T-025、T-029、T-034 任务单及其汇报。

## 3. 允许修改范围

```text
deploy/docker-compose.ecs2.yml
deploy/env.example
```

## 4. 禁止修改范围

```text
after/、front/、status/**、deploy/nginx/（属 T-036）、deploy/logging/
```

## 5. 具体交付物

1. 补 auth、user、admin、log 服务定义，使用 T-034 镜像与 `--build-arg MODULE`。
2. 每个服务配置 `healthcheck`、`depends_on: condition: service_healthy`、`restart: unless-stopped`、内存与 CPU 限制、日志卷挂载。
3. 内部变量统一注入：`FOODMAP_TOKEN_SECRET`、`FOODMAP_INTERNAL_SECRET`（若 T-032 已合并）、各服务 DB 变量、`ADMIN_LOG_SERVICE_BASE_URL` 指向容器服务名。
4. 逐服务整理 env 清单，缺关键变量时容器启动即失败，不给默认值。
5. 删除「Application services are placeholders」注释。
6. 与 T-036 协调：`env.example` 若有冲突，由主代理合并。

## 6. 验收标准

- [ ] `docker compose -f deploy/docker-compose.ecs2.yml config` 无错误。
- [ ] 全栈启动后网关健康检查为 UP（有环境时）。
- [ ] 注册、登录、查询当前用户链路经网关端到端通过。
- [ ] env.example 覆盖所有 prod profile 必填变量，无真实密钥。
- [ ] 无环境时写明「未执行 + 原因」。

## 7. 汇报格式

同 `harness/agent-task-template.md` 第 8 节。
