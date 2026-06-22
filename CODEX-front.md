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

当前落地工程：

- `front/FoodMapApp/FoodMapApp.xcodeproj` 已生成，可通过 Xcode 打开。
- 当前使用 SwiftUI + MVVM 生成认证测试壳。
- 登录页支持账号名、手机号或邮箱作为登录标识。
- 注册页支持账号名、手机号、邮箱、昵称、密码。
- 登录和注册请求已通过统一 `APIClient` 调用后端；B1 完整联调要求前端统一通过 Gateway 调用，不再以直连 Auth 服务作为默认联调入口。
- Access Token 和 Refresh Token 已通过 Keychain 封装持久化。
- 登录成功后进入 `MapHomeView` 地图首页壳；当前地图点位仍使用本地样例和低风险缓存，不作为门店地图业务联调证据。
- 当前代码默认服务地址仍为 `http://127.0.0.1:8081`，这是 B1 完整联调阻断项；下一步必须改为 Gateway 优先，例如本地 `http://127.0.0.1:18080`。
- 当前 `APIClient` 只覆盖登录和注册 POST；B1 完整联调前必须补齐 GET、Bearer Token、`/api/users/me`、`status` 响应字段和非 2xx 统一错误响应解析。
- 本机 Xcode 如果尚未安装 iOS 平台组件，需要先在 Xcode Settings 的 Components 中安装 iOS 平台后再执行完整模拟器构建。

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
- 高德离线底图下载状态和城市缓存策略。
- 低风险业务数据缓存和缓存元数据管理。

MVP 本地存储：

- Token 使用 Keychain。
- 简单 UI 偏好使用 UserDefaults。
- 当前选择城市、蜂窝网络下载开关、离线地图检查时间等轻量状态使用 UserDefaults。
- 门店点位摘要、门店摘要、我的推荐摘要和 PUBLIC 摘要可使用 SQLite/CoreData 或等价本地结构缓存。
- 离线底图由高德 iOS 3D 地图 SDK 管理，业务代码只保存城市、状态和更新时间等元数据。

MVP 要求支持高德离线底图；业务数据只缓存低风险摘要，不支持好友、情侣、指定用户、群组推荐详情和评论的完整离线浏览。

缓存清理规则：

- 退出登录、账号切换或 Token 失效时清理当前账号业务缓存。
- 本地最多保留 3 个城市离线底图包；超过后按最近最少使用自动删除旧城市，当前城市不能自动删除。
- 业务缓存必须带有账号、城市、scope、缓存时间和过期时间，避免不同账号或不同权限范围串读。
- 离线状态下缓存内容只读，不允许新增、编辑、评论或上传图片。

离线下载规则：

- Wi-Fi 环境下自动静默下载和更新当前城市离线底图，不提供关闭入口。
- 蜂窝网络下载由用户设置开关控制，默认关闭。
- App 打开时静默检查当前城市离线底图新版本；检查和下载不阻塞地图首页。

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
- 视觉情绪偏温馨，优先使用米白、暖橙、柿红、茶绿色等低噪声色彩。
- 主界面应像“可信任的私人美食地图”，避免高饱和大面积纯色、冷硬后台感和嘈杂点评平台感。
- 卡片、按钮和标签保持克制圆角，默认圆角不超过 8pt，突出内容而不是装饰。
- 色彩使用必须服务于隐私和范围识别：主操作用暖橙/柿红，地图和可信状态可用茶绿色，公开内容不能用过强视觉权重压过个人记录。

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

- POST /api/auth/register，B1 已落地最小契约
- POST /api/auth/login，B1 已落地最小契约
- GET /api/users/me，B1 已落地最小契约
- POST /api/auth/refresh，后续实现
- POST /api/auth/logout，后续实现

当前实现：

- `LoginView` 已落地账号名、手机号或邮箱登录入口。
- `RegisterView` 已落地测试注册入口。
- `LoginViewModel` 负责登录表单状态、错误展示和提交状态。
- `AuthSessionStore` 负责认证会话状态、Token 保存和退出登录。
- `APIClient` 当前负责登录、注册接口 JSON 请求和基础成功响应解析；尚未完成 `status` 字段建模、非 2xx 错误体解析、Bearer Token 注入和当前用户 GET 请求。
- `KeychainTokenStore` 负责 Token 的 Keychain 保存、读取和清理。
- 注册成功后当前停留在注册弹窗并展示账号 ID；登录成功后进入 `MapHomeView`。
- 当前 Token 恢复逻辑仍使用占位会话进入已登录态，B1 完整联调前必须通过 `/api/users/me` 校验真实当前用户，禁止把 `accountId/userId = 0` 作为运行时已登录会话。

