import Foundation

/// 本地保存的认证 Token 对，仅用于客户端请求认证，不承载用户资料。
struct AuthTokens: Codable, Equatable {
    /// 后续访问业务 API 时使用的短期访问令牌。
    let accessToken: String
    /// 用于刷新访问令牌的长期令牌，必须避免出现在日志中。
    let refreshToken: String
}

/// Token 存储抽象，便于生产环境使用 Keychain，测试环境替换为内存实现。
protocol AuthTokenStore {
    /// 读取本地 Token；读取失败或不存在时返回 nil。
    func load() -> AuthTokens?
    /// 保存最新 Token，调用方需要处理 Keychain 或编码失败。
    func save(_ tokens: AuthTokens) throws
    /// 清理本地 Token，通常用于退出登录或认证失效。
    func clear()
}
