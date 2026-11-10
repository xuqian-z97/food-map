# T-033 后台 RBAC 服务端化

- 功能点：F-020｜优先级：P0｜波次：W2｜组：C（第 3 步）｜预估：2d｜依赖：T-031 已合并；T-025、T-027 已合并（同改 GatewayAuthFilter）

## 1. 任务背景

`AdminPermissionGuard` 直接读取 `X-Admin-User-Id` 与 `X-Admin-Permissions` 请求头判定权限，网关完全不处理 `X-Admin-*`，调用方自带权限头即可通过。权限必须来自服务端数据，网关必须无条件剥除这些头。

## 2. 必读文档

`AGENTS.md`、`CODEX-after.md`（数据库设计约束、管理后台章节）、`skills/foodmap-backend-service/SKILL.md`、`harness/api-contract-checklist.md`。

## 3. 允许修改范围

2026-11-10 主代理评审通过 [T-033 设计稿](../specs/2026-11-10-t033-admin-rbac-implementation.md) 后，文件边界按设计稿 §5 扩展如下：

```text
after/foodmap-admin-service/src/main/java/com/foodmap/admin/**（含 security/controller/dto/service/config/application/infrastructure）
after/foodmap-admin-service/src/main/resources/**（Flyway V2、mapper、profile 配置、开发密钥）
after/foodmap-admin-service/src/test/**
after/foodmap-gateway-service/src/main/java/com/foodmap/gateway/**
after/foodmap-gateway-service/src/main/resources/**（路由、后台验签配置、profile 配置、开发公钥）
after/foodmap-gateway-service/src/test/**
harness/scenarios/t033-admin-rbac.yml（新建）与 admin-rbac-check.py（新建）
harness/scenarios/internal-service-auth-check.py（仅后台正向用例改为登录取得 Token）
harness/verify-dependencies.json（新增 t033 依赖预检）
CODEX-after.md（RBAC 表结构与后台契约同步）
docs/superpowers/agent-tasks/T-033-admin-rbac-server-side.md、docs/superpowers/plans/2026-11-09-backend-hardening.md（边界与状态同步）
```

## 4. 禁止修改范围

```text
after/foodmap-common/**（需要改动时停止并报告）
其他 after 服务、front/、deploy/、status/**（台账由主代理在验收后按 §6.3.1 统一写入）
```

## 5. 具体交付物

1. 网关在认证过滤器入口按大小写不敏感规则无条件剥除所有 `X-Admin-*` 请求头（含同名多值头），与现有 `X-FoodMap-*` 剥除逻辑并列；为 `/api/admin/**` 声明显式路由，只对精确 `POST /api/admin/auth/login` 豁免后台验签，验签通过后透传原 `Authorization`，不生成任何 `X-Admin-*` 头。
2. Flyway `V2__*.sql`：`admin_roles`、`admin_permissions`、`admin_user_roles`、`admin_role_permissions`、`admin_access_sessions`、`admin_operation_logs`，遵守固定字段、业务主键 sequence、部分唯一索引规则，并用 `comment on table` / `comment on column` 写全中文注释；预置 `OPS_VIEWER` 角色、`LOG_ACCESS_READ` 权限及关系。
3. 对应持久化实体（字段级 Javadoc）、标准 Mapper 与 Mapper.xml、复杂 SQL 的 DefineMapper，以及仓储端口与实现；会话认证与授权快照查询从 `jti` 会话行开始校验管理员状态、权限版本与有效权限。
4. 后台登录与登出：扩展密码组件 `matches`（解析现有 `算法$迭代次数$盐$摘要` 格式，常量时间比较，未知账号用占位哈希等成本校验）；独立 RSA RS256 JWT（默认 15 分钟）；登录成功在短事务内锁定账号行、复核状态与哈希、登记会话并写审计；登出按 `jti + adminUserId + 未撤销` 撤销会话。
5. `AdminPermissionGuard` 只接收已验证的 `AuthenticatedAdmin`，权限由服务端按数据库授权判定；后台 MVC 拦截器独立验签并装配请求上下文；登录限流返回 `429`；受控初始化入口用于创建首批管理员。
6. 现有 `LOG_ACCESS_READ` 日志查询能力与 T-032 内部签名链路不得回归。

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
