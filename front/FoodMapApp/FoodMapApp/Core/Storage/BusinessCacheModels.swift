import Foundation

/// 弱网或离线时可展示的低风险门店摘要缓存。
struct CachedStoreSummary: Codable, Equatable, Identifiable {
    /// 门店业务 ID。
    let storeId: Int64
    /// 门店名称。
    let storeName: String
    /// 展示地址或商圈。
    let area: String
    /// 推荐菜名摘要，仅限我的推荐或 PUBLIC 摘要等低风险内容。
    let dishName: String
    /// 当前用户最近一次在线加载时可见的推荐数量。
    let visibleRecommendationCount: Int
    /// 数据范围。
    let scopeRawValue: String
    /// 地图占位相对 x 坐标。
    let positionX: Double
    /// 地图占位相对 y 坐标。
    let positionY: Double
    /// 缓存写入时间。
    let cachedAt: Date
    /// 缓存过期时间。
    let expiresAt: Date

    var id: Int64 { storeId }

    /// 缓存是否已经超过可展示时间。
    var isExpired: Bool {
        Date() > expiresAt
    }
}
