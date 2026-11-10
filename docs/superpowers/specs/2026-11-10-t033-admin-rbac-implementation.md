# T-033 管理后台 RBAC 代码实现设计

2026-11-10 · 评审稿 · 对应 F-020「管理后台」

本方案沿用 FoodMap 的独立管理员账号和后台 Access Token 约定。首个交付切片是用 `admin_users` 中的账号登录，经网关查询访问日志，再验证登出后原 Token 失效。每位管理员有独立的 `adminUserId`，可配置一个或多个角色，授权事实保存在 `foodmap_admin_db`。

## 1. 现状与交付边界

| 组件 | 当前实现 | 本次改造 |
| --- | --- | --- |
| 管理员账号 | V1 已建立 `admin_users`。`AdminUserService` 能创建账号，密码组件只提供 `hash` | 复用原有账号、密码格式和 `permission_version`，补密码校验、后台登录和登出 |
| 网关 | 现有过滤器对受保护的 `/api/**` 解析 App Token，路由表尚无 `/api/admin/**` | 增加显式后台路由和后台验签分支，在网关入口无条件清理 `X-Admin-*` |
| 后台权限 | `AdminPermissionGuard` 读取身份头与权限头 | 用验签后的管理员身份查会话与角色关系，按当前数据库授权判定 |
| 日志查询 | `AdminLogController` 通过 `HttpLogApiAccessLogClient` 签名调用日志服务 | 保留现有查询契约及 T-032 内部签名，权限通过后才发起内部调用 |

交付范围包含后台登录、登出、日志只读、受控账号初始化和审计。后台账号管理页面、在线角色管理接口、刷新 Token 和其他后台业务接口安排在后续切片。

