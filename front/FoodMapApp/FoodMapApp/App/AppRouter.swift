import SwiftUI

/// 应用根路由，根据认证会话在登录页和地图首页之间切换。
struct AppRouter: View {
    @StateObject private var sessionStore = AuthSessionStore()

    var body: some View {
        Group {
            if sessionStore.isRestoringSession {
                startupLoadingView
            } else if let session = sessionStore.session {
                MapHomeView(
                    session: session,
                    onRefreshCurrentUser: {
                        try await sessionStore.refreshCurrentUserForDebug()
                    },
                    onSignOut: {
                        sessionStore.signOut()
                    }
                )
            } else {
                loginRoute
            }
        }
        .task {
            await sessionStore.restoreSessionIfNeeded()
        }
        .animation(.easeInOut(duration: 0.2), value: sessionStore.session != nil)
    }

    private var loginRoute: some View {
        ZStack(alignment: .top) {
            LoginView()
                .environmentObject(sessionStore)

            if let message = sessionStore.restoreErrorMessage {
                Label(message, systemImage: "exclamationmark.triangle.fill")
                    .font(.callout)
                    .foregroundStyle(FoodMapTheme.persimmon)
                    .padding(12)
                    .background(FoodMapTheme.ricePaper.opacity(0.96))
                    .clipShape(RoundedRectangle(cornerRadius: FoodMapTheme.cardCornerRadius, style: .continuous))
                    .shadow(color: .black.opacity(0.08), radius: 10, x: 0, y: 6)
                    .padding(.horizontal, 20)
                    .padding(.top, 18)
                    .frame(maxWidth: 560)
            }
        }
    }

    private var startupLoadingView: some View {
        ZStack {
            FoodMapTheme.ricePaper
                .ignoresSafeArea()

            VStack(spacing: 14) {
                ProgressView()
                    .tint(FoodMapTheme.persimmon)
                Text("正在恢复登录状态")
                    .font(.callout.weight(.semibold))
                    .foregroundStyle(FoodMapTheme.mutedInk)
            }
        }
    }
}
