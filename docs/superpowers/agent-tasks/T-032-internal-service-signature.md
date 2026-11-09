# T-032 内部接口服务身份签名

- 功能点：F-025｜优先级：P0｜波次：W2｜组：C（第 2 步）｜预估：2d｜依赖：T-031 已合并

## 1. 任务背景

`/internal/users/provision` 与 `/internal/logs/api-access` 没有任何认证，同网段任何容器都能直连调用，网关只挡外部。需要让内部调用带上服务身份。本任务横跨 5 个模块，必须由单个子代理串行完成。

## 2. 必读文档

`AGENTS.md`、`CODEX-after.md`（内部接口与 Redis Key 规范）、`.agents/backend-agent.md`、`skills/foodmap-backend-service/SKILL.md`、`skills/foodmap-api-contract/SKILL.md`。

## 3. 允许修改范围

```text
after/foodmap-common/src/main/java/com/foodmap/common/security/InternalService*.java（新建）
after/foodmap-common/src/main/java/com/foodmap/common/feign/**（仅为注入签名拦截器）
after/foodmap-common/src/test/java/com/foodmap/common/security/**
after/foodmap-user-service/src/main/java/com/foodmap/user/controller/InternalUserProvisionController.java
after/foodmap-log-service/src/main/java/com/foodmap/log/controller/LogApiAccessLogController.java
after/foodmap-auth-service/src/main/java/com/foodmap/auth/infrastructure/client/user/FeignUserProfileProvisionClient.java
after/foodmap-admin-service/src/main/java/com/foodmap/admin/infrastructure/client/**
各服务 src/test 下对应的新增测试
deploy/env.example（仅新增 FOODMAP_INTERNAL_SECRET）
```

## 4. 禁止修改范围

```text
after/foodmap-gateway-service/**（网关不参与内部签名）
HmacTokenCodec.java（属 T-026）
front/、status/**、CODEX-*.md
```

## 5. 具体交付物

1. `InternalServiceSignature`：对 `serviceName + timestamp + nonce + method + path + bodyHash` 做 HMAC-SHA256；密钥来自 `FOODMAP_INTERNAL_SECRET`，生产缺失即启动失败，不设默认值。
2. Feign 请求拦截器自动写入 `X-FoodMap-Service`、`X-FoodMap-Timestamp`、`X-FoodMap-Nonce`、`X-FoodMap-Signature`。
3. 服务端校验组件：时间窗不超过 5 分钟；nonce 通过统一 Redis 封装去重，Key 遵守 `foodmap:{service}:{biz}:{version}:{key}`，TTL 10 分钟；Redis 不可用时 fail-closed 返回 401。
4. 调用方白名单：`/internal/users/provision` 仅允许 `foodmap-auth-service`；`/internal/logs/**` 仅允许 `foodmap-admin-service`。
5. 签名比较使用常量时间比较；日志中不得输出签名与密钥。

## 6. 验收标准

- [ ] 无签名头的内部请求返回 401。
- [ ] 时间戳超过 5 分钟返回 401。
- [ ] 重放同一 nonce 返回 401。
- [ ] 非白名单服务调用返回 403。
- [ ] 注册流程（auth 调 user provision）仍成功；管理后台日志查询链路仍可用。
- [ ] 上述四个拒绝分支均有单测。
- [ ] `cd after && mvn -q -B test` 通过。
- [ ] 有环境时执行 `node tools/verify/cli.mjs run harness/scenarios/`。
- [ ] 文件边界已遵守；CODEX-after.md 需同步内部接口鉴权说明，汇报中列出需主代理补写的要点。

## 7. 汇报格式

```text
完成内容：
修改文件：
验证结果：
- 已执行：
- 未执行：…，原因：…
风险和后续：
```
