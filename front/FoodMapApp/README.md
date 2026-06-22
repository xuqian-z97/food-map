# FoodMap iOS 登录测试工程

## 打开方式

使用 Xcode 打开：

```text
front/FoodMapApp/FoodMapApp.xcodeproj
```

选择 `FoodMapApp` scheme，运行到 iOS Simulator。

如果 Xcode 提示没有可用 iOS 平台或模拟器运行时，需要先打开 Xcode Settings 的 Components 页面，安装对应的 iOS 平台组件。当前命令行环境的 `xcode-select` 可能仍指向 Command Line Tools，这不影响直接用 Xcode 打开工程。

## 后端地址

B1 完整前后端联调时，iOS 应通过 Gateway 访问后端：

```text
http://127.0.0.1:18080
```

当前代码里的历史默认值仍可能是 Auth 直连端口 `http://127.0.0.1:8081`。正式执行 B1 iOS 联调前，需要先把默认值调整为 Gateway，或在登录页“服务地址”输入框中手工改成 Gateway 地址。

Auth/User 直连地址只用于后端排障，例如：

```text
http://127.0.0.1:18081
http://127.0.0.1:18082
```

iOS Simulator 访问 `127.0.0.1` 时会连到 Mac 本机。真机测试时需要改成 Mac 的局域网 IP。

## 后端启动

先启动本地依赖：

```sh
docker compose --env-file .env.dev -f deploy/docker-compose.dev.yml up -d
```

再启动认证、用户和网关服务。B1 本地联调推荐端口：

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

如使用 Nacos 服务发现，可按后端联调文档启动对应 profile。

## 当前页面

- 登录页：调用 Gateway `POST /api/auth/login`
- 注册页：调用 Gateway `POST /api/auth/register`
- B1 待补：登录成功和 Token 恢复后调用 Gateway `GET /api/users/me`
- 登录成功页：`MapHomeView` 地图首页壳
- Token 存储：Keychain
