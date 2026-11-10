# T-025 Token 密钥统一注入与 fail-fast

- 功能点：F-024｜优先级：P0｜波次：W0｜组：B（第 1 步）｜预估：0.5d｜依赖：无

## 1. 任务背景

`HmacTokenIssuer` 标注 `@Component`，Spring 会选择无参构造器，密钥恒为硬编码常量 `foodmap-local-token-secret-please-change`；认证服务源码中没有任何 `@Value` 读取 `foodmap.security.token-secret`。结论是：**无论运维如何配置环境变量，认证服务都用默认密钥签发**。网关则从配置读取该变量，只要网关配置了密钥就必然签验不一致，登录链路不可用。`deploy/env.example` 写的是 `JWT_SECRET`，代码从未读取。

`Pbkdf2PasswordHashService`（auth）与 `Pbkdf2AdminPasswordHashService`（admin）的 pepper 同样是硬编码默认值。

## 2. 必读文档

`AGENTS.md`（8.2 部署约束与 profile 优先级、8.5 统一异常与日志脱敏）、`CODEX-after.md`、`.agents/backend-agent.md`、`skills/foodmap-backend-service/SKILL.md`、`docs/superpowers/plans/2026-11-09-backend-hardening.md` 的 Wave 0 小节。

## 3. 允许修改范围

```text
after/foodmap-auth-service/src/main/java/com/foodmap/auth/domain/HmacTokenIssuer.java
after/foodmap-auth-service/src/main/java/com/foodmap/auth/domain/Pbkdf2PasswordHashService.java
after/foodmap-auth-service/src/main/java/com/foodmap/auth/config/**（可新建配置类）
after/foodmap-auth-service/src/main/resources/application.yml
after/foodmap-auth-service/src/main/resources/application-local.yml
after/foodmap-auth-service/src/main/resources/application-orbstack.yml
after/foodmap-auth-service/src/main/resources/application-prod.yml
after/foodmap-auth-service/src/test/**
after/foodmap-gateway-service/src/main/java/com/foodmap/gateway/filter/GatewayAuthFilter.java
after/foodmap-gateway-service/src/main/resources/application.yml
after/foodmap-gateway-service/src/main/resources/application-prod.yml
after/foodmap-gateway-service/src/test/**
after/foodmap-admin-service/src/main/java/com/foodmap/admin/service/Pbkdf2AdminPasswordHashService.java
after/foodmap-admin-service/src/main/resources/application-prod.yml
after/foodmap-admin-service/src/test/**
deploy/env.example
```

## 4. 禁止修改范围

```text
after/foodmap-common/**（含 pom 与 logging 包，正被 T-028 并行修改）
after/foodmap-gateway-service/src/main/java 下的其它类（含 GatewayTraceFilter）
其它 after 服务、front/、status/**、CODEX-*.md、AGENTS.md、harness/**
不得 git commit、不得 git push，提交由主代理统一执行
```

## 5. 具体交付物

1. 新增 `TokenSecretProperties`（`@ConfigurationProperties(prefix = "foodmap.security")`），字段 `tokenSecret`，校验非空且长度 >= 32；校验失败直接阻止启动。
2. `HmacTokenIssuer` 改为构造器注入该配置，**删除无参构造器与硬编码默认值**；保留接受 `String secret` 的构造器供测试使用。
3. `Pbkdf2PasswordHashService` 的 pepper 改为必须注入，删除硬编码默认值；admin 的 `Pbkdf2AdminPasswordHashService` 的 `@Value` 默认值同样删除。
4. profile 策略：`local` / `orbstack` 允许保留开发用默认值（不对外，便于本机启动）；**`prod` 不得有任何默认值**，缺失即启动失败。
5. `deploy/env.example`：删除 `JWT_SECRET`，改为 `FOODMAP_TOKEN_SECRET`；补充 `AUTH_PEPPER`、`ADMIN_PEPPER`；补充缺失的 `ADMIN_DB_USERNAME`、`ADMIN_DB_PASSWORD`、`ADMIN_LOG_SERVICE_BASE_URL`（如这些变量在 admin 的 prod profile 中被引用）。
6. 补单测：缺失 secret 抛异常、长度不足 32 抛异常、合法 secret 正常构造、auth 与 gateway 使用同一配置键。

## 6. 验收标准

- [ ] `grep -r "foodmap-local-token-secret" after/` 在生产代码中 0 命中（测试除外，若测试需要固定值请用独立常量）。
- [ ] auth 与 gateway 引用同一配置键 `foodmap.security.token-secret` / 环境变量 `FOODMAP_TOKEN_SECRET`。
- [ ] prod profile 缺变量时启动失败，错误信息明确指出缺失变量名。
- [ ] secret 短于 32 字符时启动失败。
- [ ] `cd after && mvn -q -B test` 通过，`GatewayAuthFilterTest` 与 `HmacTokenCodecTest` 不回归。
- [ ] 本地 `local` profile 仍可启动（不要破坏本机开发体验）。
- [ ] 文件边界已遵守；CODEX-after.md 需同步的密钥变量说明在汇报中列出，由主代理补写。
- [ ] 本任务不涉及可见范围与 PUBLIC 统计。

## 7. 汇报格式

```text
完成内容：
修改文件：
验证结果：
- 已执行：
- 未执行：…，原因：…
风险和后续：
```

说明：本地已运行的服务使用旧代码，改动需重启才能生效。`node tools/verify/cli.mjs run harness/scenarios/` 由主代理在重建服务后执行，子代理不要为通过验证而修改断言。
