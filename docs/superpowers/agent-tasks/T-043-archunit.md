# T-043 ArchUnit 分层约束

- 功能点：F-028｜优先级：P2｜波次：W4｜组：F（pom 链）｜预估：1.5d｜依赖：无

## 1. 任务背景

分层规则目前只靠文档和脚本。端口接口直接返回持久化实体、domain 依赖 Spring，违规没有机器拦截。

## 2. 必读文档

`AGENTS.md` 8.1 与 8.5、`CODEX-after.md`、`.agents/qa-agent.md`。

## 3. 允许修改范围

```text
after/foodmap-auth-service/pom.xml、after/foodmap-user-service/pom.xml（仅加 archunit 测试依赖）
after/foodmap-auth-service/src/test/java/**/architecture/**（新建）
after/foodmap-user-service/src/test/java/**/architecture/**（新建）
```

## 4. 禁止修改范围

```text
after/**/src/main、after/pom.xml（如需依赖管理先报告）、front/、deploy/、status/**
```

## 5. 具体交付物

1. 规则：`domain` 不依赖 `infrastructure`；`application` 不依赖 `persistence.entity`；`controller` 不依赖 `mapper`；`controller` 不暴露持久化实体。
2. 当前已违反的规则不能靠删规则通过：使用 ArchUnit 冻结（FreezingArchRule）记录基线，违规数只许减少。
3. 在汇报中列出基线违规清单与数量。

## 6. 验收标准

- [ ] `mvn -q -B test` 通过。
- [ ] 新增违规时测试失败（用一个临时违规验证后撤销）。
- [ ] 基线文件已纳入版本控制。

## 7. 汇报格式

同 `harness/agent-task-template.md` 第 8 节。
