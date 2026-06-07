# 后端子代理规范

## 1. 职责

后端子代理负责 FoodMap Java 微服务、API、数据库、权限、事件、缓存和基础设施实现。

必须遵守：

- CODEX-product.md
- CODEX-after.md
- AGENTS.md

## 2. 技术栈

| 类别 | 技术 |
| --- | --- |
| 语言 | Java 21 |
| 框架 | Spring Boot |
| 微服务 | Spring Cloud / Spring Cloud Alibaba |
| 网关 | Spring Cloud Gateway |
| 注册/配置 | Nacos |
| 鉴权 | Spring Security + JWT |
| ORM | MyBatis 或 MyBatis-Plus |
| 数据库 | PostgreSQL |
| 地理能力 | PostgreSQL + PostGIS |
| 缓存 | Redis |
| 消息队列 | RocketMQ 或 RabbitMQ |
| 对象存储 | MinIO 或阿里云 OSS |
| 构建 | Maven |

## 3. 负责服务

- foodmap-gateway-service
- foodmap-auth-service
- foodmap-user-service
- foodmap-relation-service
- foodmap-store-service
- foodmap-recommendation-service
- foodmap-community-service
- foodmap-media-service
- foodmap-notification-service，后续
- foodmap-admin-service，后续

## 4. 默认写入范围

默认只允许修改主代理指定的单个后端服务目录，例如：

```text
after/foodmap-auth-service
after/foodmap-common
```

不得跨服务修改，除非主代理明确授权。

## 5. 数据库规则

- 每个服务拥有独立数据库。
- 服务不能直接访问其他服务的数据表。
- 跨服务数据通过 API 或事件获取。
- 推荐服务不能直接查询关系服务数据库。
- 社区服务只能统计 PUBLIC 推荐。
- 重复出现 2 次以上的基础校验、值判断、脱敏、时间处理逻辑，应优先沉淀到 `foodmap-common` 中有明确边界的项目级工具类。
- 禁止创建无明确边界的万能工具类，如 `CommonUtils`、`StringUtils`、`DateUtils`。
- record、Command、事件信封和中间件命令中的基础参数校验应优先复用 `common.validation.Check`。
- 公共类、跨模块复用类、接口、枚举、异常、事件、配置类和中间件封装类必须提供类级 Javadoc 注释。
- 常量类和枚举类中的每一个常量、每一个枚举项都必须提供 Javadoc 注释，说明业务含义、使用场景和排查价值。
- 对外暴露的公共方法、静态工厂方法、复杂业务判断、脱敏规则、幂等规则、权限规则和状态流转必须提供方法级注释。
- 注释必须说明职责、边界和排查关注点，不能只重复代码字面含义。
- Redis、MQ、对象存储、内部服务调用必须通过项目统一封装或服务内基础设施适配器访问。
- 业务代码不得散落直接调用中间件 SDK。
- 业务代码不得随意字符串拼接打印关键业务日志。
- 涉及用户、认证、推荐、评论、文件和中间件调用的日志必须使用通用日志方法或统一日志封装。
- 日志不能输出 Token、密码、密钥、完整手机号、完整邮箱或私密推荐内容。

## 6. 验收标准

每次后端子任务至少满足：

- 服务边界符合 CODEX-after.md。
- Controller 使用 DTO，不直接暴露实体。
- 写接口从 Token 获取当前用户身份。
- 权限、归属、可见范围由后端校验。
- 重复基础校验和值判断已沉淀到明确边界的 common 工具类，未新增万能 Utils。
- 公共 API、复杂业务逻辑、中间件封装、常量项和枚举项已补充必要注释。
- 中间件访问和日志打印符合 `CODEX-after.md` 的阶段一封装规则。
- 列表接口支持分页。
- 地图接口支持边界框查询。
- 关键业务规则有测试。
- 条件允许时执行 Maven 构建或服务测试。

## 7. 禁止事项

- 不得把多个服务合并成单体。
- 不得跨服务直接访问数据库表。
- 不得让前端承担核心权限判断。
- 不得把非 PUBLIC 推荐写入全站社区统计。
- 不得擅自改变消息队列、数据库或注册中心选型。
