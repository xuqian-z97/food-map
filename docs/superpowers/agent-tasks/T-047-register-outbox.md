# T-047 注册流程 Outbox 最终一致性

- 功能点：F-030｜优先级：P1｜波次：W4｜组：G（第 3 步）｜预估：2.5d｜依赖：T-038 已合并；T-032 已合并（用户开通接口带内部签名）

## 1. 任务背景

`AuthServiceImpl.register` 在 `@Transactional` 内同步调用 Feign 开通用户，持有数据库连接等待跨服务调用；用户服务已提交而 auth 回滚会留下孤儿用户，没有补偿。违反 CODEX-after.md 的 Saga、Outbox 与短事务约束。

## 2. 必读文档

`AGENTS.md` 8.5（跨服务事务、锁与短持有条款）、`CODEX-after.md`、`skills/foodmap-backend-service/SKILL.md`、T-038、T-042 汇报。

## 3. 允许修改范围

```text
after/foodmap-auth-service/src/main/java/com/foodmap/auth/**
after/foodmap-auth-service/src/main/resources/db/migration/V6__*.sql（新建）
after/foodmap-auth-service/src/main/resources/mapper/**
after/foodmap-auth-service/src/test/**
after/foodmap-user-service/src/main/java/com/foodmap/user/controller/InternalUserProvisionController.java
after/foodmap-user-service/src/main/java/com/foodmap/user/service/**（仅幂等开通）
after/foodmap-user-service/src/test/**
```

## 4. 禁止修改范围

```text
after/foodmap-common（需要公共事件接口时停下报告）、front/、deploy/、status/**
```

## 5. 具体交付物

1. Flyway 新增 Outbox 表，含固定字段、业务主键 sequence、中文表与字段注释。
2. 注册改为：本地事务写账号、凭证、身份绑定与 Outbox 记录；事务内不再有跨服务同步调用。
3. 异步投递器带重试与退避；用户开通接口按 `userId` 幂等，重复请求返回同一结果。
4. 失败状态机 `PENDING → PROVISIONING → ACTIVE → FAILED → COMPENSATING → COMPENSATED`，失败有告警日志与人工处理入口（至少内部查询接口或 SQL 说明）。
5. 对客户端的注册响应语义变化（如用户资料暂未就绪）必须在汇报中说明，并先与主代理确认，因为会影响 iOS 注册后的读取流程。

## 6. 验收标准

- [ ] 用户服务不可用时注册不产生孤儿数据，状态可查。
- [ ] 重复开通返回同一结果。
- [ ] 补偿任务可重试 FAILED 记录。
- [ ] 事务内无 Feign 调用（有测试或 ArchUnit 规则）。
- [ ] T-038 集成测试与 `verify.sh` 注册链路场景通过。

## 7. 汇报格式

同 `harness/agent-task-template.md` 第 8 节。
