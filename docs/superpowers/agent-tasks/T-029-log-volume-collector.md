# T-029 应用服务挂载日志卷并对齐采集

- 功能点：F-019｜优先级：P0｜波次：W1｜组：D（第 2 步）｜预估：1d｜依赖：T-028 已合并

## 1. 任务背景

`foodmap-app-logs` 卷只挂给了 Fluent Bit，没有任何应用服务挂载；`rewrite_tag` 用正则匹配 message 文本路由，事件名一改就静默失效。T-028 引入 `logType` 字段后，本任务改为按字段路由并让应用真正写入该卷。

## 2. 必读文档

`AGENTS.md`、`CODEX-after.md`（日志与部署章节）、`harness/file-boundary-rules.md`、T-028 任务单与其汇报、`deploy/logging/` 现有配置。

## 3. 允许修改范围

```text
deploy/docker-compose.dev.yml
deploy/docker-compose.ecs2.yml（只加日志卷挂载，不补应用服务，补服务属 T-035）
deploy/logging/fluent-bit/fluent-bit.conf
deploy/logging/fluent-bit/parsers.conf
deploy/logging/logstash/pipeline/foodmap-logs.conf（仅当字段路由需要）
harness/scripts/validate-logging.sh
```

## 4. 禁止修改范围

```text
after/、front/、status/**
deploy/nginx/、deploy/env.example
deploy/logging/postgres/、deploy/logging/elasticsearch/
```

## 5. 具体交付物

1. dev 与 ecs2 compose 中，凡是应用服务（已存在的 gateway 等）均挂载 `foodmap-app-logs:/foodmap/logs:rw`；Fluent Bit 保持 `:ro`。
2. `fluent-bit.conf`：`rewrite_tag` 改为按 JSON 字段 `logType` 路由，删除基于 message 文本的正则规则；五类 topic（application、api-access、sql、audit、security）全部有对应规则。
3. `parsers.conf` 的时间格式与 T-028 输出的 `timestamp` 格式一致。
4. `validate-logging.sh` 增加检查：路由规则使用 `logType`、不再含 message 正则、compose 中应用服务挂载了日志卷。

## 6. 验收标准

- [ ] `docker compose -f deploy/docker-compose.dev.yml config` 无错误。
- [ ] 起 gateway 后卷内出现对应 `.log` 文件（有 Docker 环境时验证）。
- [ ] Fluent Bit 日志无 parse error。
- [ ] Kafka `foodmap.logs.api-access` 有消息（有环境时抽查）。
- [ ] `./harness/scripts/validate-logging.sh` 通过。
- [ ] 无 Docker 环境时必须写明「未执行 + 原因」，不得静默跳过。
- [ ] 文件边界已遵守；不涉及权限与可见范围。

## 7. 汇报格式

```text
完成内容：
修改文件：
验证结果：
- 已执行：
- 未执行：…，原因：…
风险和后续：
```
