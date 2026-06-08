# CODEX 项目迭代文档

## 1. 文档目的

本文档用于定义 FoodMap 项目的生成方式、迭代方式和维护规则。

项目必须根据以下产品和架构文档逐步构建：

- CODEX-product.md
- CODEX-front.md
- CODEX-after.md
- CODEX-gen.md
- AGENTS.md
- `.agents/*.md`
- `skills/*/SKILL.md`
- `harness/*.md`

这些文件是后续代码生成的事实依据。

## 2. 强制迭代规则

后续每次变更都必须遵守：

1. 先阅读相关 CODEX 文档。
2. 如果需求影响产品行为，必须更新 CODEX-product.md。
3. 如果需求影响 iOS 架构、页面、SDK 或 UI 流程，必须更新 CODEX-front.md。
4. 如果需求影响后端服务、API、数据归属或基础设施，必须更新 CODEX-after.md。
5. 如果需求影响里程碑、开发顺序或项目规则，必须更新 CODEX-gen.md。
6. 如果需求影响开发代理职责、前后端验收标准或协作规则，必须更新 AGENTS.md。
7. 如果需求影响项目专属 skills，必须更新 `skills/*/SKILL.md`。
8. 如果需求影响多代理约束、验收流程或自动检查脚本，必须更新 `harness/`。
9. 根据更新后的文档生成或修改代码。
10. 尽可能通过测试或构建命令验证变更。

代码不能偏离这些文档。

## 3. GitHub 同步规则

项目远程仓库：

```text
https://github.com/xuqian-z97/food-map.git
```

后续文档和代码都需要与该 GitHub 仓库保持同步。

同步规则：

1. 本地生成或修改文档、代码后，应先检查 git 状态。
2. 提交前确认变更符合 CODEX 文档约束。
3. 每次重要迭代完成后，应提交到 git。
4. 提交后应推送到远程仓库。
5. 如果远程仓库已有新提交，应先拉取并处理冲突，再继续推送。
6. 不得使用会丢弃用户改动的破坏性 git 操作，除非用户明确要求。

## 4. 当前产品方向

FoodMap 是一款 iOS 优先的美食推荐地图 App。

当前方向：

- 个人美食地图。
- 好友共享。
- 情侣共享。
- 可选择公开推荐。
- 公开推荐进入社区统计。
- 后端使用 Java 微服务。
- 每个后端微服务拥有独立数据库。
- 地图数据使用高德地图。
- MVP 不要求离线使用。

## 5. 当前仓库阶段

当前阶段：

```text
Stage 1：后端微服务骨架已完成
```

已完成交付物：

- 产品文档。
- 前端架构文档。
- 后端架构文档。
- 项目迭代文档。
- AGENTS 协作规范。
- .agents 子代理规范。
- skills 项目专属工作流。
- harness 多代理约束和验收脚手架。
- `after/` Java 微服务 Maven 多模块骨架。
- 本地隔离开发环境配置和 profile 切换约定。

当前尚未生成 `front/` iOS 应用代码；后端暂为可编译骨架，尚未实现业务 API、数据库迁移和持久化逻辑。

当前迭代新增约束：

- 数据库结构对应 Java 类作为持久化实体存放在 `infrastructure.persistence.entity` 中。
- 固定字段由 `foodmap-common` 的 `BaseEntity` 承载。
- 持久化实体、DTO、VO 必须分离，Controller 不直接暴露 Entity。

## 6. 计划中的仓库结构

推荐未来结构：

