import SwiftUI

/// FoodMap iOS 应用入口，负责创建根窗口并交给 AppRouter 决定登录态路由。
@main
struct FoodMapApp: App {
    var body: some Scene {
        WindowGroup {
            AppRouter()
        }
    }
}
