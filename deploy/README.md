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

