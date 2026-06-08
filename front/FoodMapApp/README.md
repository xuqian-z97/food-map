# FoodMap iOS 登录测试工程

## 打开方式

使用 Xcode 打开：

```text
front/FoodMapApp/FoodMapApp.xcodeproj
```

选择 `FoodMapApp` scheme，运行到 iOS Simulator。

如果 Xcode 提示没有可用 iOS 平台或模拟器运行时，需要先打开 Xcode Settings 的 Components 页面，安装对应的 iOS 平台组件。当前命令行环境的 `xcode-select` 可能仍指向 Command Line Tools，这不影响直接用 Xcode 打开工程。

## 后端地址

登录页默认服务地址：

```text
http://127.0.0.1:8081
```

如果认证服务用其他端口启动，可以在登录页的“服务地址”输入框中修改，例如：

```text
http://127.0.0.1:18081
```

iOS Simulator 访问 `127.0.0.1` 时会连到 Mac 本机。真机测试时需要改成 Mac 的局域网 IP。

## 后端启动

先启动本地依赖：

```sh
docker compose --env-file .env.dev -f deploy/docker-compose.dev.yml up -d
```

再启动认证服务：

```sh
cd after
NACOS_DISCOVERY_ENABLED=false ./scripts/run-service.sh foodmap-auth-service local
```

认证服务默认端口是 `8081`。

## 当前页面

- 登录页：调用 `POST /api/auth/login`
- 注册页：调用 `POST /api/auth/register`
- 登录成功页：地图占位页
- Token 存储：Keychain
