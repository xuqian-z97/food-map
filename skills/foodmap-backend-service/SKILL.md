---
name: foodmap-backend-service
description: Use when creating, updating, testing, or reviewing FoodMap Java Spring Boot microservices, service modules, backend APIs, database schemas, visibility rules, events, Docker Compose, or backend acceptance criteria.
---

# FoodMap 后端服务 Skill

## 使用时机

当任务涉及 FoodMap 后端时使用本 skill：

- 创建 Java 微服务骨架。
- 实现认证、用户、关系、门店、推荐、社区、媒体服务。
- 设计 API、DTO、数据库表、事件。
- 实现权限、可见范围、社区统计。
- 配置 Docker Compose、Nacos、Redis、消息队列、PostgreSQL/PostGIS。
- 生成或调整 ECS2 Docker Compose 部署模板。

## 必读文档

- `CODEX-after.md`
- `AGENTS.md`
- `.agents/backend-agent.md`

如涉及产品行为，同时读取 `CODEX-product.md`。
如涉及 API 契约，同时读取 `skills/foodmap-api-contract/SKILL.md`。

## 技术栈

- Java 21
- Spring Boot
- Spring Cloud / Spring Cloud Alibaba
- Spring Cloud Gateway
- Nacos
- Spring Security + JWT
- MyBatis + Mapper.xml
- PostgreSQL / PostGIS
- Redis
- RocketMQ 或 RabbitMQ
- MinIO 或阿里云 OSS
- Maven
- Docker Compose

## 部署约束

- MVP 使用 Docker Compose 部署。
- ECS2 是主应用服务器。
- ECS1 是辅助服务器。
- 两台服务器不在同一云厂商/VPC 时，不通过裸公网访问 Redis、PostgreSQL、Nacos、RabbitMQ。
- 如需跨服务器访问内部组件，必须先建立 WireGuard/VPN。
- OSS 用于用户头像、推荐菜单图片、推荐评论图片和门店图片。

## 服务边界

MVP 服务：

- `foodmap-gateway-service`
- `foodmap-auth-service`
- `foodmap-user-service`
- `foodmap-relation-service`
- `foodmap-store-service`
- `foodmap-recommendation-service`
- `foodmap-community-service`
- `foodmap-media-service`

后续服务：

- `foodmap-notification-service`
- `foodmap-admin-service`

## 强制规则

