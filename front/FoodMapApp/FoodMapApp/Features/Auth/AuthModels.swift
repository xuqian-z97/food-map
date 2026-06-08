import Foundation

/// 登录请求模型，字段名必须与后端认证接口保持一致。
struct LoginRequest: Encodable {
    /// 登录标识，允许账号名、手机号或邮箱。
    let loginIdentifier: String
    /// 用户输入的明文密码，仅用于请求体发送，不能持久化或打印日志。
    let password: String
}

/// 登录响应模型，包含认证会话和 Token 过期时间。
struct LoginResponse: Decodable {
    /// 认证服务账号业务 ID。
    let accountId: Int64
    /// 用户服务用户业务 ID。
    let userId: Int64
    /// 短期访问令牌。
    let accessToken: String
    /// 长期刷新令牌。
    let refreshToken: String
    /// Access Token 过期时间。
    let accessTokenExpiresTime: String
    /// Refresh Token 过期时间。
    let refreshTokenExpiresTime: String
}

/// 注册请求模型，当前用于本地联调认证和用户服务基础能力。
struct RegisterRequest: Encodable {
    /// 用户账号名，后端要求唯一。
    let accountName: String
    /// 可选手机号，MVP 阶段用于登录标识。
    let phone: String?
    /// 可选邮箱，MVP 阶段用于登录标识。
    let email: String?
    /// 用户明文密码，仅用于本次注册请求。
    let password: String
    /// 用户昵称，会同步给用户服务作为资料快照。
    let nickname: String
    /// 注册渠道，iOS 客户端固定传 IOS。
    let registeredChannel: String
}

/// 注册响应模型，返回账号和用户业务 ID，便于联调排查。
struct RegisterResponse: Decodable {
    /// 认证服务账号业务 ID。
    let accountId: Int64
    /// 用户服务用户业务 ID。
    let userId: Int64
    /// 账号状态，例如 NORMAL。
    let accountStatus: String
}
