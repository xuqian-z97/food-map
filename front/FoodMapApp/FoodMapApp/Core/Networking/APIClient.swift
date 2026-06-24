import Foundation

/// FoodMap 后端 API 客户端，承担 Gateway 请求构建、认证头注入和统一响应解析。
struct APIClient {
    /// B1 iOS 本地联调默认入口，App 外部请求必须优先通过 Gateway。
    static let defaultBaseURLString = "http://127.0.0.1:18080"
    /// 本地保存服务地址的 Key，登录页和启动恢复共用同一配置。
    static let baseURLDefaultsKey = "foodmap.apiBaseURL"
    /// 早期认证服务直连地址；读取到该历史默认值时自动回到 Gateway。
    static let legacyDirectAuthBaseURLString = "http://127.0.0.1:8081"

    /// 返回当前推荐服务地址；历史直连默认值自动迁移为 Gateway。
    static func preferredBaseURLString(defaults: UserDefaults = .standard) -> String {
        guard let saved = defaults.string(forKey: baseURLDefaultsKey) else {
            return defaultBaseURLString
        }

        let trimmed = saved.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty || trimmed == legacyDirectAuthBaseURLString {
            return defaultBaseURLString
        }
        return trimmed
    }

    private let baseURL: URL
    private let session: URLSession
    private let decoder = JSONDecoder()
    private let encoder = JSONEncoder()

    /// 使用用户输入或默认配置中的服务地址创建客户端。
    init(baseURLString: String, session: URLSession = .shared) throws {
        let trimmed = baseURLString.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let url = URL(string: trimmed), url.scheme != nil, url.host != nil else {
            throw NetworkError.invalidBaseURL
        }
        self.baseURL = url
        self.session = session
    }

    /// 调用登录接口，登录标识可以是账号名、手机号或邮箱。
    func login(identifier: String, password: String) async throws -> LoginResponse {
        let request = LoginRequest(loginIdentifier: identifier, password: password)
        return try await post(path: "/api/auth/login", body: request)
    }

    /// 调用注册接口，注册成功后由调用方决定是否继续登录。
    func register(_ request: RegisterRequest) async throws -> RegisterResponse {
        try await post(path: "/api/auth/register", body: request)
    }

    /// 使用 Access Token 查询当前用户资料，必须通过 Gateway 注入可信身份头。
    func currentUser(accessToken: String) async throws -> CurrentUserResponse {
        try await authenticatedGet(path: "/api/users/me", accessToken: accessToken)
    }

    /// 发送需要 Bearer Token 的 GET 请求。
    func authenticatedGet<ResponseBody: Decodable>(
        path: String,
        accessToken: String
    ) async throws -> ResponseBody {
        try await request(
            path: path,
            method: "GET",
            timeout: APIRequestTimeout.query,
            accessToken: accessToken,
            body: Optional<EmptyRequestBody>.none
        )
    }

    private func post<RequestBody: Encodable, ResponseBody: Decodable>(
        path: String,
        body: RequestBody
    ) async throws -> ResponseBody {
        try await request(
            path: path,
            method: "POST",
            timeout: APIRequestTimeout.submit,
            accessToken: nil,
            body: body
        )
    }

    private func request<RequestBody: Encodable, ResponseBody: Decodable>(
        path: String,
        method: String,
        timeout: TimeInterval,
        accessToken: String?,
        body: RequestBody?
    ) async throws -> ResponseBody {
        let url = try makeURL(path: path)
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.timeoutInterval = timeout
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue(makeRequestID(), forHTTPHeaderField: "X-Request-Id")
        request.setValue(makeRequestID(), forHTTPHeaderField: "X-Trace-Id")

        if let accessToken {
            request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        }

        if let body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try encoder.encode(body)
        }

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch let error as URLError {
            throw NetworkError.transport(error)
        } catch {
            throw NetworkError.invalidResponse
        }

        guard let httpResponse = response as? HTTPURLResponse else {
            throw NetworkError.invalidResponse
        }

        guard (200..<300).contains(httpResponse.statusCode) else {
            throw parseErrorResponse(data: data, fallbackStatusCode: httpResponse.statusCode)
        }

        let envelope: APIResponse<ResponseBody>
        do {
            envelope = try decoder.decode(APIResponse<ResponseBody>.self, from: data)
        } catch {
            throw NetworkError.decodingFailed
        }

        guard envelope.success else {
            throw NetworkError.server(
                status: envelope.status,
                code: envelope.code,
                message: envelope.message
            )
        }

        guard let responseData = envelope.data else {
            throw NetworkError.emptyData
        }

        return responseData
    }

    /// 根据后端 API path 生成完整 URL，避免将 `/api/auth/login` 作为单个 path component 后被错误转义。
    private func makeURL(path: String) throws -> URL {
        let base = baseURL.absoluteString.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let normalizedPath = path.hasPrefix("/") ? path : "/" + path
        guard let url = URL(string: base + normalizedPath) else {
            throw NetworkError.invalidBaseURL
        }
        return url
    }

    /// 解析后端统一错误体；无法解析时回落到 HTTP 状态码分类。
    private func parseErrorResponse(data: Data, fallbackStatusCode: Int) -> NetworkError {
        guard !data.isEmpty,
              let envelope = try? decoder.decode(APIErrorResponse.self, from: data) else {
            return .requestFailed(statusCode: fallbackStatusCode)
        }

        return .server(
            status: envelope.status ?? fallbackStatusCode,
            code: envelope.code ?? "HTTP_\(fallbackStatusCode)",
            message: envelope.message ?? ""
        )
    }

    /// 生成本次请求的联调追踪 ID；不包含账号、手机号、邮箱或 Token。
    private func makeRequestID() -> String {
        "ios-\(UUID().uuidString.lowercased())"
    }
}

/// 无请求体占位，避免 authenticated GET 额外暴露重载复杂度。
private struct EmptyRequestBody: Encodable {}

/// 错误响应只关心统一信封字段，忽略 data 以兼容不同后端错误详情。
private struct APIErrorResponse: Decodable {
    let success: Bool?
    let status: Int?
    let code: String?
    let message: String?
}