```text
food-map
├── .agents
│   ├── frontend-agent.md
│   ├── backend-agent.md
│   ├── api-agent.md
│   ├── qa-agent.md
│   └── docs-agent.md
├── skills
│   ├── foodmap-doc-sync
│   ├── foodmap-backend-service
│   ├── foodmap-ios-feature
│   ├── foodmap-api-contract
│   └── foodmap-qa-check
├── harness
│   ├── README.md
│   ├── agent-task-template.md
│   ├── acceptance-checklist.md
│   ├── file-boundary-rules.md
│   ├── api-contract-checklist.md
│   └── scripts
├── deploy
│   ├── README.md
│   ├── architecture.md
│   ├── env.example
│   ├── docker-compose.ecs2.yml
│   ├── nginx
│   └── checklists
├── AGENTS.md
├── CODEX-product.md
├── CODEX-front.md
├── CODEX-after.md
├── CODEX-gen.md
├── after
│   ├── pom.xml
│   ├── foodmap-common
│   ├── foodmap-gateway-service
│   ├── foodmap-auth-service
│   ├── foodmap-user-service
│   ├── foodmap-relation-service
│   ├── foodmap-store-service
│   ├── foodmap-recommendation-service
│   ├── foodmap-community-service
│   ├── foodmap-media-service
│   └── docker-compose.yml
├── front
│   └── FoodMapApp
└── docs
    ├── api
    ├── database
    └── design
```

## 7. 开发策略

项目应尽量按纵向业务切片推进。

推荐顺序：

1. 文档基础建设。
2. 后端基础设施骨架。
3. 认证服务和用户服务。
4. iOS App 壳和认证流程。
5. 门店服务和高德集成占位。
6. iOS 地图壳。
7. 推荐服务。
8. iOS 添加推荐流程。
9. 关系服务。
10. iOS 好友和情侣流程。
11. 社区服务和公开统计。
12. 媒体服务和图片上传。

这个顺序可以让项目较早变得可用，同时保持微服务边界清晰。

## 8. 后端生成计划

### B0：后端骨架

交付物：

- after/pom.xml
- foodmap-common
- 各服务模块
- 基础 Spring Boot 应用
- Docker Compose 基础配置
- `application-{profile}.yml` 分文件环境配置
- 标准包结构

服务：

- gateway
- auth
- user
- relation
- store
- recommendation
- community
- media

### B1：认证和用户

交付物：

- 注册接口。
- 登录接口。
- JWT 工具。
- Refresh Token 持久化。
- 用户资料接口。
- 服务独立数据库。

### B2：门店

交付物：

- 门店表结构。
- 门店创建接口。
- 门店搜索接口。
- 高德 POI 集成占位。
- 门店详情接口。
- 地图视野查询接口。

### B3：推荐

交付物：

- 推荐表结构。
- 推荐创建/编辑/删除接口。
- 标签。
- 图片引用。
- 评论表结构。
- 评论列表、发布、删除接口。
- 评论图片引用，单条评论最多 3 张图片。
- 可见范围。
- 可见规则。

### B4：关系

交付物：

- 好友申请。
- 好友关系。
- 情侣申请。
- 情侣关系。
- 内部关系校验接口。

### B5：社区

交付物：

- 推荐事件。
- 事件消费者。
- 公开门店统计。
- 公开菜品统计。
- 热门门店接口。
- 附近公开接口。

### B6：媒体

交付物：

- 上传接口。
- 图片元数据。
- MinIO/OSS 集成占位。
- 媒体引用校验。

## 9. 前端生成计划

### F0：iOS App 壳

交付物：

- SwiftUI App 项目。
- 根路由。
- 基础 TabView。
- 设计系统基础。

### F1：认证

交付物：

- 登录页。
- 注册页。
- API Client。
- Token 存储。
- 认证会话路由。

### F2：地图

交付物：

- 高德地图集成。
- 地图首页。
- 地图范围选择器。
- 点位渲染。
- 门店预览。

### F3：添加推荐

交付物：

- 门店搜索页。
- 添加推荐表单。
- 可见范围选择器。
- 标签选择器。
- 图片选择占位。

### F4：门店详情

交付物：

- 门店详情页。
- 可见推荐列表。
- 公开统计展示。

### F5：社交

交付物：

- 好友列表。
- 好友搜索。
- 好友申请处理。
- 情侣绑定。
- 情侣地图入口。

### F6：社区

交付物：

- 热门门店。
- 热门菜品。
- 附近公开列表。

## 10. 数据和 API 契约规则

在生成会调用后端 API 的前端页面之前：

