# T-042 AuthServiceImpl 按用例拆分

- 功能点：F-028｜优先级：P2｜波次：W4｜组：G（第 2 步）｜预估：2d｜依赖：T-038 已合并（集成测试兜底）

## 1. 任务背景

`AuthServiceImpl` 743 行，承担注册、登录、刷新、登出、身份绑定、会话、登录日志与拒绝名单。仓库有 11 处 `@Deprecated`，新旧身份模型双写长期留存。

## 2. 必读文档

`AGENTS.md`、`CODEX-after.md`、`skills/foodmap-backend-service/SKILL.md`、T-038 汇报。

## 3. 允许修改范围

```text
after/foodmap-auth-service/src/main/java/com/foodmap/auth/**
after/foodmap-auth-service/src/test/**
```

## 4. 禁止修改范围

```text
after/foodmap-common、其他 after 服务、front/、deploy/、status/**
Flyway 迁移脚本（需要改表时停下报告）
```

## 5. 具体交付物

1. 拆分为 `RegisterUseCase`、`LoginUseCase`、`SessionUseCase`、`IdentityBindingUseCase`，`AuthServiceImpl` 仅做门面委托。
2. 单个类不超过 300 行；方法级 Javadoc 与事务边界保持（register、login 现有 `@Transactional` 语义不得变化）。
3. `@Deprecated` 的双写代码要么移除，要么写明退出条件与 issue 编号。
4. 外部行为（接口、响应、日志事件名）保持不变。

## 6. 验收标准

- [ ] 全部现有测试与 T-038 集成测试通过，不得删除或弱化断言。
- [ ] `node tools/verify/cli.mjs run harness/scenarios/` 在有环境时通过。
- [ ] 每个新类有类级 Javadoc。
- [ ] 无新的跨层依赖。

## 7. 汇报格式

同 `harness/agent-task-template.md` 第 8 节。
