# T-036 nginx HTTPS / HSTS / 安全响应头

- 功能点：F-021｜优先级：P0｜波次：W3｜组：F（独立）｜预估：0.5d｜依赖：无

## 1. 任务背景

`deploy/nginx/foodmap.conf` 只有 `listen 80`，登录、注册、Token 全部走明文 HTTP。

## 2. 必读文档

`AGENTS.md`、`CODEX-after.md`（部署约束）、`deploy/README.md`、现有 `deploy/nginx/foodmap.conf`。

## 3. 允许修改范围

```text
deploy/nginx/**
deploy/env.example（仅新增证书路径变量，与 T-035 冲突时由主代理合并）
```

## 4. 禁止修改范围

```text
after/、front/、status/**、deploy/docker-compose.*.yml（证书挂载需求在汇报中说明，由 T-035 落地）
```

## 5. 具体交付物

1. 80 端口全部 301 跳转 443。
2. 443 启用 TLS 1.2/1.3，禁用弱套件，证书路径可配置，仓库不得包含私钥。
3. 响应头：`Strict-Transport-Security`、`X-Content-Type-Options`、`X-Frame-Options`、`Referrer-Policy`。
4. 保持 `location /actuator/` 返回 403，并新增对 `/actuator/prometheus` 的同样保护。
5. 补自签证书生成说明（仅用于本地验证）。

## 6. 验收标准

- [ ] `nginx -t` 配置检查通过（用容器执行）。
- [ ] HTTP 请求得到 301。
- [ ] 自签证书下 `curl -k https://localhost/api/...` 路由正常。
- [ ] 响应含上述安全头。
- [ ] 仓库中无证书私钥。

## 7. 汇报格式

同 `harness/agent-task-template.md` 第 8 节。
