import Foundation

/// FoodMap 前端请求超时规则，便于地图弱网降级和提交类接口分开处理。
enum APIRequestTimeout {
    /// 地图页门店查询，超时后优先降级展示缓存点位。
    static let mapQuery: TimeInterval = 6
    /// 普通查询。
    static let query: TimeInterval = 8
    /// 登录、注册、创建推荐等提交类接口。
    static let submit: TimeInterval = 15
}
