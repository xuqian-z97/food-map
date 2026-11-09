# T-033 后台 RBAC 服务端化

- 功能点：F-020｜优先级：P0｜波次：W2｜组：C（第 3 步）｜预估：2d｜依赖：T-031 已合并；T-025、T-027 已合并（同改 GatewayAuthFilter）

## 1. 任务背景

`AdminPermissionGuard` 直接读取 `X-Admin-User-Id` 与 `X-Admin-Permissions` 请求头判定权限，网关完全不处理 `X-Admin-*`，调用方自带权限头即可通过。权限必须来自服务端数据，网关必须无条件剥除这些头。

## 2. 必读文档

`AGENTS.md`、`CODEX-after.md`（数据库设计约束、管理后台章节）、`skills/foodmap-backend-service/SKILL.md`、`harness/api-contract-checklist.md`。

## 3. 允许修改范围

```text
after/foodmap-admin-service/src/main/java/com/foodmap/admin/security/**
after/foodmap-admin-service/src/main/java/com/foodmap/admin/controller/**
after/foodmap-admin-service/src/main/java/com/foodmap/admin/application/**
after/foodmap-admin-service/src/main/java/com/foodmap/admin/infrastructure/persistence/**
after/foodmap-admin-service/src/main/resources/db/migration/V2__*.sql（新建）
after/foodmap-admin-service/src/main/resources/mapper/**
after/foodmap-admin-service/src/test/**
after/foodmap-gateway-service/src/main/java/com/foodmap/gateway/filter/GatewayAuthFilter.java（仅增加剥除 X-Admin-* 逻辑）
after/foodmap-gateway-service/src/test/java/com/foodmap/gateway/**
```

## 4. 禁止修改范围

```text
after/foodmap-common/**（需要改动时停止并报告）
其他 after 服务、front/、deploy/、status/**
```

## 5. 具体交付物

1. 网关在认证过滤器入口无条件移除 `X-Admin-User-Id`、`X-Admin-Permissions`，与现有 `X-FoodMap-*` 剥除逻辑并列。
2. Flyway `V2__*.sql`：管理员角色与权限关系表（角色、角色权限、管理员角色），遵守固定字段、业务主键 sequence、部分唯一索引规则，并用 `comment on table` / `comment on column` 写全中文注释。
3. 对应持久化实体（字段级 Javadoc）、标准 Mapper 与 Mapper.xml、仓储端口与实现。
4. `AdminPermissionGuard` 只接收已验证的管理员身份标识，权限由服务端按身份查询；不再接受权限码请求头。管理员身份来源的设计（会话或签发的管理员 Token）若无现成实现，先在汇报中提出方案并停止，等主代理确认，不要自行扩大范围。
5. 现有 `LOG_ACCESS_READ` 权限能力不得回归。

## 6. 验收标准

- [ ] 客户端自带 `X-Admin-Permissions: LOG_ACCESS_READ` 无法通过校验。
- [ ] 网关剥除 `X-Admin-*` 有单测断言。
- [ ] 合法管理员可访问日志查询入口，无权限管理员返回 403，未登录返回 401。
- [ ] Flyway 迁移可在空库执行，表和字段均有中文注释。
- [ ] `cd after && mvn -q -B -pl foodmap-admin-service,foodmap-gateway-service -am test` 通过。
- [ ] 文件边界已遵守；CODEX-after.md 需同步 RBAC 表结构，汇报中列出表与字段清单供主代理补写。

## 7. 汇报格式

```text
完成内容：
修改文件：
验证结果：
- 已执行：
- 未执行：…，原因：…
风险和后续：
```
