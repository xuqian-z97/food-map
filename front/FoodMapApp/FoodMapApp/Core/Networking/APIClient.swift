import Foundation

/// FoodMap 后端 API 客户端，当前承担认证接口调用和统一响应解析。
struct APIClient {
    private let baseURL: URL
    private let session: URLSession

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

    private func post<RequestBody: Encodable, ResponseBody: Decodable>(
        path: String,
        body: RequestBody
    ) async throws -> ResponseBody {
        let url = try makeURL(path: path)
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(body)

        let (data, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw NetworkError.invalidResponse
        }
        guard (200..<300).contains(httpResponse.statusCode) else {
            throw NetworkError.requestFailed(statusCode: httpResponse.statusCode)
        }

        let envelope = try JSONDecoder().decode(APIResponse<ResponseBody>.self, from: data)
        guard envelope.success else {
            throw NetworkError.server(code: envelope.code, message: envelope.message)
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
}
