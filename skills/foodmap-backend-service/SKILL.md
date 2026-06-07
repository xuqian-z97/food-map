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
- MyBatis 或 MyBatis-Plus
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
- 权限和可见范围由后端校验。
- 关键业务规则有测试或明确说明暂未测试原因。
