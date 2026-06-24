import Foundation

/// 网络层统一错误，负责将技术错误转换为页面可以直接展示的中文提示。
enum NetworkError: LocalizedError {
    /// 服务地址不是合法 URL。
    case invalidBaseURL
    /// 后端响应不是 HTTP 响应。
    case invalidResponse
    /// HTTP 状态码不是 2xx，且响应体无法按后端统一错误结构解析。
    case requestFailed(statusCode: Int)
    /// 后端统一业务错误，保留 status/code/message 便于联调排查。
    case server(status: Int, code: String, message: String)
    /// 后端业务响应成功但 data 为空。
    case emptyData
    /// URLSession 传输层错误，例如断网、DNS 失败、连接失败或超时。
    case transport(URLError)
    /// 后端响应体无法按约定解码。
    case decodingFailed

    var errorDescription: String? {
        switch self {
        case .invalidBaseURL:
            return "服务地址无效，请检查 Gateway 地址"
        case .invalidResponse:
            return "服务响应无效，请稍后重试"
        case .requestFailed(let statusCode):
            return Self.defaultMessage(for: statusCode)
        case .server(let status, _, let message):
            let trimmed = message.trimmingCharacters(in: .whitespacesAndNewlines)
            return trimmed.isEmpty ? Self.defaultMessage(for: status) : trimmed
        case .emptyData:
            return "服务响应缺少必要数据"
        case .transport(let error):
            return Self.transportMessage(for: error)
        case .decodingFailed:
            return "服务响应格式不符合约定，请联系开发者排查"
        }
    }

    /// 后端或 HTTP 层返回的状态码，供会话失效和联调日志分类使用。
    var statusCode: Int? {
        switch self {
        case .requestFailed(let statusCode):
            return statusCode
        case .server(let status, _, _):
            return status
        case .invalidBaseURL, .invalidResponse, .emptyData, .transport, .decodingFailed:
            return nil
        }
    }

    /// 后端稳定业务码，供问题记录和排查使用。
    var businessCode: String? {
        switch self {
        case .server(_, let code, _):
            return code
        case .invalidBaseURL, .invalidResponse, .requestFailed, .emptyData, .transport, .decodingFailed:
            return nil
        }
    }

    /// 当前错误是否明确表示本地认证已失效，只有这类错误才允许清理 Keychain 中的 Token。
    var shouldClearStoredAuthentication: Bool {
        if statusCode == 401 || statusCode == 403 {
            return true
        }

        guard let businessCode else {
            return false
        }

        return [
            "ACCOUNT_DISABLED",
            "ACCOUNT_NOT_FOUND",
            "USER_DISABLED",
            "USER_NOT_FOUND"
        ].contains(businessCode)
    }

    /// 将常见 HTTP 状态转为用户能理解的中文提示。
    private static func defaultMessage(for statusCode: Int) -> String {
        switch statusCode {
        case 400:
            return "请求参数有误，请检查输入内容"
        case 401:
            return "登录已失效，请重新登录"
        case 403:
            return "当前账号无权访问该资源"
        case 404:
            return "请求的资源不存在"
        case 409:
            return "账号信息已存在，请更换账号名、手机号或邮箱"
        case 500...599:
            return "服务暂时不可用，请稍后重试"
        default:
            return "请求失败：\(statusCode)"
        }
    }

    /// 将 URLSession 错误转为不泄露内部地址的中文提示。
    private static func transportMessage(for error: URLError) -> String {
        switch error.code {
        case .notConnectedToInternet, .networkConnectionLost:
            return "网络不可用，请检查网络连接"
        case .cannotFindHost, .cannotConnectToHost, .dnsLookupFailed:
            return "无法连接后端服务，请确认 Gateway 已启动"
        case .timedOut:
            return "请求超时，请确认 Gateway 和后端服务状态"
        default:
            return "网络请求失败，请稍后重试"
        }
    }
}
