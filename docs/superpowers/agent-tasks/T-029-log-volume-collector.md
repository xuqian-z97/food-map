# T-029 应用服务挂载日志卷并对齐采集

- 功能点：F-019｜优先级：P0｜波次：W1｜组：D（第 2 步）｜预估：1.5d（含解析器修复）｜依赖：T-028 已合并

## 1. 任务背景

`foodmap-app-logs` 卷只挂给了 Fluent Bit，没有任何应用服务挂载；`rewrite_tag` 用正则匹配 message 文本路由，事件名一改就静默失效。

更严重的是 **T-028 已把日志改成单行 JSON，采集链下游出现断裂**，本任务必须一并修复：

1. `ApiAccessLogEventParser` 从 Kafka 取到 JSON 后，`log` 字段是一整行 JSON（紧凑无空格），`extractEventName` 会把整串当作事件名，写入 `event_name varchar(128)` 时溢出。
2. `method` / `path` / `status` / `durationMs` 仍在 `message` 文本（`eventName k=v k=v`）里，不是 JSON 顶层字段，当前代码不解析 `message`，全部退化为默认值（`UNKNOWN` / `/unknown` / `0`）。
3. Fluent Bit `parsers.conf` 的 `Time_Format %Y-%m-%dT%H:%M:%S.%L%z` 与 logback 输出的 `+08:00` 偏移格式可能不匹配（glibc `%z` 通常只接受 `+0800`）。

改完后必须保证：一次登录能在 ES 按 requestId 查到日志，且 `api_access_log` 里有正确的 `event_name`、`method`、`path`、`status`、`duration_ms`。

## 2. 必读文档

`AGENTS.md` 6.3 与 6.3.1、`CODEX-after.md`（日志章节）、`harness/file-boundary-rules.md`、`deploy/logging/` 现有配置、T-028 任务单与其汇报、`docs/superpowers/plans/2026-11-09-backend-hardening.md` Wave 1。

## 3. 允许修改范围

```text
deploy/docker-compose.dev.yml
deploy/docker-compose.ecs2.yml（只加日志卷挂载，不补应用服务，补服务属 T-035）
deploy/logging/fluent-bit/fluent-bit.conf
deploy/logging/fluent-bit/parsers.conf
deploy/logging/logstash/pipeline/foodmap-logs.conf（仅当字段路由需要）
harness/scripts/validate-logging.sh
after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/messaging/ApiAccessLogEventParser.java
after/foodmap-log-service/src/test/java/com/foodmap/log/infrastructure/messaging/**
```

## 4. 禁止修改范围

```text
after/foodmap-common/**（T-028 已完成，不得再改 SafeLog / logback 模板）
其他 after 服务、after/foodmap-log-service 的 controller/service/persistence
front/、status/**、CODEX-*.md、AGENTS.md、harness/scenarios/**
不得 git commit、不得 git push，提交由主代理统一执行
```

## 5. 具体交付物

### A. 采集配置

1. dev 与 ecs2 compose 中，凡是应用服务（已存在的 gateway 等）均挂载 `foodmap-app-logs:/foodmap/logs:rw`；Fluent Bit 保持 `:ro`。
2. `fluent-bit.conf`：`rewrite_tag` 改为按 JSON 字段 `logType` 路由，删除基于 message 文本的正则规则；五类 topic（application、api-access、sql、audit、security）全部有对应规则。
3. `parsers.conf`：`Time_Format` 与 logback 实际 timestamp 格式对齐。若 glibc `%z` 不接受 `+08:00`，按下列任一方式解决并在汇报中说明选择：改用不带冒号的偏移格式；或改用 Fluent Bit 支持的时间解析方式；或保留 `timestamp` 为普通字段、以采集时间作为事件时间。
4. `validate-logging.sh` 增加检查：路由规则使用 `logType`、不再含 message 正则、compose 中应用服务挂载了日志卷、parser 时间格式与 logback 一致。

### B. 解析器修复（核心）

5. `ApiAccessLogEventParser` 字段解析优先级改为：**JSON 顶层字段 > `message` 文本键值对 > `log` 原文**。
6. `eventName` 提取优先级：顶层 `eventName` > `message` 首个 token（空格分隔）> 默认值；**必须有长度保护**，超过 `event_name` 列宽时截断，绝不允许整行 JSON 写入。
7. `method` / `path` / `status` / `durationMs` 从 `message` 文本解析，确保写库为真实值而非默认值。
8. 保持向后兼容：纯文本 `log` 载荷仍能正确解析（现网可能有旧格式在途消息）。
9. 用 T-028 实际产出的 JSON 行作为测试夹具，断言 `eventName`、`method`、`path`、`status`、`durationMs`、`requestId`、`traceId`、`serviceName`、`logType` 全部正确。

## 6. 验收标准

- [ ] 用真实 JSON 日志行做单测，解析出的 `event_name` 等于 `api.access.completed`，不是整行 JSON。
- [ ] `method` / `path` / `status` / `durationMs` 为真实值，不是默认值。
- [ ] 超过列宽的 eventName 被截断，不产生入库异常。
- [ ] 旧纯文本格式仍可解析（有测试）。
- [ ] `docker compose -f deploy/docker-compose.dev.yml config` 无错误。
- [ ] `./harness/scripts/validate-logging.sh` 通过。
- [ ] `cd after && mvn -q -B -pl foodmap-log-service -am test` 通过；本地 Maven 仓库不可写时使用 `-Dmaven.repo.local=/tmp/foodmap-t028-m2repo`。
- [ ] 有 Docker 环境时验证：卷内出现 `.log` 文件、Fluent Bit 无 parse error、Kafka `foodmap.logs.api-access` 有消息；无环境必须写明「未执行 + 原因」。
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
