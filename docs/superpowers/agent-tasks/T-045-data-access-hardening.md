# T-045 数据访问加固

- 功能点：F-029｜优先级：P2｜波次：W4｜组：F（建议在 T-038 之后）｜预估：1.5d｜依赖：无（有 T-038 更稳）

## 1. 任务背景

`UserRepositoryImpl.save` 先 update 再 insert，并发下抛 `DuplicateKeyException` 变成 500；`AuthAccountDefineMapper.xml` 的登录标识查询跨列 `or` 且无排序；`selectListByCondition` 无 limit；`LIKE` 未转义 `%`、`_`。

## 2. 必读文档

`AGENTS.md` 8.5（Mapper 规范）、`skills/foodmap-backend-service/SKILL.md`。

## 3. 允许修改范围

```text
after/foodmap-user-service/src/main/java/com/foodmap/user/infrastructure/persistence/**
after/foodmap-user-service/src/main/resources/mapper/**
after/foodmap-auth-service/src/main/resources/mapper/**
after/foodmap-auth-service/src/main/java/com/foodmap/auth/infrastructure/persistence/**
对应 src/test/**
```

## 4. 禁止修改范围

```text
Flyway 迁移（需要加索引时停下报告）、AuthServiceImpl（属 T-042）、front/、deploy/、status/**
```

## 5. 具体交付物

1. upsert 改 `ON CONFLICT` 或捕获 `DuplicateKeyException` 并映射为 409 业务异常。
2. 登录标识查询按账号名、手机号、邮箱类型显式区分，结果确定。
3. 动态列表查询强制 limit 与上限。
4. LIKE 查询转义 `%`、`_`、`\`。

## 6. 验收标准

- [ ] 并发写入不再产生 500。
- [ ] 输入 `%` 不再匹配全部记录（有测试）。
- [ ] 列表查询带 limit。
- [ ] 现有测试不回归；有 T-038 时补集成断言。

## 7. 汇报格式

同 `harness/agent-task-template.md` 第 8 节。
