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
- Controller 不能直接调用 Mapper，事务边界放在 application 层。
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
- Controller 只能使用 DTO 作为请求和响应契约，不能直接暴露数据库持久化实体。
- 数据库访问统一使用 MyBatis Mapper + Mapper.xml。
- 每张业务表必须生成 `{EntityName}Mapper.java` / `{EntityName}Mapper.xml` 标准单表 SQL。
- 复杂业务 SQL 必须放入 `{EntityName}DefineMapper.java` / `{EntityName}DefineMapper.xml`。
- application 层只能依赖仓储端口接口，不能直接依赖内存仓储、MyBatis Mapper 等基础设施实现。
- 内存仓储只允许作为单元测试或本地替身，不作为生产 profile 默认持久化实现。
- 公共类、跨模块复用类、接口、枚举、异常、事件、配置类和中间件封装类必须提供类级 Javadoc 注释。
- 常量类和枚举类中的每一个常量、每一个枚举项都必须提供 Javadoc 注释，说明业务含义、使用场景和排查价值。
- 对外暴露的公共方法、静态工厂方法、复杂业务判断、脱敏规则、幂等规则、权限规则和状态流转必须提供方法级注释。
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
- application 层依赖仓储端口接口，基础设施实现没有向上穿透。
- 权限和可见范围由后端校验。
- 关键业务规则有测试或明确说明暂未测试原因。
