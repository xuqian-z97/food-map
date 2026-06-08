import Foundation

/// 当前 App 认证会话快照，保存 UI 路由所需的最小用户身份信息。
struct AuthSession: Equatable {
    /// 认证服务账号业务 ID。
    let accountId: Int64
    /// 用户服务用户业务 ID。
    let userId: Int64
    /// Access Token 过期时间字符串，后续可替换为统一日期解析。
    let accessTokenExpiresTime: String
    /// Refresh Token 过期时间字符串，后续刷新 Token 时使用。
    let refreshTokenExpiresTime: String
}

/// 认证会话状态容器，负责登录、注册、退出登录和 Token 持久化编排。
@MainActor
final class AuthSessionStore: ObservableObject {
    @Published private(set) var session: AuthSession?

    private let tokenStore: AuthTokenStore

    /// 创建认证状态容器；如果本地已有 Token，则先恢复为已登录占位态，后续再补充用户资料拉取。
    init(tokenStore: AuthTokenStore = KeychainTokenStore()) {
        self.tokenStore = tokenStore
        if tokenStore.load() != nil {
            session = AuthSession(
                accountId: 0,
                userId: 0,
                accessTokenExpiresTime: "",
                refreshTokenExpiresTime: ""
            )
        }
    }

    /// 使用账号名、手机号或邮箱登录，成功后将 Token 写入本地安全存储。
    func login(identifier: String, password: String, baseURL: String) async throws {
        let client = try APIClient(baseURLString: baseURL)
        let response = try await client.login(identifier: identifier, password: password)
        try persist(response)
    }

    /// 调用后端注册接口；注册成功不自动登录，避免隐式改变当前会话状态。
    func register(request: RegisterRequest, baseURL: String) async throws -> RegisterResponse {
        let client = try APIClient(baseURLString: baseURL)
        return try await client.register(request)
    }

    /// 清理本地 Token 并回到未登录状态。
    func signOut() {
        tokenStore.clear()
        session = nil
    }

    /// 将登录响应转换为本地会话，并只持久化请求认证所需的 Token。
    private func persist(_ response: LoginResponse) throws {
        try tokenStore.save(AuthTokens(
            accessToken: response.accessToken,
            refreshToken: response.refreshToken
        ))
        session = AuthSession(
            accountId: response.accountId,
            userId: response.userId,
            accessTokenExpiresTime: response.accessTokenExpiresTime,
            refreshTokenExpiresTime: response.refreshTokenExpiresTime
        )
    }
}