- 每个服务拥有独立数据库。
- 服务之间不能共享数据表。
- 跨服务数据通过 API 或事件获取。
- 后端代码必须遵守 `CODEX-after.md` 中的“后端微服务企业级开发基线”。
- 写接口必须从 Token 获取当前用户身份。
- Controller 使用 DTO，不直接暴露实体。
- Controller 不能直接调用 Mapper，事务边界放在 ServiceImpl 层。
- 单服务内出现多张表新增、编辑、删除时，必须在 ServiceImpl 用例方法上使用本地事务，优先 `@Transactional(rollbackFor = Exception.class)`。
- 跨服务写流程不默认使用强分布式事务，必须采用 Saga/补偿事务 + Outbox + 幂等消费实现最终一致性。
- 跨服务数据交互失败时，必须有失败状态、补偿任务、重试或人工处理入口，不能静默失败。
- 可能并发冲突的业务主键或唯一性数据操作，必须优先判断数据库唯一约束、乐观锁、悲观锁或 Redis 分布式锁是否需要引入。
- Redis 分布式锁只能通过统一封装访问，锁必须有 owner token、lease time，并通过原子脚本释放。
- Redis 锁看门狗只用于耗时不稳定但必须串行的临界区，必须设置续期间隔、续期租约和最大续期次数，禁止无限续期。
- 业务代码使用分布式锁时优先使用 `DistributedLockClient` 的 `executeWithLock`、`tryExecuteWithLock` 或 `executeWithWatchdog` 公共方法。
- API 正常和异常响应使用统一结构：`success`、`status`、`code`、`message`、`data`。
- `status` 必须使用 HTTP 数字状态码语义，`code` 必须使用稳定可枚举业务码。
- 后端必须通过统一异常拦截机制处理业务异常、参数校验异常、JSON 解析异常、请求方法错误和未预期异常。
- 异常响应不能暴露异常类名、堆栈、SQL、Token、密码或内部依赖地址。
- `401` 用于未认证或登录状态失效，`403` 用于已认证但权限不足，不能混用。
- 所有入参必须使用 Bean Validation 或等价方式校验。
- 服务间同步调用必须设置超时，并有明确失败处理。
- 所有数据库结构变更必须通过 Flyway 脚本管理。
- 关键事件消费者必须支持幂等。
- 重复出现 2 次以上的基础校验、值判断、脱敏、时间处理逻辑，应优先沉淀到 `foodmap-common` 中有明确边界的项目级工具类。
- 禁止创建无明确边界的万能工具类，如 `CommonUtils`、`StringUtils`、`DateUtils`。
- record、Command、事件信封和中间件命令中的基础参数校验应优先复用 `common.validation.Check`。
- Spring Bean 依赖注入默认允许使用 `@Autowired` 字段注入，便于用户接手和快速开发；字段必须保持 `private`。
- 强必需依赖、需要 `final` 不可变、需要脱离 Spring 容器测试、或需要尽早暴露循环依赖的类，优先使用构造器注入。
- 多个同类型 Bean 或需要按名称选择实现时，可以使用 `@Resource`、`@Qualifier` 或等价方式明确注入目标。
- 同一个类中不要混用多种注入方式，确有框架原因时必须补充说明。
- 数据库结构对应 Java 类必须放在服务内 `infrastructure.persistence.entity` 包中，并与 DTO、VO 明确区分。
- `foodmap-common` 的 `BaseEntity` 只承载 `id / created_time / updated_time / is_delete` 固定字段，不承载业务主键。
- 持久化实体字段必须提供字段级 Javadoc；如果字段对应数据库字段，注释必须与表字段中文注释一一对应。
- Controller 只能使用 DTO 作为请求和响应契约，不能直接暴露数据库持久化实体。
- 数据库访问统一使用 MyBatis Mapper + Mapper.xml。
- 每张业务表必须生成 `{EntityName}Mapper.java` / `{EntityName}Mapper.xml` 标准单表 SQL。
- 复杂业务 SQL 必须放入 `{EntityName}DefineMapper.java` / `{EntityName}DefineMapper.xml`。
- Repository 实现类名不使用 `MyBatis`、`Jdbc`、`Redis` 等技术前缀，统一采用 `{EntityName}RepositoryImpl` 或业务聚合语义命名；技术实现差异通过包名和注释表达。
- service 层必须使用 `XxxService` 接口 + `XxxServiceImpl` 实现类，Controller 只能依赖 `XxxService` 接口。
- ServiceImpl 只能依赖仓储端口接口，不能直接依赖内存仓储、MyBatis Mapper 等基础设施实现。
- 内存仓储只允许作为单元测试或本地替身，不作为生产 profile 默认持久化实现。
- 公共类、跨模块复用类、接口、枚举、异常、事件、配置类和中间件封装类必须提供类级 Javadoc 注释。
- 常量类和枚举类中的每一个常量、每一个枚举项都必须提供 Javadoc 注释，说明业务含义、使用场景和排查价值。
- 业务方法必须提供方法级 Javadoc 注释，注释必须说明方法业务作用，并为每个入参提供 `@param`；非 `void` 方法必须为返回结果提供 `@return`，`void` 方法不需要增加 `@return`。
- Controller 层接口方法必须使用接口注释，说明接口用途、调用方关注的入参、返回结果、权限或登录态来源；如方法有入参和返回值，必须同步补齐 `@param` / `@return`。
- Service、ServiceImpl、domain、repository、mapper、通用工具和中间件适配器中的业务方法必须按业务方法注释规则执行。
- JavaBean getter / setter、Spring Boot 启动类 `main` 方法不强制提供方法级 Javadoc；实体字段说明以字段级 Javadoc 为准。
- 构造方法不是普通业务方法，不要求 `@return`，但有入参且承担依赖边界说明时，应在构造方法 Javadoc 中提供对应 `@param`。
- `private` 方法如果承载复杂业务判断、脱敏规则、幂等规则、权限规则、状态流转或非显而易见的转换逻辑，也必须提供方法级 Javadoc，并按参数和返回值补齐 `@param` / `@return`。
- 注释必须说明职责、边界和排查关注点，不能只重复代码字面含义。
- Redis、MQ、对象存储、内部服务调用必须通过项目统一封装或服务内基础设施适配器访问，业务代码不能散落直接调用中间件 SDK。
- Redis Key 必须遵守 `foodmap:{service}:{biz}:{version}:{key}` 格式，业务缓存必须设置 TTL。
- 业务事件发布必须通过统一事件发布接口，事件消费者必须具备幂等处理。
- 对象存储访问必须通过统一对象存储接口，本地优先 MinIO，生产适配阿里云 OSS。
- 业务代码不得随意字符串拼接打印关键业务日志。
- 涉及用户、认证、推荐、评论、文件和中间件调用的日志必须使用通用日志方法或统一日志封装。
- 日志不能输出 Token、密码、密钥、完整手机号、完整邮箱或私密推荐内容。
- 推荐服务负责可见范围事实判断。
- 推荐评论和评论图片继承所属推荐菜单的可见范围。
- 单条推荐评论最多支持 3 张图片。
- 社区服务只能统计 `PUBLIC` 且 `NORMAL` 的推荐。
- 推荐服务不能直接访问关系服务数据库。

## 单服务目录

```text
service-name
├── src/main/java
│   └── com/foodmap/{service}
│       ├── controller
│       ├── application
│       ├── domain
│       ├── infrastructure
│       ├── mapper
│       ├── dto
│       ├── config
│       └── ServiceApplication.java
├── src/main/resources
│   ├── application.yml
│   └── mapper
└── pom.xml
```

## 工作流程

1. 先确认任务对应服务和写入范围。
2. 如服务/API/数据库变化，先更新 `CODEX-after.md` 和必要的 API 文档。
3. 实现服务代码。
4. 补充关键测试。
5. 执行 Maven 构建或服务测试。
6. 汇报修改文件、验证结果、剩余风险。

## 验收标准

- 服务边界和数据库归属符合 `CODEX-after.md`。
- 不存在跨服务直接查表。
- API 使用 DTO。
- 数据库持久化实体、DTO、VO 已分层存放并显式转换。
- 标准 Mapper/XML 和 DefineMapper/XML 分工符合 `CODEX-after.md`。
- ServiceImpl 层依赖仓储端口接口，基础设施实现没有向上穿透。
- 权限和可见范围由后端校验。
- 关键业务规则有测试或明确说明暂未测试原因。