B1 认证完整联调前端开发计划：

- P0：默认或推荐 Base URL 改为 Gateway，本地为 `http://127.0.0.1:18080`，保留手工覆盖能力用于排障。
- P0：`APIClient` 支持 GET/POST、Bearer Token、`X-Request-Id`、`X-Trace-Id`、`success/status/code/message/data` 成功和失败响应解析。
- P0：新增当前用户模型和 `/api/users/me` 调用，登录成功和 Token 恢复后都必须拉取真实用户资料。
- P0：401/403 时清理无效 Token 并回到登录页；400/409/500/504 和网络不可用需要展示明确错误。
- P1：新增前端网络层和会话层测试，覆盖登录、注册、当前用户、错误响应和 Token 清理。
- P1：保留登录成功、注册成功、当前用户成功、登录失败和参数错误的截图或脱敏网络摘要作为联调证据。

B1 认证完整联调安全点：

- iOS Debug 构建通过。
- 注册、登录、当前用户三条链路均通过 Gateway 发起。
- Keychain 中只保存 Token，不保存密码；联调证据不记录 Token 明文。
- `AuthSessionStore.session` 中的 `accountId/userId` 来自后端当前用户接口，不使用 0 占位。
- 前端能展示参数错误、账号冲突、未认证、权限不足、服务异常和网络不可用。
- 后端 Gateway/Auth/User 已保持 internal 外部拦截、注册失败回滚和 accountId 归属校验回归通过。

### 7.2 地图功能

页面：

- MapHomeView
- MapFilterView
- StoreMarkerPreviewView
- CityPickerView
- OfflineStoreSummaryView

职责：

- 渲染高德地图。
- 渲染门店点位。
- 记录当前地图视野。
- 查询当前可见门店和推荐内容。
- 切换地图数据范围。
- 选择和切换当前城市。
- 管理当前城市高德离线底图下载、更新和状态展示。
- 网络不可用或接口超时时展示最近缓存的门店点位摘要。
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
- RecommendationCommentsView
- CommentComposerView
- VisibilityPickerView
- TagPickerView

职责：

- 创建推荐菜单。
- 编辑推荐菜单。
- 删除推荐菜单。
- 查看推荐菜单评论。
- 发布文字评论。
- 上传可选评论图片，单条评论最多 3 张。
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

离线和弱网降级：

- 地图页门店查询超时时间为 5 到 8 秒。
- 普通查询超时时间约 8 秒。
- 提交类接口超时时间为 10 到 15 秒。
- 后端接口超时、DNS 失败、连接失败或系统网络不可用时，前端统一进入网络不可用降级状态。
- 降级状态下高德底图继续显示；如果存在缓存门店点位，展示缓存点位并允许进入缓存门店摘要页。
- 缓存门店摘要页顶部先显示“离线缓存”标识，后续可根据体验决定是否弱化。
- 好友、情侣、指定用户、群组推荐详情和评论内容不做离线展示，页面提示联网后查看最新授权内容。

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
- 当前城市离线底图下载、暂停、删除、版本检查和更新。

后端职责：

- 保存被选择的 POI 标识。
- 持久化门店数据。
- 处理重复门店归并。
- 强制校验用户权限。

前端不能只依赖高德数据作为业务记录来源。

离线地图集成规则：

- 使用高德 iOS 3D 地图 SDK 的离线地图能力；如果工程尚未接入真实 SDK，先通过适配器协议保留能力边界。
- App 首次启动后请求定位，定位成功则下载定位城市离线底图；拒绝定位或定位失败时进入城市选择。
- App 每次打开时尝试定位当前城市；定位城市与用户当前选择城市不一致时，弹窗询问是否切换。
- Wi-Fi 自动静默更新当前城市离线底图；蜂窝网络下载默认关闭，由用户设置显式开启。
- 城市切换后触发目标城市离线底图下载，并更新最近使用时间。
- 离线底图最多保留 3 个城市，超过后按最近最少使用清理非当前城市。

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
