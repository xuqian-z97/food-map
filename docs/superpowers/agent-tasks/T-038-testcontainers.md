# T-038 Testcontainers 集成测试

- 功能点：F-021｜优先级：P1｜波次：W4｜组：G（第 1 步）｜预估：3d｜依赖：T-026 已合并

## 1. 任务背景

118 个测试均不连真实数据库。Flyway 迁移、Mapper XML、唯一索引、事务回滚没有自动化验证。T-042、T-047 依赖本任务兜底。

## 2. 必读文档

`AGENTS.md`、`CODEX-after.md`（数据库约束）、`skills/foodmap-backend-service/SKILL.md`、`.agents/qa-agent.md`。

## 3. 允许修改范围

```text
after/pom.xml（仅 Testcontainers 依赖管理与 failsafe 插件；与其他 pom 任务冲突时先停下）
after/foodmap-auth-service/pom.xml
after/foodmap-user-service/pom.xml
after/foodmap-auth-service/src/test/**（新增 *IT.java）
after/foodmap-user-service/src/test/**（新增 *IT.java）
```

## 4. 禁止修改范围

```text
after/**/src/main、front/、deploy/、status/**
```

## 5. 具体交付物

1. PostgreSQL Testcontainers 基类，执行真实 Flyway 迁移。
2. auth 与 user 的 Repository 读写测试。
3. 事务回滚测试：多表写入中途失败后数据未落库。
4. 唯一索引冲突测试，断言 `DuplicateKeyException`；当前尚未映射为 409 的，用测试记录现状，不要改生产代码。
5. 使用 failsafe 跑 `*IT`，`mvn verify` 执行；无 Docker 时明确跳过而非静默通过。

## 6. 验收标准

- [ ] `cd after && mvn -B verify` 在有 Docker 时通过。
- [ ] 迁移脚本在空库执行成功。
- [ ] 回滚与冲突场景有断言。
- [ ] 测试不依赖手工环境。
- [ ] 现有单测不回归，不得删除测试。

## 7. 汇报格式

同 `harness/agent-task-template.md` 第 8 节。
