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
- 写接口必须从 Token 获取当前用户身份。
- Controller 使用 DTO，不直接暴露实体。
- 推荐服务负责可见范围事实判断。
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

