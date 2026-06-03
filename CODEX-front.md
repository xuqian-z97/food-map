# CODEX 前端架构文档

## 1. 文档目的

本文档用于定义 FoodMap 的 iOS 前端架构。

后续所有前端代码都必须根据本文档生成。如果前端需求、页面结构、SDK 选择或 API 契约发生变化，必须在修改代码之前或同时更新本文档。

## 2. 平台

主要平台：

- iOS

初始实现形式：

- 原生 iOS App

推荐语言和 UI 技术：

- Swift
- SwiftUI

地图服务：

- 高德 iOS SDK

## 3. 前端目标

1. 提供以地图为中心的美食推荐体验。
2. 让添加推荐菜单的流程足够轻量。
3. 让用户在发布前清楚理解可见范围。
4. 支持个人、好友、情侣、公开四种浏览模式。
5. 保持模块化结构，方便后续加入群组和社区功能。

## 4. 用户体验原则

1. 登录后第一个功能页面应该是地图。
2. 添加推荐不能像填写复杂表单一样沉重。
3. 可见范围必须清晰，避免用户误公开。
4. App 应该更像可信任的私人美食地图，而不是嘈杂的点评平台。
5. 公开社区功能不能压过个人记录功能。

## 5. 推荐 iOS 架构

架构模式：

- MVVM
- SwiftUI 负责视图
- ViewModel 负责状态和业务编排
- Service/Client 负责 API 调用
- Repository 在必要时负责数据协调

高层目录结构：

```text
FoodMapApp
├── App
│   ├── FoodMapApp.swift
│   └── AppRouter.swift
├── Core
│   ├── Networking
│   ├── Auth
│   ├── Storage
│   ├── DesignSystem
│   ├── Extensions
│   └── Utilities
├── Features
│   ├── Auth
│   ├── Map
│   ├── Store
│   ├── Recommendation
│   ├── Friends
│   ├── Couple
│   ├── Community
│   └── Profile
└── Resources
    ├── Assets.xcassets
    └── Localizable.strings
```

## 6. 核心模块

### 6.1 App 模块

职责：

- App 启动。
- 初始化依赖容器。
- 根导航。
- 根据登录状态切换页面。

主要状态：

- 启动加载中
- 未登录
- 已登录

### 6.2 网络模块

职责：

- API Base URL 配置。
- 构建请求。
- 注入 Token。
- 错误处理。
- 响应解析。
- Refresh Token 自动重试策略。

建议类型：

```text
APIClient
APIRequest
APIResponse
AuthInterceptor
NetworkError
Endpoint
```

### 6.3 认证模块

职责：

- 登录。
- 注册。
- 刷新 Token。
- 退出登录。
- 存储 Access Token 和 Refresh Token。

Token 存储：

- Access Token 和 Refresh Token 应存储在 Keychain 中。

### 6.4 本地存储模块

职责：

- Keychain 访问。
- 轻量级本地设置。
- 可选的非敏感缓存。

MVP 本地存储：

- Token 使用 Keychain。
- 简单 UI 偏好使用 UserDefaults。

MVP 不要求离线查看地图和菜单。

### 6.5 设计系统模块

职责：

- 颜色。
- 字体。
- 按钮。
- 输入框。
- 标签组件。
- 地图点位样式。
- 可见范围标识。

设计风格：

- 干净
- 友好
- 地图优先
- 实用
- 避免营销式首页

## 7. 功能模块

### 7.1 认证功能

页面：

- LoginView
- RegisterView
- ForgotPasswordView 占位

ViewModel：

- LoginViewModel
- RegisterViewModel

接口：

- POST /api/auth/register
- POST /api/auth/login
- POST /api/auth/refresh
- GET /api/users/me

### 7.2 地图功能

页面：

- MapHomeView
- MapFilterView
- StoreMarkerPreviewView

职责：

- 渲染高德地图。
- 渲染门店点位。
- 记录当前地图视野。
- 查询当前可见门店和推荐内容。
- 切换地图数据范围。
- 打开门店详情。
- 进入添加推荐流程。

地图范围：

```text
MINE      我的
FRIENDS   好友
COUPLE    情侣
PUBLIC    公开
GROUP     群组，后续支持
```

地图查询参数：

- 地图边界框
- 缩放级别
- 数据范围
- 标签
- 关键词
- 推荐程度
- 价格范围

### 7.3 门店功能

页面：

- StoreSearchView
- StoreCreateView
- StoreDetailView

职责：

- 搜索门店。
- 展示高德 POI 结果。
- 手动创建门店。
- 展示门店详情。
- 展示当前用户可见的推荐列表。

### 7.4 推荐菜单功能

