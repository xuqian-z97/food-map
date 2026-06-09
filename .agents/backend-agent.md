# 后端子代理规范

## 1. 职责

后端子代理负责 FoodMap Java 微服务、API、数据库、权限、事件、缓存和基础设施实现。

必须遵守：

- CODEX-product.md
- CODEX-after.md
- AGENTS.md

## 2. 技术栈

| 类别 | 技术 |
| --- | --- |
| 语言 | Java 21 |
| 框架 | Spring Boot |
| 微服务 | Spring Cloud / Spring Cloud Alibaba |
| 网关 | Spring Cloud Gateway |
| 注册/配置 | Nacos |
| 鉴权 | Spring Security + JWT |
| ORM | MyBatis + Mapper.xml |
| 数据库 | PostgreSQL |
| 地理能力 | PostgreSQL + PostGIS |
| 缓存 | Redis |
| 消息队列 | RocketMQ 或 RabbitMQ |
| 对象存储 | MinIO 或阿里云 OSS |
| 构建 | Maven |

## 3. 负责服务

- foodmap-gateway-service
- foodmap-auth-service
- foodmap-user-service
- foodmap-relation-service
- foodmap-store-service
- foodmap-recommendation-service
- foodmap-community-service
- foodmap-media-service
- foodmap-notification-service，后续
- foodmap-admin-service，后续

## 4. 默认写入范围

默认只允许修改主代理指定的单个后端服务目录，例如：

```text
after/foodmap-auth-service
after/foodmap-common
```

不得跨服务修改，除非主代理明确授权。

## 5. 数据库规则

- 每个服务拥有独立数据库。
- 服务不能直接访问其他服务的数据表。
- 跨服务数据通过 API 或事件获取。
- 推荐服务不能直接查询关系服务数据库。
- 社区服务只能统计 PUBLIC 推荐。
- 重复出现 2 次以上的基础校验、值判断、脱敏、时间处理逻辑，应优先沉淀到 `foodmap-common` 中有明确边界的项目级工具类。
- 禁止创建无明确边界的万能工具类，如 `CommonUtils`、`StringUtils`、`DateUtils`。
- record、Command、事件信封和中间件命令中的基础参数校验应优先复用 `common.validation.Check`。
- Spring Bean 依赖注入默认允许使用 `@Autowired` 字段注入，便于用户接手和快速开发；字段必须保持 `private`。
- 强必需依赖、需要 `final` 不可变、需要脱离 Spring 容器测试、或需要尽早暴露循环依赖的类，优先使用构造器注入。
- 多个同类型 Bean 或需要按名称选择实现时，可以使用 `@Resource`、`@Qualifier` 或等价方式明确注入目标。
- 同一个类中不要混用多种注入方式，确有框架原因时必须补充说明。
- 数据库结构对应 Java 类必须放在服务内 `infrastructure.persistence.entity` 包中，并与 DTO、VO 明确区分。
- `foodmap-common` 的 `BaseEntity` 只承载 `id / created_time / updated_time / is_delete` 固定字段，不承载业务主键。
- Controller 只能使用 DTO 作为请求和响应契约，不能直接暴露数据库持久化实体。
- 数据库访问统一使用 MyBatis Mapper + Mapper.xml。
- 每张业务表必须生成 `{EntityName}Mapper.java` / `{EntityName}Mapper.xml` 标准单表 SQL。
- 复杂业务 SQL 必须放入 `{EntityName}DefineMapper.java` / `{EntityName}DefineMapper.xml`。
- Repository 实现类名不使用 `MyBatis`、`Jdbc`、`Redis` 等技术前缀，统一采用 `{EntityName}RepositoryImpl` 或业务聚合语义命名；技术实现差异通过包名和注释表达。
- application 层只能依赖仓储端口接口，不能直接依赖内存仓储、MyBatis Mapper 等基础设施实现。
- 内存仓储只允许作为单元测试或本地替身，不作为生产 profile 默认持久化实现。
- 公共类、跨模块复用类、接口、枚举、异常、事件、配置类和中间件封装类必须提供类级 Javadoc 注释。
- 常量类和枚举类中的每一个常量、每一个枚举项都必须提供 Javadoc 注释，说明业务含义、使用场景和排查价值。
- 对外暴露的公共方法、静态工厂方法、复杂业务判断、脱敏规则、幂等规则、权限规则和状态流转必须提供方法级注释。
- 注释必须说明职责、边界和排查关注点，不能只重复代码字面含义。
- Redis、MQ、对象存储、内部服务调用必须通过项目统一封装或服务内基础设施适配器访问。
- 业务代码不得散落直接调用中间件 SDK。
- 业务代码不得随意字符串拼接打印关键业务日志。
- 涉及用户、认证、推荐、评论、文件和中间件调用的日志必须使用通用日志方法或统一日志封装。
- 日志不能输出 Token、密码、密钥、完整手机号、完整邮箱或私密推荐内容。

## 6. 验收标准

每次后端子任务至少满足：

- 服务边界符合 CODEX-after.md。
- Controller 使用 DTO，不直接暴露实体。
- 数据库持久化实体、DTO、VO 已分层存放并显式转换。
- 标准 Mapper/XML 和 DefineMapper/XML 分工符合 CODEX-after.md。
- application 层依赖仓储端口接口，MyBatis/内存仓储等实现没有向上穿透。
- 写接口从 Token 获取当前用户身份。
- 权限、归属、可见范围由后端校验。
- 重复基础校验和值判断已沉淀到明确边界的 common 工具类，未新增万能 Utils。
- 公共 API、复杂业务逻辑、中间件封装、常量项和枚举项已补充必要注释。
- 中间件访问和日志打印符合 `CODEX-after.md` 的阶段一封装规则。
- 列表接口支持分页。
- 地图接口支持边界框查询。
- 关键业务规则有测试。
- 条件允许时执行 Maven 构建或服务测试。

## 7. 禁止事项

- 不得把多个服务合并成单体。
- 不得跨服务直接访问数据库表。
- 不得让前端承担核心权限判断。
- 不得把非 PUBLIC 推荐写入全站社区统计。
- 不得擅自改变消息队列、数据库或注册中心选型。
