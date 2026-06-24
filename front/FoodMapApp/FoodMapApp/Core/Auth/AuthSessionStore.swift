import Foundation

/// 当前 App 认证会话快照，保存 UI 路由所需的最小用户身份信息。
struct AuthSession: Equatable {
    /// 认证服务账号业务 ID。
    let accountId: Int64
    /// 用户服务用户业务 ID。
    let userId: Int64
    /// 登录账号名，由后端当前用户接口确认。
    let accountName: String
    /// 当前展示昵称，由后端当前用户接口返回。
    let nickname: String
    /// 当前用户状态，例如 NORMAL。
    let userStatus: String
    /// Access Token 过期时间字符串，后续可替换为统一日期解析。
    let accessTokenExpiresTime: String
    /// Refresh Token 过期时间字符串，后续刷新 Token 时使用。
    let refreshTokenExpiresTime: String

    /// 创建认证会话；预览或占位代码可不传展示字段，运行时必须来自当前用户接口。
    init(
        accountId: Int64,
        userId: Int64,
        accessTokenExpiresTime: String,
        refreshTokenExpiresTime: String,
        accountName: String = "",
        nickname: String = "",
        userStatus: String = ""
    ) {
        self.accountId = accountId
        self.userId = userId
        self.accountName = accountName
        self.nickname = nickname
        self.userStatus = userStatus
        self.accessTokenExpiresTime = accessTokenExpiresTime
        self.refreshTokenExpiresTime = refreshTokenExpiresTime
    }
}

/// 认证会话状态容器，负责登录、注册、退出登录和 Token 持久化编排。
@MainActor
final class AuthSessionStore: ObservableObject {
    @Published private(set) var session: AuthSession?
    @Published private(set) var isRestoringSession: Bool
    @Published private(set) var restoreErrorMessage: String?

    private let tokenStore: AuthTokenStore
    private let urlSession: URLSession

    /// 创建认证状态容器；如果本地已有 Token，等待 AppRouter 异步校验当前用户后再建立 session。
    init(tokenStore: AuthTokenStore = KeychainTokenStore(), urlSession: URLSession = .shared) {
        self.tokenStore = tokenStore
        self.urlSession = urlSession
        self.isRestoringSession = tokenStore.load() != nil
    }

    /// 使用账号名、手机号或邮箱登录，成功后保存 Token 并通过当前用户接口建立真实会话。
    func login(identifier: String, password: String, baseURL: String) async throws {
        let client = try APIClient(baseURLString: baseURL, session: urlSession)
        let response = try await client.login(identifier: identifier, password: password)
        try await persistAndLoadCurrentUser(loginResponse: response, client: client)
    }

    /// 调用后端注册接口；注册成功不自动登录，避免隐式改变当前会话状态。
    func register(request: RegisterRequest, baseURL: String) async throws -> RegisterResponse {
        let client = try APIClient(baseURLString: baseURL, session: urlSession)
        return try await client.register(request)
    }

    /// App 启动时校验 Keychain 中的 Token；成功后恢复真实 session，失败时按错误类型清理或回到登录页。
    func restoreSessionIfNeeded(baseURL: String = APIClient.preferredBaseURLString()) async {
        guard let tokens = tokenStore.load() else {
            isRestoringSession = false
            restoreErrorMessage = nil
            session = nil
            return
        }

        isRestoringSession = true
        restoreErrorMessage = nil
        defer { isRestoringSession = false }

        do {
            let client = try APIClient(baseURLString: baseURL, session: urlSession)
            let currentUser = try await client.currentUser(accessToken: tokens.accessToken)
            session = makeSession(
                currentUser: currentUser,
                accessTokenExpiresTime: "",
                refreshTokenExpiresTime: ""
            )
        } catch {
            session = nil
            if (error as? NetworkError)?.shouldClearStoredAuthentication == true {
                tokenStore.clear()
            }
            restoreErrorMessage = error.localizedDescription
        }
    }

    /// 清理本地 Token 并回到未登录状态。
    func signOut() {
        tokenStore.clear()
        session = nil
        restoreErrorMessage = nil
        isRestoringSession = false
    }

    /// 保存登录 Token，随后拉取当前用户资料；仅当后端明确认证失效时清理本地 Token。
    private func persistAndLoadCurrentUser(loginResponse: LoginResponse, client: APIClient) async throws {
        let tokens = AuthTokens(
            accessToken: loginResponse.accessToken,
            refreshToken: loginResponse.refreshToken
        )
        try tokenStore.save(tokens)

        do {
            let currentUser = try await client.currentUser(accessToken: loginResponse.accessToken)
            session = makeSession(
                currentUser: currentUser,
                accessTokenExpiresTime: loginResponse.accessTokenExpiresTime,
                refreshTokenExpiresTime: loginResponse.refreshTokenExpiresTime
            )
            restoreErrorMessage = nil
        } catch {
            if (error as? NetworkError)?.shouldClearStoredAuthentication == true {
                tokenStore.clear()
            }
            session = nil
            throw error
        }
    }

    /// 将后端当前用户资料转换为运行时 session，禁止使用 0 作为已登录用户占位。
    private func makeSession(
        currentUser: CurrentUserResponse,
        accessTokenExpiresTime: String,
        refreshTokenExpiresTime: String
    ) -> AuthSession {
        AuthSession(
            accountId: currentUser.accountId,
            userId: currentUser.userId,
            accessTokenExpiresTime: accessTokenExpiresTime,
            refreshTokenExpiresTime: refreshTokenExpiresTime,
            accountName: currentUser.accountName ?? "",
            nickname: currentUser.nickname,
            userStatus: currentUser.userStatus
        )
    }
}
