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
| Kafka logging broker | `localhost:9094`，仅在 `--profile logging` 时启动 |
| Elasticsearch logs | `http://localhost:9200`，仅在 `--profile logging` 时启动 |
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

## 6. 启动日志链路依赖

B1.5-b 起，Kafka 作为日志缓冲管道进入本地 Compose，Fluent Bit 作为本地日志采集器，Elasticsearch 作为本地 7 天热查询存储，Logstash 作为 Kafka 到 Elasticsearch 的本地消费写入器，`log-postgres-migrate` 作为独立日志库迁移器，均进入 `logging` profile，但默认不随基础依赖启动。接口访问摘要由本机或后续容器化的 `foodmap-log-service` 消费落库，Kafka 消费默认关闭，需要显式打开。

启动 Kafka、日志 topic 初始化、Fluent Bit 采集器、Elasticsearch、索引模板初始化、Logstash 消费写入器和日志库迁移：

```sh
docker compose --env-file .env.dev -f deploy/docker-compose.dev.yml --profile logging up -d kafka kafka-init fluent-bit elasticsearch elasticsearch-init logstash log-postgres-migrate
```

查看日志 topic：

```sh
docker exec -it foodmap-kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
```

默认创建：

```text
foodmap.logs.application
foodmap.logs.api-access
foodmap.logs.sql
foodmap.logs.audit
foodmap.logs.security
```

Kafka 对外暴露 `localhost:9094`，容器网络内使用 `kafka:9092`。Fluent Bit 读取 `foodmap-app-logs` 共享卷中的 `/foodmap/logs/*.log`，按日志事件名前缀分流：`api.access.*` 进入 `foodmap.logs.api-access`，`sql.execute.*` 进入 `foodmap.logs.sql`，`audit.*` 和 `security.*` 分别进入对应 topic，未匹配日志进入 `foodmap.logs.application`。

Elasticsearch 对外暴露 `localhost:9200`，`elasticsearch-init` 会写入 `foodmap-logs-hot-7d` ILM 策略和 `foodmap-logs-*` 索引模板。Logstash 消费五类 Kafka topic，按 topic 补充 `logType`，并写入每日 `foodmap-logs-YYYY.MM.dd` 索引。

当前小阶段已建立 Kafka 到 Elasticsearch 的本地热查询写入链路，并创建 `foodmap_log_db.api_access_log` 作为接口访问摘要库表；`foodmap-log-service` 已支持从 `foodmap.logs.api-access` 消费并写入该表，也支持内部查询 API、默认 15 天保留清理、全量日志 OSS 归档计划记录、归档执行状态机和真实 Elasticsearch 分页导出 gzip JSON Lines。Elasticsearch 访问已下沉到 `foodmap-common` 的统一 `SearchClient`，本地 MinIO/S3 兼容上传已通过 common `ObjectStorageClient` 接入，管理后台可通过 `/api/admin/logs/api-access` 代理查询 15 天接口访问摘要。Java 服务容器化后需要把应用日志挂载到 `foodmap-app-logs` 卷；未完成文件日志配置前，Fluent Bit 可以空跑，不影响业务服务。

本机启动管理后台日志查询代理时，可按需设置日志服务地址：

```sh
ADMIN_LOG_SERVICE_BASE_URL=http://127.0.0.1:8089
ADMIN_LOG_SERVICE_REQUEST_TIMEOUT=5s
```

本机启动接口访问摘要消费服务示例：

```sh
LOG_SERVICE_API_ACCESS_CONSUMER_ENABLED=true \
LOG_KAFKA_BROKERS=127.0.0.1:9094 \
LOG_DB_URL=jdbc:postgresql://127.0.0.1:5432/foodmap_log_db \
LOG_DB_USERNAME=foodmap \
LOG_DB_PASSWORD=foodmap \
./scripts/run-service.sh foodmap-log-service local
```

如需同时联调 15 天保留清理，可以额外设置：

```sh
LOG_SERVICE_API_ACCESS_RETENTION_CLEANUP_ENABLED=true
LOG_SERVICE_API_ACCESS_RETENTION_DAYS=15
LOG_SERVICE_API_ACCESS_RETENTION_CLEANUP_CRON="0 20 3 * * *"
```

如需联调全量日志归档计划生成，可以额外设置：

```sh
LOG_ARCHIVE_PLANNING_ENABLED=true
LOG_ARCHIVE_HOT_RETENTION_DAYS=7
LOG_ARCHIVE_BUCKET_NAME=foodmap-log-archive
LOG_ARCHIVE_OBJECT_PREFIX=logs/full
```

