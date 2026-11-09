# T-039 mvnw + enforcer + Redisson optional

- 功能点：F-021｜优先级：P1｜波次：W4｜组：F（pom 链第 1 步，建议最先做）｜预估：1d｜依赖：无

## 1. 任务背景

`after/mvnw` 不存在；父 pom 无 enforcer；`foodmap-common` 的 Redisson 非 optional（mybatis、feign、web 都是 optional），导致所有服务被动引入 Netty 与 Redisson。当前父 pom：Java 21、Spring Boot 3.3.7、Redisson 3.52.0。

## 2. 必读文档

`AGENTS.md`、`CODEX-after.md`（Redisson 使用约束）、`.agents/backend-agent.md`。

## 3. 允许修改范围

```text
after/pom.xml
after/foodmap-common/pom.xml
after/mvnw、after/mvnw.cmd、after/.mvn/**（新建）
需要显式引入 Redisson 的服务 pom（如 gateway、auth，需先确认）
```

## 4. 禁止修改范围

```text
after/**/src、front/、deploy/、status/**
```

## 5. 具体交付物

1. Maven Wrapper，固定 Maven 3.9.x。
2. enforcer：Java 21、Maven 3.9+、依赖收敛、禁止发布依赖 SNAPSHOT 的 profile 说明。
3. 固定 surefire、failsafe、jar、resources 插件版本。
4. Redisson 改 optional；先用 `mvn dependency:tree` 确认真正需要 Redis 的服务（denylist、分布式锁）并显式引入，避免启动期缺 Bean。

## 6. 验收标准

- [ ] `./mvnw -v` 可用。
- [ ] enforcer 在 Java 或 Maven 版本不符时使构建失败。
- [ ] 不需要 Redis 的服务依赖树不再含 Redisson 与 Netty。
- [ ] `cd after && ./mvnw -q -B test` 通过。
- [ ] 若 common 自动配置因缺 Redisson 类而失败，停下报告。

## 7. 汇报格式

同 `harness/agent-task-template.md` 第 8 节。