页面：

- AddRecommendationView
- EditRecommendationView
- RecommendationDetailView
- VisibilityPickerView
- TagPickerView

职责：

- 创建推荐菜单。
- 编辑推荐菜单。
- 删除推荐菜单。
- 上传可选图片。
- 选择标签。
- 选择可见范围。

必填字段：

- 菜名

可选字段：

- 推荐理由
- 图片
- 标签
- 价格
- 推荐程度

### 7.5 好友功能

页面：

- FriendsView
- FriendSearchView
- FriendRequestsView

职责：

- 搜索用户。
- 发送好友申请。
- 接受/拒绝好友申请。
- 删除好友。
- 查看好友推荐。

### 7.6 情侣功能

页面：

- CoupleHomeView
- CoupleBindView
- CoupleMapView

职责：

- 绑定情侣。
- 接受/拒绝情侣申请。
- 解除情侣关系。
- 查看情侣可见推荐。

### 7.7 社区功能

页面：

- CommunityHomeView
- HotStoresView
- HotDishesView
- NearbyPublicView

MVP 职责：

- 展示公开热门门店。
- 展示附近公开推荐。
- 展示公开门店/菜品统计。

### 7.8 我的功能

页面：

- ProfileView
- MyRecommendationsView
- PrivacySettingsView
- AccountSettingsView

职责：

- 查看和编辑个人资料。
- 查看我的推荐。
- 管理默认隐私设置。
- 退出登录。

## 8. 导航结构

推荐根结构：

```text
TabView
├── 地图
├── 社区
├── 添加
├── 好友
└── 我的
```

MVP 简化结构：

```text
TabView
├── 地图
├── 好友
└── 我的
```

添加动作可以作为地图页上的主要悬浮按钮。

## 9. API 交互模型

前端不能自行决定用户是否有权限查看某条推荐，只能做展示层辅助。

后端必须只返回当前用户有权查看的数据。

前端发送：

- 当前地图视野。
- 选中的数据范围。
- 筛选条件。

后端返回：

- 门店点位摘要。
- 门店详情。
- 当前用户可见的推荐内容。
- 允许展示的公开统计。

## 10. 状态管理

使用 SwiftUI 的可观察状态机制。

建议状态分类：

- AuthSessionState
- MapViewportState
- MapFilterState
- RecommendationDraftState
- FriendRequestState
- ProfileState

避免用一个全局对象承载所有状态。

## 11. 错误处理

常见 UI 状态：

- 加载中
- 空状态
- 错误
- 网络不可用
- 无权限
- Token 过期

示例：

- 网络不可用时提示当前功能需要联网。
- 推荐内容不可见时不展示内容，也不泄露元数据。
- 图片上传失败时允许重试，并保留已填写的文字内容。

## 12. 图片上传流程

推荐流程：

1. 用户选择图片。
2. App 压缩图片。
3. App 向媒体服务请求上传凭证。
4. App 上传图片到对象存储或媒体服务。
5. App 在创建/更新推荐时提交媒体 ID。

如果对象存储集成尚未完成，MVP 可以先通过后端直传图片。但 API 设计应预留未来切换为对象存储直传的能力。

## 13. 高德地图集成

高德 SDK 职责：

- 地图渲染。
- 当前定位。
- POI 搜索展示支持。
- 点位渲染。

后端职责：

- 保存被选择的 POI 标识。
- 持久化门店数据。
- 处理重复门店归并。
- 强制校验用户权限。

前端不能只依赖高德数据作为业务记录来源。

## 14. 安全要求

1. Token 存储在 Keychain。
2. 本地不保存密码。
3. 日志中不能输出私密推荐内容。
4. 受保护数据不能只依赖前端权限判断。
5. 退出登录时清理认证状态。

## 15. 前端初始里程碑

### F1：App 壳

- 创建 iOS 项目。
- 添加 SwiftUI App 入口。
- 添加登录状态路由。
- 添加基础 TabView。

### F2：认证

- 登录/注册页面。
- API Client。
- Token 存储。

### F3：地图

- 集成高德 SDK。
- 展示当前位置。
- 展示后端返回的门店点位。

### F4：门店和推荐

- 搜索/选择门店。
- 添加推荐菜单。
- 图片上传占位。
- 选择可见范围。

### F5：社交和公开内容

- 好友页面。
- 情侣页面。
- 公开热门列表。

## 16. 前端待决策问题

1. 最低支持 iOS 版本。
2. 使用 Alamofire 还是原生 URLSession。
3. MVP 图片是否直接上传到对象存储。
4. MVP 是否展示社区 Tab。
5. 最终视觉风格和产品名称。

