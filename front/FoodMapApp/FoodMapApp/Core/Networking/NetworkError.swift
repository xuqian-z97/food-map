import Foundation

/// 网络层统一错误，负责将技术错误转换为页面可以直接展示的中文提示。
enum NetworkError: LocalizedError {
    /// 服务地址不是合法 URL。
    case invalidBaseURL
    /// 后端响应不是 HTTP 响应。
    case invalidResponse
    /// HTTP 状态码不是 2xx。
    case requestFailed(statusCode: Int)
    /// 后端业务响应 success=false。
    case server(code: String, message: String)
    /// 后端业务响应成功但 data 为空。
    case emptyData

    var errorDescription: String? {
        switch self {
        case .invalidBaseURL:
            return "服务地址无效"
        case .invalidResponse:
            return "服务响应无效"
        case .requestFailed(let statusCode):
            return "请求失败：\(statusCode)"
        case .server(_, let message):
            return message
        case .emptyData:
            return "响应数据为空"
        }
    }
}
