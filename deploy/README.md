# FoodMap 部署说明

## 1. 部署目标

本文档定义 FoodMap MVP 阶段在两台不同云厂商服务器和阿里云 OSS 上的部署方案。

当前服务器：

```text
ECS1
- 公网 IP：112.124.13.171
- 配置：2C2G
- 系统：CentOS 7.4 64
- 已安装：Docker，含 Redis、MySQL 容器

ECS2
- 公网 IP：115.190.223.31
- 配置：4C4G
- 系统：Ubuntu 22.04 64
- 已安装：无

对象存储
- 阿里云 OSS
```

## 2. 关键约束

两台服务器不在同一个云厂商或同一个 VPC，因此不能默认使用内网通信。

强制原则：

- 不把 Redis、PostgreSQL、Nacos、RabbitMQ、微服务内部端口直接暴露公网。
- 不通过裸公网连接 Redis 或数据库。
- 如果必须跨服务器访问内部组件，必须先建立 WireGuard/VPN 隧道。
- MVP 阶段优先把核心运行时放在 ECS2 单机，降低跨公网风险。

## 3. 推荐 MVP 部署

ECS2 作为主应用服务器：

```text
ECS2 / 115.190.223.31
├── Nginx
├── Spring Cloud Gateway
├── Nacos standalone
├── PostgreSQL + PostGIS
├── RabbitMQ
├── Redis，MVP 推荐也放 ECS2
├── foodmap-auth-service
├── foodmap-user-service
├── foodmap-relation-service
├── foodmap-store-service
├── foodmap-recommendation-service
├── foodmap-community-service
└── foodmap-media-service
```

ECS1 作为辅助服务器：

```text
ECS1 / 112.124.13.171
├── 备份落地目录
├── 轻量监控，后续
├── 运维跳板，后续可选
└── Redis 备用，不建议 MVP 跨公网使用
```

阿里云 OSS：

```text
OSS
├── 用户头像
├── 推荐菜单图片
└── 门店图片
```

## 4. 为什么不把 Redis 放 ECS1

ECS1 和 ECS2 不在同一个私有网络时，ECS2 访问 ECS1 Redis 只能走公网，除非额外建立 VPN。

公网 Redis 的问题：

- 安全风险高。
- 网络延迟和抖动更大。
- 需要额外 TLS/VPN/访问控制。
- Redis 是高频依赖，跨公网不适合作为 MVP 默认方案。

因此 MVP 推荐：

```text
Redis 和后端服务同放 ECS2
```

ECS1 先作为备份和辅助节点。

## 5. 后续扩展方案

### 5.0 Nacos 配置中心约定

B1.5-b 起后端服务接入 Nacos Config 可选导入，默认关闭，避免本地开发时依赖配置中心。日志链路在本地通过 Docker Compose `logging` profile 启动 Kafka、`kafka-init`、Fluent Bit、Elasticsearch、`elasticsearch-init`、Logstash 和 `log-postgres-migrate`；业务服务不直接依赖 Kafka SDK，后续容器化服务需要把应用日志写入共享日志卷后由 Fluent Bit 统一转发，并由 Logstash 消费 Kafka 写入 Elasticsearch 热查询索引。接口访问摘要表位于独立日志库 `foodmap_log_db.api_access_log`，由 `foodmap-log-service` 在显式开启 `LOG_SERVICE_API_ACCESS_CONSUMER_ENABLED=true` 后消费 `foodmap.logs.api-access` 并幂等写入；摘要清理任务由 `LOG_SERVICE_API_ACCESS_RETENTION_CLEANUP_ENABLED=true` 显式开启，默认保留 15 天。全量日志 OSS 归档当前已具备 `log_archive_records` 计划记录、执行状态机、`foodmap-common` 统一 Elasticsearch SearchClient、真实日志归档导出适配器、common ObjectStorageClient 上传桥接和 MinIO/S3 兼容对象存储实现，`LOG_ARCHIVE_PLANNING_ENABLED=true` 后可生成 PENDING 归档窗口；`LOG_ARCHIVE_EXECUTION_ENABLED=true` 后可驱动状态流转；`LOG_ARCHIVE_ELASTICSEARCH_ENABLED=true` 后会按窗口从 Elasticsearch 分页导出 gzip JSON Lines；`LOG_ARCHIVE_STORAGE_UPLOAD_ENABLED=true` 且 `FOODMAP_STORAGE_MINIO_ENABLED=true` 后会把归档载荷交给 common MinIO 适配器，生产 OSS 后续通过同一个 ObjectStorageClient 端口接入。

启用方式：

```text
NACOS_CONFIG_ENABLED=true
NACOS_CONFIG_GROUP=FOODMAP
NACOS_SERVER_ADDR=nacos:8848
```

推荐 Data ID：

```text
foodmap-logging-{profile}.yml
{spring.application.name}-{profile}.yml
```

`foodmap-logging-{profile}.yml` 用于共享日志配置，模板位于：

```text
deploy/nacos/foodmap-logging-template.yml
```

生产环境临时打开 SQL DEBUG 时，应优先按 `request-ids`、`trace-ids` 或 `mapper-includes` 定向开启，排查结束后及时清空。

### 5.1 有 VPN/WireGuard 后

如果 ECS1 和 ECS2 之间建立 WireGuard 隧道，可以调整为：

```text
ECS1
├── Redis
├── 备份任务
└── 监控

ECS2
├── Nginx
├── Gateway
├── 微服务
├── PostgreSQL/PostGIS
├── Nacos
└── RabbitMQ
```

所有内部依赖使用 WireGuard 私有地址通信。

### 5.2 生产增强后

当用户量上升后，建议迁移：

- PostgreSQL/PostGIS 到云数据库或独立数据库服务器。
- Redis 到托管 Redis。
- Nacos/RabbitMQ 到独立节点。
- 微服务到 ACK/Kubernetes 或更多 ECS。
- 日志到 SLS 或 ELK。

## 6. 当前 deploy 文件

```text
deploy
├── README.md
├── architecture.md
├── env.example
├── docker-compose.ecs2.yml
├── nginx
│   └── foodmap.conf
└── checklists
    ├── deploy-checklist.md
    └── security-group.md
```

当前配置是模板，用于指导部署。后端微服务代码生成后，需要补充镜像名、端口、健康检查和环境变量。

## 7. 本地隔离开发环境

本地开发使用：

```text
deploy/docker-compose.dev.yml
.env.dev.example
deploy/dev-env/README.md
```

当前本地 Compose 主要启动 PostgreSQL/PostGIS、Redis、Nacos、RabbitMQ 和 MinIO。

推荐方式：

1. 依赖运行在 OrbStack 或 Docker Desktop。
2. Java 微服务先通过 IDEA/Maven 在 Mac 本机启动，默认使用 `local` profile。
3. 后续微服务容器化后，在 Compose 网络中使用 `orbstack` profile。

启动本地依赖：

```sh
cp .env.dev.example .env.dev
docker compose --env-file .env.dev -f deploy/docker-compose.dev.yml up -d
```
