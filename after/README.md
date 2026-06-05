# FoodMap 后端

## 1. 目录目的

本目录存放 FoodMap 的 Java 微服务后端骨架。

后端实现必须遵守：

- `CODEX-after.md`
- `AGENTS.md`
- `.agents/backend-agent.md`
- `skills/foodmap-backend-service/SKILL.md`
- `harness/`

## 2. 技术基线

- Java 21
- Maven
- Spring Boot 3.3.x
- Spring Cloud 2023.0.x
- Spring Cloud Alibaba Nacos Discovery
- `deploy/` 下的 Docker Compose 部署和本地开发模板

## 3. 模块

```text
after
├── pom.xml
├── foodmap-common
├── foodmap-gateway-service
├── foodmap-auth-service
├── foodmap-user-service
├── foodmap-relation-service
├── foodmap-store-service
├── foodmap-recommendation-service
├── foodmap-community-service
└── foodmap-media-service
```

## 4. 当前范围

当前迭代只生成可编译的服务骨架：

- Maven 父工程和模块。
- Spring Boot 启动入口。
- 基础内部 health/info 接口。
- 网关路由占位。
- 统一 API 响应和枚举。
- `application-local.yml / application-orbstack.yml / application-prod.yml` 分文件 profile 配置。

业务 API、数据库迁移、服务 DTO 和持久化逻辑将在后续迭代中实现。

## 5. 环境 Profile

启动环境优先级：

```text
SPRING_PROFILES_ACTIVE > FOODMAP_PROFILE > local
```

| Profile | 使用方式 |
| --- | --- |
| local | 默认值，适合 IDEA/Maven 在 Mac 本机启动，依赖通过 `127.0.0.1` 访问 |
| orbstack | 适合后续服务运行在 Docker/OrbStack 容器网络，依赖通过 Compose 服务名访问 |
| prod | 生产环境，必须显式注入配置 |

配置文件职责：

```text
application.yml           通用配置，如端口、服务名、Actuator、网关路由
application-local.yml     IDEA/Maven 本机启动配置
application-orbstack.yml  Docker/OrbStack 容器网络配置
application-prod.yml      生产环境配置占位
```

## 6. 构建

在本目录执行：

```sh
mvn validate
```

更严格的编译：

```sh
mvn -DskipTests compile
```

从项目根目录执行：

```sh
./harness/scripts/run-all.sh
```

## 7. 网关验证

服务注册到 Nacos 后，可以通过网关验证内部健康检查路由：

```sh
curl http://localhost:8080/internal/auth/health
curl http://localhost:8080/internal/users/health
```

`/api/**` 路径预留给正式业务 API，当前骨架阶段的健康检查路径统一使用 `/internal/**`。

## 8. 启动单个服务

示例：

```sh
./scripts/run-service.sh foodmap-auth-service local
```

不传 profile 时默认使用 `local`。

脚本会先安装当前服务及其依赖模块，再只对目标服务执行 `spring-boot:run`，避免 Spring Boot 插件误作用到父 POM。
