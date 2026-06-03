# 部署检查清单

## 1. ECS2 初始化

- [ ] 安装 Docker。
- [ ] 安装 Docker Compose plugin。
- [ ] 配置防火墙和安全组。
- [ ] 创建部署目录。
- [ ] 上传 `.env`。
- [ ] 检查 `.env` 不进入 Git。

## 2. 基础组件

- [ ] PostgreSQL/PostGIS 启动。
- [ ] Redis 启动。
- [ ] RabbitMQ 启动。
- [ ] Nacos standalone 启动。
- [ ] Nginx 启动。

## 3. 数据库

- [ ] 创建 `foodmap_auth_db`。
- [ ] 创建 `foodmap_user_db`。
- [ ] 创建 `foodmap_relation_db`。
- [ ] 创建 `foodmap_store_db`。
- [ ] 创建 `foodmap_recommendation_db`。
- [ ] 创建 `foodmap_community_db`。
- [ ] 创建 `foodmap_media_db`。
- [ ] `foodmap_store_db` 启用 PostGIS。

## 4. 应用服务

- [ ] Gateway 启动。
- [ ] Auth service 启动。
- [ ] User service 启动。
- [ ] Relation service 启动。
- [ ] Store service 启动。
- [ ] Recommendation service 启动。
- [ ] Community service 启动。
- [ ] Media service 启动。

## 5. OSS

- [ ] Bucket 创建完成。
- [ ] RAM 用户权限最小化。
- [ ] media-service 可以生成上传凭证。
- [ ] 图片上传后可保存 metadata。

## 6. 验收

- [ ] `/api/` 可以访问 Gateway。
- [ ] 注册接口可用。
- [ ] 登录接口可用。
- [ ] 门店搜索接口可用。
- [ ] 推荐创建接口可用。
- [ ] PUBLIC 推荐能进入社区统计。
- [ ] 私密推荐不进入社区统计。

