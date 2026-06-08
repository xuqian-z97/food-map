# FoodMap 本地隔离开发环境

## 1. 目标

本目录说明 FoodMap 在本机隔离环境中的启动方式。

当前约定：

- OrbStack 或 Docker Desktop 负责运行容器。
- `deploy/docker-compose.dev.yml` 负责启动本地依赖。
- IDEA / Maven 默认用 `local` profile 启动 Java 服务。
- 后续 Java 服务容器化后，Compose 内部使用 `orbstack` profile。

## 2. 环境类型

| Profile | 启动位置 | 依赖访问方式 | 使用场景 |
| --- | --- | --- | --- |
| local | Mac 本机 / IDEA | `127.0.0.1` | 当前默认开发方式 |
| orbstack | Docker / OrbStack 容器网络 | Compose 服务名，如 `nacos:8848` | 后续微服务容器化 |
| prod | ECS / 生产环境 | 显式环境变量 | 线上部署 |

为什么这样区分：

- Mac 本机访问容器端口时使用 `127.0.0.1`。
- 容器之间访问不能使用 `127.0.0.1`，必须使用 Compose 服务名。
- 生产环境不能依赖本地默认值，必须显式注入环境变量。

## 3. 启动本地依赖

首次使用：

```sh
cp .env.dev.example .env.dev
docker compose --env-file .env.dev -f deploy/docker-compose.dev.yml up -d
```

查看状态：

```sh
docker compose --env-file .env.dev -f deploy/docker-compose.dev.yml ps
```

停止：

```sh
docker compose --env-file .env.dev -f deploy/docker-compose.dev.yml down
```

清理数据卷：

```sh
docker compose --env-file .env.dev -f deploy/docker-compose.dev.yml down -v
```

## 4. 默认端口

| 组件 | 地址 |
| --- | --- |
| PostgreSQL/PostGIS | `localhost:5432` |
| Redis | `localhost:6379` |
| Nacos | `http://localhost:8848/nacos` |
| RabbitMQ Management | `http://localhost:15672` |
| MinIO Console | `http://localhost:9001` |

开发账号密码见 `.env.dev.example`，仅允许用于本地开发。

## 5. 本机启动 Java 服务

当前阶段推荐先用 IDEA 或 Maven 在 Mac 本机启动服务，不急着容器化 Java 服务。

默认情况下，不设置任何变量就是 `local`：

```sh
cd after
./scripts/run-service.sh foodmap-auth-service
```

显式指定：

```sh
./scripts/run-service.sh foodmap-auth-service local
```

此时服务会连接：

```text
NACOS_SERVER_ADDR=127.0.0.1:8848
```

认证服务和用户服务已经接入 PostgreSQL/Flyway，本机启动时还会连接：

```text
AUTH_DB_URL=jdbc:postgresql://127.0.0.1:5432/foodmap_auth_db
USER_DB_URL=jdbc:postgresql://127.0.0.1:5432/foodmap_user_db
```

全新 PostgreSQL 数据卷会通过 `deploy/postgres/init/01-create-foodmap-databases.sql` 自动创建各微服务逻辑数据库。

如果你的 PostgreSQL 容器已经有旧数据卷，初始化脚本不会再次执行。可以手动执行：

```sh
docker exec -it foodmap-postgres psql -U foodmap -d foodmap_platform
```

进入 psql 后执行：

```sql
create database foodmap_auth_db owner foodmap;
create database foodmap_user_db owner foodmap;
create database foodmap_relation_db owner foodmap;
create database foodmap_store_db owner foodmap;
create database foodmap_recommendation_db owner foodmap;
create database foodmap_community_db owner foodmap;
create database foodmap_media_db owner foodmap;
```

## 6. 后续容器化 Java 服务

当微服务 Dockerfile 补齐后，Compose 中的 Java 服务应使用：

```text
FOODMAP_PROFILE=orbstack
NACOS_SERVER_ADDR=nacos:8848
```

这样服务在容器网络里可以通过 Compose 服务名访问 Nacos、Redis、PostgreSQL、RabbitMQ 和 MinIO。

## 7. 生产配置原则

生产环境使用 `prod` profile，并且必须显式注入：

```text
SPRING_PROFILES_ACTIVE=prod
NACOS_SERVER_ADDR=<生产 Nacos 地址>
```

后续数据库、Redis、OSS、MQ 配置也采用同样方式，不在生产配置中写本地默认值。

## 8. Spring Boot 配置文件约定

每个后端微服务都使用分文件 profile 配置：

```text
application.yml
application-local.yml
application-orbstack.yml
application-prod.yml
```

`application.yml` 只放通用项；环境差异必须放到对应的 `application-{profile}.yml` 中。
