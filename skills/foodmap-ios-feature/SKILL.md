---
name: foodmap-ios-feature
description: Use when creating, updating, testing, or reviewing FoodMap iOS SwiftUI features, pages, navigation, MVVM state, API clients, Keychain auth, AMap integration, or frontend acceptance criteria.
---

# FoodMap iOS 功能 Skill

## 使用时机

当任务涉及 FoodMap iOS 前端时使用本 skill：

- 创建 SwiftUI App 壳。
- 实现登录、地图、门店、推荐、好友、情侣、社区、我的页面。
- 实现 ViewModel、API Client、Keychain Token 存储。
- 集成高德 iOS SDK。
- 处理页面状态、错误状态、权限展示。

## 必读文档

- `CODEX-front.md`
- `AGENTS.md`
- `.agents/frontend-agent.md`

如涉及产品行为，同时读取 `CODEX-product.md`。
如涉及 API，读取 `skills/foodmap-api-contract/SKILL.md`。

## 技术栈

- iOS
- Swift
- SwiftUI
- MVVM
- 高德 iOS SDK
- URLSession 优先
- Keychain
- UserDefaults

## 页面范围

认证：

- `LoginView`
- `RegisterView`
- `ForgotPasswordView`

地图：

- `MapHomeView`
- `MapFilterView`
- `StoreMarkerPreviewView`

门店：

- `StoreSearchView`
- `StoreCreateView`
- `StoreDetailView`

推荐：

- `AddRecommendationView`
- `EditRecommendationView`
- `RecommendationDetailView`
- `VisibilityPickerView`
- `TagPickerView`

社交和我的：

- `FriendsView`
- `FriendSearchView`
- `FriendRequestsView`
- `CoupleHomeView`
- `CoupleBindView`
- `CoupleMapView`
- `CommunityHomeView`
- `HotStoresView`
- `HotDishesView`
- `NearbyPublicView`
- `ProfileView`
- `MyRecommendationsView`
- `PrivacySettingsView`
- `AccountSettingsView`

## 强制规则

- 登录后优先进入地图页。
- 添加推荐时菜名必填且为纯文字。
- 可见范围必须在提交前明确选择。
- 前端只能展示后端返回的授权内容。
- Token 存储在 Keychain。
- 不在日志中输出 Token、密码、私密推荐内容。
- 不用前端本地判断替代后端权限校验。

## 工作流程

1. 确认页面和写入范围。
2. 如页面/导航/API 变化，先更新 `CODEX-front.md` 或 API 文档。
3. 实现 SwiftUI View、ViewModel、模型和 API 调用。
4. 补充关键状态：加载、空、错误、网络不可用、无权限。
5. 条件允许时执行 iOS 构建或测试。
6. 汇报修改文件、验证结果、剩余风险。

## 验收标准

- 页面符合 `CODEX-front.md`。
- ViewModel 状态清晰可测。
- 可见范围展示清楚。
- 图片上传失败保留已填文字并允许重试。
- 私密内容不泄露到公开页面或日志。

