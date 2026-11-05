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
        case "error-states":
            await runErrorStateChecks(sessionStore: sessionStore, baseURL: baseURL, environment: environment)
        case "keep-token":
            await runTokenRetentionCheck(sessionStore: sessionStore, baseURL: baseURL, environment: environment)
        case "login-disabled":
            await runDisabledAccountCheck(sessionStore: sessionStore, baseURL: baseURL, environment: environment)
        case "session-expired":
            await runExpiredTokenCheck(sessionStore: sessionStore, baseURL: baseURL, environment: environment)
        case "register-checks":
            await runRegisterChecks(baseURL: baseURL, environment: environment)
        default:
            append("unknown.action=\(action)")
        }
    }

    /// 把网络错误压成一行断言文本：状态码、业务码、是否允许清理本地认证、可直接展示的提示。
    private static func describe(_ error: Error) -> String {
        guard let network = error as? NetworkError else { return "other(\(error))" }
        return "status=\(network.statusCode ?? -1) code=\(network.businessCode ?? "none") clearAuth=\(network.shouldClearStoredAuthentication) message=\(network.errorDescription ?? "")"
    }

    /// 错误态检查：错误密码应拿到后端业务提示，网关不可用应落到传输层且不允许清理本地认证。
    private static func runErrorStateChecks(sessionStore: AuthSessionStore, baseURL: String, environment: [String: String]) async {
        do {
            try await sessionStore.login(identifier: environment["FOODMAP_ACCOUNT"] ?? "", password: "WrongPassword123", baseURL: baseURL)
            append("bad-password.unexpectedSuccess")
        } catch {
            append("bad-password \(describe(error))")
        }

        do {
            try await sessionStore.login(identifier: environment["FOODMAP_ACCOUNT"] ?? "", password: "WrongPassword123", baseURL: "http://127.0.0.1:18099")
            append("gateway-down.unexpectedSuccess")
        } catch {
            append("gateway-down \(describe(error))")
        }
    }

    /// 注册链路检查：短密码要拿到后端 400 业务文案，userId-only 响应要能解码出 userId。
    private static func runRegisterChecks(baseURL: String, environment: [String: String]) async {
        guard let client = try? APIClient(baseURLString: baseURL) else {
            append("register.clientFailed baseURL=\(baseURL)")
            return
        }

        let shortRequest = RegisterRequest(
            accountName: "zxq_probe_short_\(Int(Date().timeIntervalSince1970))",
            phone: nil,
            email: nil,
            password: "abc",
            nickname: "probe",
            registeredChannel: "IOS"
        )
        do {
            let response = try await client.register(shortRequest)
            append("register.shortPassword.unexpectedSuccess userId=\(response.userId)")
        } catch {
            append("register.shortPassword \(describe(error))")
        }

        guard let accountName = environment["FOODMAP_NEW_ACCOUNT"] else {
            append("register.valid.missingAccount")
            return
        }
        let validRequest = RegisterRequest(
            accountName: accountName,
            phone: nil,
            email: nil,
            password: "Foodmap@123",
            nickname: "probe",
            registeredChannel: "IOS"
        )
        do {
            let response = try await client.register(validRequest)
            append("register.valid userId=\(response.userId) accountStatus=\(response.accountStatus)")
        } catch {
            append("register.valid \(describe(error))")
        }
    }

    /// 停用账号检查：账号被停用时登录应被拒绝，且不留下任何本地认证状态。
    private static func runDisabledAccountCheck(sessionStore: AuthSessionStore, baseURL: String, environment: [String: String]) async {
        do {
            try await sessionStore.login(
                identifier: environment["FOODMAP_ACCOUNT"] ?? "",
                password: environment["FOODMAP_PASSWORD"] ?? "",
                baseURL: baseURL
            )
            append("disabled.login.unexpectedSuccess tokenPresent=\(KeychainTokenStore().load() != nil)")
        } catch {
            append("disabled.login \(describe(error)) tokenPresent=\(KeychainTokenStore().load() != nil)")
        }
    }

    /// 过期 Token 检查：把外部构造的过期 Token 写进 Keychain，启动恢复应被拒绝并清理本地认证。
    private static func runExpiredTokenCheck(sessionStore: AuthSessionStore, baseURL: String, environment: [String: String]) async {
        guard let accessToken = environment["FOODMAP_SEED_ACCESS_TOKEN"],
              let refreshToken = environment["FOODMAP_SEED_REFRESH_TOKEN"] else {
            append("expired.missingSeed")
            return
        }
        let store = KeychainTokenStore()
        do {
            try store.save(AuthTokens(accessToken: accessToken, refreshToken: refreshToken))
            append("expired.seeded tokenPresent=\(store.load() != nil)")
        } catch {
            append("expired.seedFailed error=\(error)")
            return
        }
        await sessionStore.restoreSessionIfNeeded(baseURL: baseURL)
        append("expired.restore sessionNil=\(sessionStore.session == nil) tokenPresent=\(store.load() != nil) message=\(sessionStore.restoreErrorMessage ?? "none")")
    }

    /// 会话保活检查：临时性网络失败不得清空 Keychain，恢复后应能重新建立会话。
    private static func runTokenRetentionCheck(sessionStore: AuthSessionStore, baseURL: String, environment: [String: String]) async {
        let deadBaseURL = "http://127.0.0.1:18099"
        do {
            try await sessionStore.login(
                identifier: environment["FOODMAP_ACCOUNT"] ?? "",
                password: environment["FOODMAP_PASSWORD"] ?? "",
                baseURL: baseURL
            )
            append("retention.loginOk tokenPresent=\(KeychainTokenStore().load() != nil)")
        } catch {
            append("retention.loginFailed \(describe(error))")
            return
        }

        do {
            _ = try await sessionStore.refreshCurrentUserForDebug(baseURL: deadBaseURL)
            append("retention.tempCall.unexpectedSuccess")
        } catch {
            append("retention.tempCall \(describe(error))")
        }
        append("retention.afterTempCall tokenPresent=\(KeychainTokenStore().load() != nil)")

        await sessionStore.restoreSessionIfNeeded(baseURL: deadBaseURL)
        append("retention.afterRestoreDead tokenPresent=\(KeychainTokenStore().load() != nil) sessionNil=\(sessionStore.session == nil) message=\(sessionStore.restoreErrorMessage ?? "none")")

        await sessionStore.restoreSessionIfNeeded(baseURL: baseURL)
        append("retention.afterRecovery tokenPresent=\(KeychainTokenStore().load() != nil) userId=\(sessionStore.session?.userId ?? -1)")

        await sessionStore.signOut(baseURL: baseURL)
        append("retention.cleanup tokenPresent=\(KeychainTokenStore().load() != nil) sessionNil=\(sessionStore.session == nil)")
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
