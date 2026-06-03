# 安全组检查清单

## 1. ECS2 公网入站

允许：

- [ ] 80/tcp：HTTP
- [ ] 443/tcp：HTTPS
- [ ] 22/tcp：SSH，仅允许固定运维 IP

禁止公网开放：

- [ ] 5432/tcp PostgreSQL
- [ ] 6379/tcp Redis
- [ ] 8848/tcp Nacos
- [ ] 9848/tcp Nacos gRPC
- [ ] 5672/tcp RabbitMQ
- [ ] 15672/tcp RabbitMQ Management
- [ ] 8080/tcp Gateway
- [ ] 所有内部微服务端口

## 2. ECS1 公网入站

允许：

- [ ] 22/tcp：SSH，仅允许固定运维 IP

默认不开放：

- [ ] Redis
- [ ] MySQL
- [ ] 监控端口

## 3. 跨云通信

如果 ECS1 和 ECS2 需要通信：

- [ ] 先建立 WireGuard/VPN。
- [ ] 使用隧道私有 IP。
- [ ] 不使用裸公网 IP 访问 Redis/PostgreSQL/Nacos/RabbitMQ。

## 4. OSS

- [ ] AccessKey 不写入代码仓库。
- [ ] 使用最小权限 RAM 用户。
- [ ] Bucket 权限按业务需要设置。
- [ ] 上传文件类型和大小由 media-service 校验。

