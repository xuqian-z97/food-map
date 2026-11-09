# T-028 统一 logback JSON 与文件落盘

- 功能点：F-019｜优先级：P0｜波次：W1｜组：D（第 1 步）｜预估：1.5d｜依赖：无

## 1. 任务背景

全仓库没有 `logback-spring.xml`，也没有 `logging.file.name`，日志只进控制台。Fluent Bit 的 parser 是 `Format json`，而 `SafeLog` 输出 `eventName key=value` 纯文本，即使落盘也解析不出字段。本任务让所有服务输出单行 JSON 并写入 `/foodmap/logs/{service}.log`。

## 2. 必读文档

`AGENTS.md`、`CODEX-after.md`（日志章节）、`.agents/backend-agent.md`、`skills/foodmap-backend-service/SKILL.md`、`harness/acceptance-checklist.md`、方案文档 W1 小节。

## 3. 允许修改范围

```text
after/foodmap-common/pom.xml
after/foodmap-common/src/main/resources/logback-common.xml（新建）
after/foodmap-common/src/main/java/com/foodmap/common/logging/LogMdcKeys.java
after/foodmap-common/src/main/java/com/foodmap/common/logging/SafeLog.java
after/foodmap-common/src/main/java/com/foodmap/common/logging/LogContext.java
after/foodmap-common/src/test/java/com/foodmap/common/logging/**
after/foodmap-*-service/src/main/resources/logback-spring.xml（各服务新建，仅 include 公共模板）
```

## 4. 禁止修改范围

```text
front/、deploy/、harness/、status/**
after/*/src/main/java（除上面列出的 common logging 三个类）
CODEX-*.md、AGENTS.md
```

## 5. 具体交付物

1. `foodmap-common` 引入 `logstash-logback-encoder`（版本固定，写在 after/pom.xml 的依赖管理由主代理决定；若需改父 pom 先停下报告）。
2. `logback-common.xml`：JSON encoder，自动输出 `timestamp`、`level`、`logger`、`message`、`requestId`、`traceId`、`spanId`、`serviceName`、`logType`。
3. `LogMdcKeys` 新增 `LOG_TYPE`；取值：`application`、`api-access`、`sql`、`audit`、`security`，默认 `application`。SafeLog 与现有访问日志、SQL 日志调用点写入对应 `logType`，不得靠 message 文本区分。
4. 文件 appender 输出 `/foodmap/logs/${spring.application.name}.log`，按天加大小滚动；保留控制台 appender。
5. 路径不可写时（本地 IDEA 启动）降级为仅控制台，不得导致服务启动失败。
6. 脱敏规则不变，JSON 中不得出现 Token、密码、完整手机号。

## 6. 验收标准

- [ ] 每个服务输出单行合法 JSON，可被 `jq` 解析。
- [ ] JSON 含 requestId / traceId / serviceName / logType / timestamp / level。
- [ ] 访问日志的 `logType` 为 `api-access`，SQL 日志为 `sql`。
- [ ] 路径不可写时服务仍能启动。
- [ ] `cd after && mvn -q -B test` 通过，`SafeLogTest`、`LogMdcFilterTest` 不回归。
- [ ] 文件边界已遵守；是否需同步 CODEX-after.md 已在汇报中说明。
- [ ] 本任务不涉及权限、可见范围、PUBLIC 统计。

## 7. 汇报格式

```text
完成内容：
修改文件：
验证结果：
- 已执行：
- 未执行：…，原因：…
风险和后续：
```