这只会生成 `log_archive_records` 中的 `PENDING` 计划，不会实际导出 Elasticsearch 或上传 OSS。

如需联调归档执行状态机，可以额外设置：

```sh
LOG_ARCHIVE_EXECUTION_ENABLED=true
LOG_ARCHIVE_EXECUTION_CRON="0 */10 * * * *"
```

执行器默认仍使用 Noop 导出和 Noop 对象存储适配器，只用于验证 `PENDING -> RUNNING -> SUCCESS/FAILED` 状态流转；开启 Elasticsearch 导出开关后才会真实访问 ES。对象存储上传需要额外开启 `LOG_ARCHIVE_STORAGE_UPLOAD_ENABLED=true`，且运行环境必须开启 common `ObjectStorageClient` 的 MinIO/OSS 具体实现，否则仍会回退到 Noop，不代表 OSS 或 MinIO 中已经生成真实归档文件。

如需让执行器真实访问 Elasticsearch 导出归档载荷，需要额外开启：

```sh
LOG_ARCHIVE_ELASTICSEARCH_ENABLED=true
LOG_ARCHIVE_ELASTICSEARCH_URL=http://127.0.0.1:9200
LOG_ARCHIVE_ELASTICSEARCH_PAGE_SIZE=1000
LOG_ARCHIVE_ELASTICSEARCH_MAX_PAGES=1000
```

Elasticsearch 导出适配器会按 `@timestamp` 窗口过滤、按升序分页，并用 `search_after` 持续读取直到空页，然后生成 `application/x-ndjson+gzip` 载荷。

如需让执行器把归档载荷交给 common 对象存储客户端上传，需要额外开启：

```sh
LOG_ARCHIVE_STORAGE_UPLOAD_ENABLED=true
FOODMAP_STORAGE_MINIO_ENABLED=true
FOODMAP_STORAGE_MINIO_ENDPOINT=http://127.0.0.1:9000
FOODMAP_STORAGE_MINIO_ACCESS_KEY=foodmap
FOODMAP_STORAGE_MINIO_SECRET_KEY=foodmap123456
FOODMAP_STORAGE_MINIO_PUBLIC_URL_BASE=http://127.0.0.1:9000
```

`LOG_ARCHIVE_STORAGE_UPLOAD_ENABLED` 只启用日志归档到 common `ObjectStorageClient` 的桥接；`FOODMAP_STORAGE_MINIO_ENABLED` 负责注册 common MinIO/S3 兼容对象存储实现。生产 OSS 后续会通过同一个 `ObjectStorageClient` 端口接入。

`FOODMAP_SEARCH_ELASTICSEARCH_URL`、`FOODMAP_SEARCH_ELASTICSEARCH_USERNAME`、`FOODMAP_SEARCH_ELASTICSEARCH_PASSWORD` 和 `FOODMAP_SEARCH_ELASTICSEARCH_API_KEY` 是 common SearchClient 的通用配置；`LOG_ARCHIVE_ELASTICSEARCH_*` 是日志归档场景的兼容配置入口。

如果只做普通业务接口开发，可以保持 `LOG_SERVICE_API_ACCESS_CONSUMER_ENABLED=false`、`LOG_SERVICE_API_ACCESS_RETENTION_CLEANUP_ENABLED=false`、`LOG_ARCHIVE_PLANNING_ENABLED=false` 和 `LOG_ARCHIVE_EXECUTION_ENABLED=false`，避免本机未启动 Kafka、误删测试日志摘要或生成测试归档计划。

## 7. 后续容器化 Java 服务

当微服务 Dockerfile 补齐后，Compose 中的 Java 服务应使用：

```text
FOODMAP_PROFILE=orbstack
NACOS_SERVER_ADDR=nacos:8848
```

这样服务在容器网络里可以通过 Compose 服务名访问 Nacos、Redis、PostgreSQL、RabbitMQ 和 MinIO。

## 8. 生产配置原则

生产环境使用 `prod` profile，并且必须显式注入：

```text
SPRING_PROFILES_ACTIVE=prod
NACOS_SERVER_ADDR=<生产 Nacos 地址>
```

后续数据库、Redis、OSS、MQ 配置也采用同样方式，不在生产配置中写本地默认值。

## 9. Spring Boot 配置文件约定

每个后端微服务都使用分文件 profile 配置：

```text
application.yml
application-local.yml
application-orbstack.yml
application-prod.yml
```

`application.yml` 只放通用项；环境差异必须放到对应的 `application-{profile}.yml` 中。
