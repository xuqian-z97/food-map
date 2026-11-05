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
                        await sessionStore.signOut()
                    }
                )
            } else {
                loginRoute
            }
        }
        .task {
            await sessionStore.restoreSessionIfNeeded()
            #if DEBUG
            await LaunchAutomationProbe.runIfRequested(sessionStore: sessionStore)
            #endif
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

#if DEBUG
/// 仅供 Debug 构建的联调自动化探针：由环境变量驱动登录与登出，让 B1 L2 的登出链路复测可以在模拟器中无人工点击地重复执行。
///
/// 用法（模拟器启动参数）：
/// - `FOODMAP_AUTOMATION=login-then-logout` 配合 `FOODMAP_ACCOUNT`、`FOODMAP_PASSWORD`：登录后立刻退出登录。
/// - `FOODMAP_AUTOMATION=logout`：对已有本地会话直接退出登录。
/// - `FOODMAP_BASE_URL` 可选，缺省使用 APIClient 的默认网关地址。
///
/// 探针会把每一步写进 App 沙盒 `Documents/foodmap-automation.log`，便于在没有控制台的情况下取证；
/// Release 包不编译本类型，日志里不出现任何 Token 明文。
@MainActor
enum LaunchAutomationProbe {
    /// 按环境变量执行一次自动化动作；没有 `FOODMAP_AUTOMATION` 时也会记一行启动事实，便于确认探针是否被编译进来。
    static func runIfRequested(sessionStore: AuthSessionStore) async {
        let environment = ProcessInfo.processInfo.environment
        let action = environment["FOODMAP_AUTOMATION"]
        append("app.start automation=\(action ?? "none") foodmapKeys=\(environment.keys.filter { $0.hasPrefix("FOODMAP_") }.sorted())")

        guard let action else { return }
        let baseURL = environment["FOODMAP_BASE_URL"] ?? APIClient.preferredBaseURLString()

        switch action {
        case "logout":
            append("logout.begin hasSession=\(sessionStore.session != nil)")
            await sessionStore.signOut(baseURL: baseURL)
            append("logout.end sessionNil=\(sessionStore.session == nil) message=\(sessionStore.restoreErrorMessage ?? "none")")
        case "login-then-logout":
            do {
                try await sessionStore.login(
                    identifier: environment["FOODMAP_ACCOUNT"] ?? "",
                    password: environment["FOODMAP_PASSWORD"] ?? "",
                    baseURL: baseURL
                )
                append("login.ok userId=\(sessionStore.session?.userId ?? -1)")
                append("logout.begin hasSession=\(sessionStore.session != nil)")
                await sessionStore.signOut(baseURL: baseURL)
                append("logout.end sessionNil=\(sessionStore.session == nil) message=\(sessionStore.restoreErrorMessage ?? "none")")
            } catch {
                append("login.failed error=\(error)")
            }
        default:
            append("unknown.action=\(action)")
        }
    }

    /// 追加一行探针日志到 App 沙盒 Documents 目录，供复测取证。
    private static func append(_ line: String) {
        guard let directory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else { return }
        let file = directory.appendingPathComponent("foodmap-automation.log")
        let stamp = ISO8601DateFormatter().string(from: Date())
        let text = "\(stamp) \(line)\n"
        if let handle = try? FileHandle(forWritingTo: file) {
            handle.seekToEndOfFile()
            handle.write(Data(text.utf8))
            try? handle.close()
        } else {
            try? text.write(to: file, atomically: true, encoding: .utf8)
        }
    }
}
#endif
