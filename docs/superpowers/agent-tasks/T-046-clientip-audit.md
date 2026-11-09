# T-046 clientIp 掩码与审计事件

- 功能点：F-029｜优先级：P2｜波次：W4｜组：D 延伸｜预估：1.5d｜依赖：T-028 已合并

## 1. 任务背景

`ApiAccessLogFilter` 把完整 clientIp 写入日志，`ApiAccessLogEventParser` 直接把它存进 `client_ip_masked` 字段，名不副实。文档要求关键动作记录审计日志，但代码里没有审计事件生产者，Fluent Bit 的 audit 与 security 路由没有对应日志。

## 2. 必读文档

`AGENTS.md` 8.5（日志与审计条款）、`CODEX-after.md`、T-028 任务单与其汇报。

## 3. 允许修改范围

```text
after/foodmap-common/src/main/java/com/foodmap/common/logging/ApiAccessLogFilter.java
after/foodmap-common/src/main/java/com/foodmap/common/logging/AuditLog*.java（新建）
after/foodmap-common/src/test/java/com/foodmap/common/logging/**
after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/messaging/ApiAccessLogEventParser.java
after/foodmap-log-service/src/test/**
```

## 4. 禁止修改范围

```text
SafeLog、LogMdcKeys 的既有语义（属 T-028，仅可使用）、业务服务调用点、front/、deploy/、status/**
```

## 5. 具体交付物

1. clientIp 在写日志前掩码（IPv4 保留前两段，IPv6 保留前缀），字段名与语义一致；解析端不得再回退到未掩码字段。
2. 统一 `AuditLog` 接口：actor、action、resource、result、requestId，`logType=audit`。
3. 在登录、登出、管理员操作的调用点接入审计的计划写入汇报；调用点改动属各服务任务，若需改动先请求主代理授权。
4. 审计内容只含动作事实、业务主键与脱敏摘要。

## 6. 验收标准

- [ ] `client_ip_masked` 实际为掩码值（有测试）。
- [ ] `AuditLog` 输出的 JSON 含 `logType=audit`。
- [ ] 日志无 Token、密码、完整手机号。
- [ ] `mvn -q -B test` 通过。

## 7. 汇报格式

同 `harness/agent-task-template.md` 第 8 节。
