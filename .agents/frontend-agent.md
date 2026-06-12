# 前端子代理规范

## 1. 职责

前端子代理负责 FoodMap iOS App 的页面、交互、状态管理、网络请求、高德地图集成和前端验收。

必须遵守：

- CODEX-product.md
- CODEX-front.md
- AGENTS.md

## 2. 技术栈

| 类别 | 技术 |
| --- | --- |
| 平台 | iOS |
| 语言 | Swift |
| UI | SwiftUI |
| 架构 | MVVM |
| 地图 | 高德 iOS SDK |
| 网络 | URLSession 优先 |
| Token 存储 | Keychain |
| 简单偏好 | UserDefaults |

## 3. 负责页面

- LoginView
- RegisterView
- ForgotPasswordView
- MapHomeView
- MapFilterView
- StoreMarkerPreviewView
- StoreSearchView
- StoreCreateView
- StoreDetailView
- AddRecommendationView
- EditRecommendationView
- RecommendationDetailView
- VisibilityPickerView
- TagPickerView
- FriendsView
- FriendSearchView
- FriendRequestsView
- CoupleHomeView
- CoupleBindView
- CoupleMapView
- CommunityHomeView
- HotStoresView
- HotDishesView
- NearbyPublicView
- ProfileView
- MyRecommendationsView
- PrivacySettingsView
- AccountSettingsView

## 4. 默认写入范围

默认只允许修改：

```text
front/FoodMapApp
```

如果需要修改 API 文档、后端 DTO 或项目根文档，必须由主代理重新授权。

## 5. 验收标准

每次前端子任务至少满足：

- 页面符合 CODEX-front.md 的导航和模块划分。
- 登录后进入地图页。
- 添加推荐流程中菜名为必填纯文字。
- 可见范围选择清晰可见。
- 只展示后端返回的授权内容。
- Token 存储在 Keychain。
- 不在日志中输出 Token、密码、私密推荐内容。
- 关键 ViewModel 逻辑可测试。
- 条件允许时执行 iOS 构建或相关测试。

## 5.1 联调观察职责

前后端联调阶段，前端子代理默认作为前端观察子代理参与。

必须检查并反馈：

- `integration-plan.md` 中页面、交互、导航路径和状态要求是否与 iOS 实现一致。
- 前端网络请求路径、方法、请求体、响应解析是否与 API 契约一致。
- 登录态、Token 存储、退出登录、账号切换、本地缓存和离线状态处理是否符合约定。
- 是否保留截图、录屏、网络请求摘要、Xcode 日志摘要或测试结果作为联调证据。
- 是否覆盖加载中、成功、失败、空状态、无权限、网络不可用、Token 过期、图片上传失败等状态。
- `issue-log.md` 中前端现象是否包含复现步骤、页面状态、请求摘要和用户可见结果。

前端观察子代理默认不修改代码。如需修复前端问题，必须由主代理重新授权写入范围。

## 6. 禁止事项

- 不得自行更换技术栈。
- 不得绕过后端权限校验。
- 不得把私密、好友、情侣、群组内容混入公开社区展示。
- 不得修改后端服务或数据库文件，除非主代理明确授权。
