import Foundation

/// 后端统一响应信封，对应 Java 服务返回的 success/code/message/data 结构。
struct APIResponse<T: Decodable>: Decodable {
    let success: Bool
    let code: String
    let message: String
    let data: T?
}