1. 后端 API 契约必须先文档化。
2. 请求和响应 DTO 在当前迭代内应足够稳定。
3. 前端模型必须与 API DTO 匹配。
4. 当后端服务尚未实现时，可以临时使用 Mock 数据。
5. 后端 API 实现后，应替换 Mock 数据。

## 11. 测试策略

### 10.1 后端测试

预期测试：

- 领域规则单元测试。
- 用例服务测试。
- 关键 SQL 的 Repository 测试。
- Controller API 测试。
- 可见范围集成测试。

重点测试区域：

- 认证。
- 可见权限。
- 好友/情侣关系校验。
- 社区统计。

### 10.2 前端测试

预期测试：

- ViewModel 测试。
- API Client 测试。
- 重要页面的快照或 UI 测试，条件允许时执行。

重点测试区域：

- 认证会话状态。
- 推荐草稿状态。
- 可见范围选择器。
- 地图筛选。

## 12. 文档更新示例

### 示例一：提前加入群组共享

需要更新：

- CODEX-product.md：MVP 范围和群组用户流程。
- CODEX-front.md：群组页面和导航。
- CODEX-after.md：关系服务群组接口和表结构。
- CODEX-gen.md：里程碑顺序。

### 示例二：更换消息队列

需要更新：

- CODEX-after.md：技术栈和事件流。
- CODEX-gen.md：后端生成计划。

### 示例三：增加评论功能

需要更新：

- CODEX-product.md：评论行为和可见范围。
- CODEX-front.md：评论 UI。
- CODEX-after.md：新增服务或扩展推荐服务。
- CODEX-gen.md：里程碑位置。

## 13. 初始实现建议

完成文档基础建设后，下一步推荐：

```text
优先实现认证服务和用户服务基础能力。
```

原因：

- 认证和用户能力是登录、好友、情侣、推荐归属的基础。
- API 契约会指导后续 `front/` iOS App 开发。
- 认证、用户、门店、推荐服务构成项目主干。

认证和用户基础能力完成后：

```text
生成 iOS App 壳。
```

部署准备：

```text
MVP 阶段使用 Docker Compose 部署到 ECS2，ECS1 作为辅助节点。
两台服务器不在同一云厂商或 VPC 时，不通过裸公网访问 Redis、数据库、Nacos、RabbitMQ。
```

## 14. 当前里程碑状态

| 里程碑 | 状态 |
| --- | --- |
| 文档基础建设 | 已完成 |
| AGENTS 协作规范 | 已完成 |
| 多代理子规范 | 已完成 |
| 项目专属 Skills | 已完成 |
| Harness 约束和验收脚手架 | 已完成 |
| 部署方案和模板 | 已完成 |
| 后端骨架 | 已完成 |
| 本地隔离开发环境配置 | 已完成 |
| 数据库设计规则 | 已完成 |
| MVP 首批表结构草案 | 已完成 |
| 推荐评论体系设计 | 已完成 |
| 后端微服务企业级开发基线 | 已完成 |
| 阶段一中间件封装和统一日志规则 | 已完成 |
| 阶段一 foodmap-common 首批基线代码 | 已完成 |
| 后端代码注释规则和首批注释补齐 | 已完成 |
| 阶段一 common.validation.Check 校验工具 | 已完成 |
| iOS App 壳 | 未开始 |
| 认证/用户接口 | 未开始 |
| 地图壳 | 未开始 |
| 推荐流程 | 未开始 |
| 关系流程 | 未开始 |
| 社区统计 | 未开始 |
| 媒体上传 | 未开始 |

## 15. 项目待决策问题

1. 使用 Maven 还是 Gradle。当前建议：Maven。
2. 使用 RocketMQ 还是 RabbitMQ。当前建议：如果采用 Spring Cloud Alibaba，优先 RocketMQ；如果优先简化本地环境，使用 RabbitMQ。
3. 使用 MyBatis 还是 MyBatis-Plus。当前建议：MyBatis-Plus 提升开发速度，复杂 SQL 使用 MyBatis XML 控制。
4. 最低支持 iOS 版本。
5. MVP 是否展示社区 Tab。
