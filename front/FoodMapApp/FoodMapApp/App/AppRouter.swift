import SwiftUI

/// 应用根路由，根据认证会话在登录页和地图首页之间切换。
struct AppRouter: View {
    @StateObject private var sessionStore = AuthSessionStore()

    var body: some View {
        Group {
            if let session = sessionStore.session {
                MapHomeView(session: session) {
                    sessionStore.signOut()
                }
            } else {
                LoginView()
                    .environmentObject(sessionStore)
            }
        }
        .animation(.easeInOut(duration: 0.2), value: sessionStore.session != nil)
    }
}
