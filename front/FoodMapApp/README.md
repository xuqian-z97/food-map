# FoodMap iOS 登录测试工程

## 打开方式

使用 Xcode 打开：

```text
front/FoodMapApp/FoodMapApp.xcodeproj
```

选择 `FoodMapApp` scheme，运行到 iOS Simulator。

如果 Xcode 提示没有可用 iOS 平台或模拟器运行时，需要先打开 Xcode Settings 的 Components 页面，安装对应的 iOS 平台组件。当前命令行环境的 `xcode-select` 可能仍指向 Command Line Tools，这不影响直接用 Xcode 打开工程。

## 后端地址

B1 完整前后端联调时，iOS 统一通过 Gateway 访问后端，端口与 `after/` 各服务的默认端口块一致：

```text
Gateway http://127.0.0.1:8080
Auth    http://127.0.0.1:8081（仅后端排障用，App 不直连）
User    http://127.0.0.1:8082（仅后端排障用，App 不直连）
```

服务端口分配（`application.yml` 默认值，可用 `SERVER_PORT` 覆盖）：

| 服务 | 端口 | 服务 | 端口 |
| --- | --- | --- | --- |
| gateway | 8080 | recommendation | 8085 |
| auth | 8081 | community | 8086 |
| user | 8082 | media | 8087 |
| relation | 8083 | admin | 8088 |
| store | 8084 | log | 8089 |

登录页和注册页默认使用 Gateway 地址，且“服务地址”输入框仍可编辑。若本机保存过早期 Auth 直连值 `http://127.0.0.1:8081` 或旧文档口径 `http://127.0.0.1:18080`，App 会自动回到当前默认值；如需排障，可手工改为其他本地或局域网 Gateway 地址。

iOS Simulator 访问 `127.0.0.1` 时会连到 Mac 本机。真机测试时需要改成 Mac 的局域网 IP。

## 后端启动

先启动本地依赖：

```sh
docker compose --env-file .env.dev -f deploy/docker-compose.dev.yml up -d
```

推荐方式：按默认端口和 Nacos 服务发现启动，无需覆盖任何端口环境变量。

```sh
cd after
./scripts/run-service.sh foodmap-user-service local
./scripts/run-service.sh foodmap-auth-service local
./scripts/run-service.sh foodmap-gateway-service local
```

可选方式：不接 Nacos，网关用静态路由把请求直接转发到固定地址。这种方式使用 `18080/18081/18082`，
与默认端口块故意错开，适合排查服务发现问题。

```sh
cd after
NACOS_DISCOVERY_ENABLED=false SERVER_PORT=18082 ./scripts/run-service.sh foodmap-user-service local
NACOS_DISCOVERY_ENABLED=false SERVER_PORT=18081 AUTH_USER_SERVICE_BASE_URL=http://127.0.0.1:18082 ./scripts/run-service.sh foodmap-auth-service local
NACOS_DISCOVERY_ENABLED=false SERVER_PORT=18080 \
SPRING_CLOUD_GATEWAY_ROUTES_0_ID=auth-service \
SPRING_CLOUD_GATEWAY_ROUTES_0_URI=http://127.0.0.1:18081 \
SPRING_CLOUD_GATEWAY_ROUTES_0_PREDICATES_0='Path=/api/auth/**,/internal/auth/**' \
SPRING_CLOUD_GATEWAY_ROUTES_1_ID=user-service \
SPRING_CLOUD_GATEWAY_ROUTES_1_URI=http://127.0.0.1:18082 \
SPRING_CLOUD_GATEWAY_ROUTES_1_PREDICATES_0='Path=/api/users/**,/internal/users/**' \
./scripts/run-service.sh foodmap-gateway-service local
```

用可选方式启动时，需要把 App 的服务地址改成 `http://127.0.0.1:18080`。

## 当前页面

- 登录页：调用 Gateway `POST /api/auth/login`
- 注册页：调用 Gateway `POST /api/auth/register`
- 当前用户：登录成功和 Token 恢复后调用 Gateway `GET /api/users/me`，请求携带 `Authorization: Bearer <access-token>`、`X-Request-Id` 和 `X-Trace-Id`
- 登录成功页：`MapHomeView` 地图首页壳
- Token 存储：Keychain
