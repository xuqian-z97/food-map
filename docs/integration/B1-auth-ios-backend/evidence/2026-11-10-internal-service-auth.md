# T-032 内部接口签名实测记录

时间：2026-11-10 13:55-13:58（Asia/Shanghai）
环境：本地 auth 8081 / user 8082 / log 8089 / gateway 8080，均使用本次构建。

## 拒绝分支（直接访问被调服务，绕过网关）

| 请求 | 结果 |
| --- | --- |
| `POST http://127.0.0.1:8082/internal/users/provision` 无签名头 | 401 `UNAUTHORIZED`，message=`缺少内部服务身份` |
| `GET http://127.0.0.1:8089/internal/logs/api-access` 无签名头 | 401 `UNAUTHORIZED`，message=`缺少内部服务身份` |
| 同一组签名头重放第二次 | 401 `UNAUTHORIZED`，message=`内部服务请求重放` |
| 时间戳为 10 分钟前 | 401 `UNAUTHORIZED`，message=`内部服务请求已过期` |
| `foodmap-auth-service` 签名访问 `/internal/logs/**` | 403 `FORBIDDEN`，message=`内部接口不允许当前服务调用` |

## 放行与豁免

| 请求 | 结果 |
| --- | --- |
| `foodmap-admin-service` 签名 `GET /internal/logs/api-access` | 200，返回分页访问摘要 |
| `GET http://127.0.0.1:8082/internal/users/health` 无签名 | 200，健康检查免签名 |

## 调用链

`b1-logout` 场景注册→登录→登出 30/30 通过。注册由 auth 通过 Feign 调用
`/internal/users/provision`，说明出站签名与 user 服务入站校验已打通。

## 未覆盖

- admin 服务未在本机启动，管理后台到日志服务的完整 HTTP 链路未端到端执行；
  `HttpLogApiAccessLogClientTest` 已断言签名头与签名原文一致。
- 日志服务 8089 为本次临时实例，未接入容器编排。
