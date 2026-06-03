# FoodMap 部署架构

## 1. MVP 架构图

```text
iOS App
  |
  | HTTPS
  v
ECS2: Nginx :80/:443
  |
  v
ECS2: foodmap-gateway-service
  |
  +--> auth-service
  +--> user-service
  +--> relation-service
  +--> store-service
  +--> recommendation-service
  +--> community-service
  +--> media-service
              |
              v
         阿里云 OSS
```

ECS2 内部组件：

```text
Nacos standalone
PostgreSQL + PostGIS
RabbitMQ
Redis
```

ECS1 辅助：

```text
备份
监控，后续
跳板，后续
```

## 2. 网络边界

公网只暴露：

```text
80/tcp
443/tcp
22/tcp，建议限制来源 IP
```

不直接公网暴露：

```text
5432 PostgreSQL
6379 Redis
8848 Nacos
5672 RabbitMQ
15672 RabbitMQ Management
所有微服务端口
```

## 3. 服务发现

MVP 使用 ECS2 本机 Docker 网络中的 Nacos standalone。

微服务通过 Nacos 注册，Gateway 通过服务名路由。

## 4. 数据库策略

虽然架构上要求每个微服务独立数据库，但 MVP 可以在同一个 PostgreSQL/PostGIS 容器中创建多个逻辑数据库：

```text
foodmap_auth_db
foodmap_user_db
foodmap_relation_db
foodmap_store_db
foodmap_recommendation_db
foodmap_community_db
foodmap_media_db
```

规则：

- 每个服务只连接自己的数据库。
- 不允许跨库直接查询。
- 跨服务数据通过 API 或事件同步。

## 5. 消息队列策略

MVP 推荐 RabbitMQ。

原因：

- 对 4G 内存服务器更友好。
- Docker Compose 部署简单。
- 足够支撑推荐事件、统计事件、通知事件的初始需求。

RocketMQ 暂作为后续扩展选项。

## 6. OSS 策略

media-service 负责：

- 生成上传凭证。
- 校验图片类型和大小。
- 保存 objectKey、bucket、url、ownerId、status。
- 绑定推荐菜单图片引用。

图片不进入数据库二进制字段。

