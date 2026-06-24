import Foundation

/// 后端统一响应信封，对应 Java 服务返回的 success/status/code/message/data 结构。
struct APIResponse<T: Decodable>: Decodable {
    /// 请求是否按业务语义成功。
    let success: Bool
    /// HTTP 数字状态码语义，便于前端错误分类和联调排查。
    let status: Int
    /// 稳定业务码，便于联调时定位后端错误分支。
    let code: String
    /// 后端返回的用户可读提示，错误时优先展示。
    let message: String
    /// 业务响应数据，错误响应通常为空。
    let data: T?
}