来源：[后端架构](../../../CODEX-after.md#510-管理后台服务) · [T-033 任务卡](../agent-tasks/T-033-admin-rbac-server-side.md) · [后端加固计划](../plans/2026-11-09-backend-hardening.md#t-033-后台-rbac-服务端化)

## 2. 后台接口契约

接口均由 `foodmap-admin-service` 提供，经网关访问。复用 `ApiResponse<T>` 的 `success`、`status`、`code`、`message`、`data`。成功为 `200/OK`，失败时 HTTP 状态与响应 `status` 一致，`data` 为 `null`。

| 方法与路径 | 请求 | 成功响应 `data` |
| --- | --- | --- |
| `POST /api/admin/auth/login` | JSON `username`、`password`，均必填。用户名最长 64 字符并沿用创建账号的规范化规则。密码原样校验并限制请求体大小 | `accessToken`、`tokenType`（`Bearer`）、`expiresTime`（带时区的 ISO 8601 时间）。设置 `Cache-Control: no-store` |
| `POST /api/admin/auth/logout` | `Authorization: Bearer <admin-access-token>`，无请求体 | `null`。响应成功后，同一 Token 再访问保护接口返回 `401` |
| `GET /api/admin/logs/api-access` | 同上，需 `LOG_ACCESS_READ`。保留 `requestId`、`traceId`、`serviceName`、`logLevel`、`httpStatus`、`occurredFrom`、`occurredTo`、`pageIndex`、`pageSize` | 保留 `PageResponse<AdminApiAccessLogResponse>`，字段为 `items`、`total`、`pageIndex`、`pageSize`、`hasMore`。页码从 0 起，现有日志服务默认每页 20 条、上限 100 条 |

| HTTP / `code` | 适用情况 |
| --- | --- |
| `400 / BAD_REQUEST` | JSON 或请求参数校验失败 |
| `401 / UNAUTHORIZED` | 登录凭据错误、账号停用或锁定，或受保护请求的 Token 缺失、验签失败、过期、已撤销、权限版本过期。登录失败统一提示，避免暴露账号是否存在 |
| `403 / FORBIDDEN` | 后台身份有效，但当前权限不足 |
| `429 / TOO_MANY_REQUESTS` | 后台登录触发限流 |
| `503 / SERVICE_UNAVAILABLE` | 管理后台的身份、会话或权限数据源无法读取或写入。请求失败并留下脱敏排查信息 |

后台 Token 只通过 `Authorization` 传输。登录 DTO 的调试输出应屏蔽密码，登录响应的 `accessToken` 也不进入访问日志、响应体采集或测试证据。日志查询继续沿用原有下游错误映射。

## 3. 登录、验证与授权

### 3.1 管理员登录和 Token

1. `AdminAuthServiceImpl` 按后台用户名查账号。扩展 `AdminPasswordHashService.matches`，解析现有 `算法$迭代次数$盐$摘要` 格式，复用 `ADMIN_PEPPER`，用常量时间比较摘要。只支持已知算法和受控迭代次数，异常格式按校验失败处理。未知账号使用预生成的占位哈希执行等成本校验。
2. 密码计算放在数据库事务外。成功后在短事务内锁定账号行，复核账号状态及密码哈希，读取当前角色与权限版本，签发 JWT，更新 `last_login_time` 并登记 Token 会话与成功审计。签发或写入失败则回滚，事务提交后才向客户端返回 Token。已有哈希保留兼容性，登录失败审计独立于成功事务。短事务通过独立事务入口或 `TransactionTemplate` 建立，避免同类内部调用绕过事务代理。
3. 拟采用 Spring Security JOSE 的 `NimbusJwtEncoder` 和 `NimbusJwtDecoder`。后台使用独立 RSA 密钥对，固定允许 `RS256`。后台服务持有私钥，网关只配置公钥。密钥以外部文件或密钥系统注入，生产缺失或无法解析时拒绝启动，测试用临时密钥。

| JWT 字段 | 拟定值或含义 |
| --- | --- |
| `iss` / `aud` / `typ` | `foodmap-admin-service` / `foodmap-admin-api` / `foodmap-admin+jwt`，验签时匹配后台专用值 |
| `sub` | `adminUserId` 的十进制字符串，解析后必须是正 `bigint` |
| `roles` | 签发时的角色编码列表，允许为空。接口授权以数据库中当前角色和权限为准 |
| `permissionVersion` | 签发时的 `admin_users.permission_version` |
| `iat` / `exp` / `jti` | 签发时间、过期时间、随机唯一标识。拟定有效期 15 分钟，可配置。两端使用一致的过期判定 |

两端用同一份签名测试向量验证算法、签名、签发方、受众、类型和必填声明。后台 JWT 与 App Token 的验签入口相互独立。JOSE 依赖在 admin、gateway 两个模块中使用父 POM 的 Spring Boot BOM 版本，合入前用 Maven 依赖树和签名测试确认兼容性。

拟定配置前缀 `foodmap.admin.token`。两服务都读取 `public-key-location`，只有 admin 读取 `private-key-location` 和 `ttl`。示例配置只放环境变量名与文件路径占位，`local`、`orbstack` 使用开发环境生成的独立密钥，`prod` 由部署配置注入。首版单密钥轮换时，协调切换两端配置并让管理员重新登录。

参考：[RFC 8725 §3.12](https://www.rfc-editor.org/rfc/rfc8725.html#section-3.12) · [Spring Security JWT 验签](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)

### 3.2 网关与后台的校验顺序

1. `GatewayAuthFilter` 在任何公开、内部或受保护路径判断之前，按大小写不敏感规则清理所有 `X-Admin-*` 请求头，包括同名多值头。保持原有普通用户身份头清理和 `/internal/**` 拦截。
2. `application.yml` 为 `/api/admin/**` 声明显式路由，目标为 `lb://foodmap-admin-service`。在普通 App Token 分支之前处理后台路由，只对精确的 `POST /api/admin/auth/login` 豁免后台验签。保护路径只接受单个 Bearer 凭证，验签后透传原 `Authorization`。网关不生成 `X-Admin-*` 身份或权限头。
3. `admin-service` 在 MVC 拦截器内独立验签，通过应用服务一次读取当前会话、管理员状态、权限版本及有效权限。将查询结果构造为服务端请求上下文 `AuthenticatedAdmin`，供 Controller 从请求属性读取。由 MVC 统一异常处理器转换 `401/403/503`。
4. `AdminLogController` 调用 `AdminPermissionGuard.requirePermission(admin, LOG_ACCESS_READ)`。权限有效后才调用日志查询 Service，沿用内部签名和链路头。后端直连请求也必须走相同校验。查询时的数据库读事务在调用日志服务前结束。

用于评审的核心接口示意，类和方法的完整注释在实现时补齐。

```java
public interface AdminAuthService {
    AdminLoginResponse login(AdminLoginRequest request);
    void logout(AuthenticatedAdmin admin);
}

public interface AdminAuthorizationService {
    AuthenticatedAdmin authenticate(VerifiedAdminToken token);
}
```

`VerifiedAdminToken` 由后台验签组件创建。`AuthenticatedAdmin` 持有已经验证的 `adminUserId`、Token 标识和数据库读出的权限集合。两个对象只用于服务内部，均不接受请求 JSON 反序列化。

### 3.3 登出、撤权与并发

- 建议用 `foodmap_admin_db` 的会话登记表实现即时撤销。每次登录生成新 `jti`，请求必须命中有效登记记录。这样登出和审计可在同一个本地事务提交，重启或缓存丢失后也不会恢复已撤销的 Token。代价是每次后台请求都需要查询后台库，本方案将该检查与实时授权合并。
- 登出按 `jti`、`adminUserId` 和「未撤销」条件更新。只有首次成功撤销写入审计。并发登出若都已通过入口验证，后提交的一次可返回成功。撤销提交后再次使用该 Token，请求在认证阶段返回 `401`。
- 角色关联、角色状态或权限变更与受影响管理员的 `permission_version` 递增在同一事务完成。变更提交后发起的请求，旧 Token 返回 `401`。管理员重新登录后，仍没有目标权限则返回 `403`。批量变更以固定 ID 顺序锁定受影响账号，避免竞争下丢失版本更新。
- 登录、登出、授权变更记录业务主键、动作、结果、`requestId`、`traceId` 和脱敏摘要。身份未知的登录失败通过 `SafeLog` 输出审计事件，无需构造虚假的管理员 ID。生产路由开放前配置 HTTPS、登录限流和后台入口访问限制，登录限流使用可信代理解析出的来源，客户端头不能直接指定限流身份。

## 4. Flyway 与 MyBatis

建议使用 `V2__create_admin_rbac_tables.sql` 建立 RBAC 表，后续新增迁移建立会话与审计表。迁移先后应用于空库和已有 V1 账号的数据库。所有业务表遵循 `id / created_time / updated_time / is_delete` 固定字段、`bigint` 业务主键、Flyway sequence 和中文 `comment on table` / `comment on column` 约定。

| 表 | 关键字段与索引 |
| --- | --- |
| `admin_roles` | `role_id`、`role_code`、`role_name`、`role_description`、`role_status`。有效角色编码部分唯一 |
| `admin_permissions` | `permission_id`、`permission_code`、`permission_name`、`permission_module`、`permission_description`、`permission_status`。有效权限编码部分唯一 |
| `admin_user_roles` | `relation_id`、`admin_user_id`、`role_id`、`granted_by`、`granted_time`。有效 `(admin_user_id, role_id)` 部分唯一。补按 `role_id` 反查管理员的索引 |
| `admin_role_permissions` | `relation_id`、`role_id`、`permission_id`、`granted_by`、`granted_time`。有效 `(role_id, permission_id)` 部分唯一。补按 `permission_id` 反查角色的索引 |
| `admin_access_sessions`（拟新增） | `session_id bigint`（会话业务主键）、`token_id uuid not null`（JWT `jti`，全局唯一）、`admin_user_id bigint not null`（关联管理员业务主键）、`expires_time timestamptz not null`（过期时间）、`revoked_time timestamptz`（撤销时间）。按 `expires_time` 建索引，分批清理过期登记 |
| `admin_operation_logs` | 按架构文档已有字段落地。保存管理员业务 ID、动作、结果和关联流水号，审计历史独立于会话清理 |

迁移只预置 `OPS_VIEWER` 角色、`LOG_ACCESS_READ` 权限及两者关系。首批人员由受控初始化入口调用现有 `AdminUserService` 创建账号并显式分配角色。首次初始化可用一次性本地运维命令，密码从交互输入或密钥注入取得。测试账号与角色授权只在隔离测试库生成。涉及账号与角色关系的写入放在同一本地事务。

每张新表配套 Entity、标准 Mapper/XML。复杂 SQL 使用 `AdminUserRoleDefineMapper` 查询有效角色，以及 `AdminAccessSessionDefineMapper` 读取认证和授权快照。快照查询从 `jti` 对应的会话行开始，校验 `adminUserId`、未撤销、未过期和管理员 `ACTIVE` 状态，并匹配当前权限版本。权限侧只保留有效且未逻辑删除的角色、权限及两类关联。保留「会话有效、权限为空」的结果，用它区分 `401` 和 `403`。Repository 将 SQL 结果转换为应用层投影，实体字段 Javadoc 与 SQL 中文注释逐项一致。

来源：[角色和权限表](../../../CODEX-after.md#629-管理后台服务表结构) · [V1 管理员迁移](../../../after/foodmap-admin-service/src/main/resources/db/migration/V1__create_admin_user_tables.sql)

## 5. 文件清单与实施顺序

| 顺序 | 计划改动 | 评审检查 |
| --- | --- | --- |
| 1 | 同步 `CODEX-after.md`、T-033 任务卡及加固计划，补正式后台 API 契约 | 替换 B1.5 权限头描述，补会话字段与中文注释、验签责任、登出语义和文件边界 |
| 2 | `foodmap-admin-service` 的 `resources/db/migration`、`infrastructure/persistence/{entity,mapper,mybatis}`、`resources/mapper/admin`、`application/port` | 迁移、查询投影、会话撤销、受控初始化。Repository 和 Service 间通过端口交互 |
| 3 | `foodmap-admin-service` 的 `controller`、`dto`、`service`、`service/impl`、`security`、`config`、profile 配置及 `pom.xml` | 新增 `AdminAuthController`、登录 DTO、后台 Token 组件与认证拦截器，改造 `AdminPermissionGuard` 和 `AdminLogController` |
| 4 | `foodmap-gateway-service` 的 `GatewayAuthFilter`、后台验签配置类、路由/profile 配置及 `pom.xml` | 剥头、精确豁免登录、后台独立验签、普通 App Token 回归 |
| 5 | 两模块测试、`harness/scenarios/t033-admin-rbac.yml`、T-032 现有场景、依赖预检配置和验收台账 | 通过真实网关和后台服务验证，用可观测结果关联 F-020 |

**任务范围需先同步。** 当前任务卡仅允许网关修改剥头逻辑，也未包含后台 DTO、Service、密钥配置及依赖声明。上表是供评审的扩展边界。方案复用现有 `foodmap-common` 接口与响应结构，通过两端一致的 JWT 契约和测试覆盖分离的验签配置。

发布顺序是先完成迁移并启动新后台服务，再开放网关显式路由。回退时关闭后台路由并保留迁移和审计数据，避免旧权限头路径重新暴露。

## 6. 验收用例

| 场景 | 可观测断言 |
| --- | --- |
| 授权管理员登录并经网关查询 | 登录 `200/OK`。查询 `200/OK`，命中预置的一条脱敏日志摘要，可用 `requestId` 关联日志服务 |
| 已认证但无权限的管理员 | 查询 `403/FORBIDDEN`。即使自带 `X-Admin-Permissions: LOG_ACCESS_READ`，也不会调用日志服务 |
| 未登录、伪造头、普通 App Token | 网关查询 `401/UNAUTHORIZED`。直连后台伪造头、携带普通 App Token 同样 `401`。后台 Token 调普通保护 API 也被拒绝 |
| 网关请求头清理 | 后台登录和受保护请求均剥除大小写变体、多值及任意 `X-Admin-*`，保留合法 Bearer 凭证和链路头 |
| JWT 无效 | 签名、算法、受众、签发方、类型或必填声明不合规，以及已过期的 Token 均返回 `401` |
| 登出、账号停用或授权撤销 | 登出后原 Token `401`，另一个有效会话仍可查。账号停用或权限版本变化后，受影响的旧 Token `401`。重新登录且无权限时 `403` |
| 数据源故障 | 依赖不可用返回 `503`。登出未完成撤销时不报告成功，查询失败时没有日志服务调用 |
| Flyway 兼容与审计 | 空库迁移和 V1 升级均成功，表字段有中文注释、V1 账号可登录。登录、登出与授权操作可通过审计记录追溯 |

验收至少准备两个独立管理员和一个普通 App 用户。测试凭据在运行时注入，清理只使用本轮生成的业务主键。模块测试通过后，从仓库根目录执行场景和全量门禁。

现有 T-032 场景的后台日志正向用例使用旧权限头。实现时将该正向请求改为先登录取得后台 Token，继续断言 admin 到 log 的真实内部签名调用成功，并保留原有无签名、重放、越权和篡改断言。T-033 场景单独验证旧头无法授权。[现有 T-032 探针](../../../harness/scenarios/internal-service-auth-check.py)

```sh
(cd after && mvn -q -B -pl foodmap-admin-service,foodmap-gateway-service -am test)
node tools/verify/cli.mjs run harness/scenarios/t033-admin-rbac.yml
node tools/verify/cli.mjs run harness/scenarios/
VERIFY_REQUIRE_FULL=1 ./harness/scripts/verify.sh
./harness/scripts/verify-record.sh harness/scenarios/t033-admin-rbac.yml \
  --scope=t033-admin-rbac --kind=e2e --covers=F-020
./status/status check
./status/status build
```

上线切片以实际网关链路和日志服务返回为准。报告分别列出通过、失败和跳过项。业务验收要求全量无跳过，台账记录测试证据并重新生成看板。以后接入后台前端时，另按仓库约定准备前后端联调目录和验收计划。

来源：[T-033 验收标准](../agent-tasks/T-033-admin-rbac-server-side.md#6-验收标准) · [验证纪律](../../../AGENTS.md#631-验证纪律)

## 7. 评审决策

1. 确认 JWT 使用独立 RSA 密钥、建议 15 分钟有效期，以及密钥注入和轮换责任。
2. 确认使用后台 PostgreSQL 作为会话和审计的事实源，接受每次请求的会话查询，定稿过期登记的清理策略。
3. 确认首批账号的受控初始化入口、生产后台登录准入条件，以及 T-033 的文件范围扩展。
