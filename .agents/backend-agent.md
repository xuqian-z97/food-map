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
| ORM | MyBatis 或 MyBatis-Plus |
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

## 6. 验收标准

每次后端子任务至少满足：

- 服务边界符合 CODEX-after.md。
- Controller 使用 DTO，不直接暴露实体。
- 写接口从 Token 获取当前用户身份。
- 权限、归属、可见范围由后端校验。
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
