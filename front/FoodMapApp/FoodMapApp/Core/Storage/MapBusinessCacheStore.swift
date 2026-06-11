import Foundation

/// 地图弱网降级缓存，只保存低风险摘要，不保存社交推荐详情和评论正文。
final class MapBusinessCacheStore {
    private enum Key {
        static func storeSummaries(accountId: Int64, cityCode: String) -> String {
            "foodmap.cache.storeSummaries.\(accountId).\(cityCode)"
        }
    }

    private let defaults: UserDefaults
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    /// 创建地图业务缓存存储。
    /// - Parameter defaults: 用于保存弱网降级摘要的 UserDefaults。
    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    /// 读取指定账号和城市下仍未过期的门店摘要缓存。
    /// - Parameters:
    ///   - accountId: 当前账号 ID，用于隔离不同用户缓存。
    ///   - cityCode: 当前城市编码。
    /// - Returns: 可展示的门店摘要缓存。
    func loadStoreSummaries(accountId: Int64, cityCode: String) -> [CachedStoreSummary] {
        let key = Key.storeSummaries(accountId: accountId, cityCode: cityCode)
        guard
            let data = defaults.data(forKey: key),
            let summaries = try? decoder.decode([CachedStoreSummary].self, from: data)
        else {
            return seedSummaries(for: cityCode)
        }
        return summaries.filter { !$0.isExpired }
    }

    /// 保存指定账号和城市的门店摘要缓存。
    /// - Parameters:
    ///   - summaries: 可缓存的低风险门店摘要。
    ///   - accountId: 当前账号 ID。
    ///   - cityCode: 当前城市编码。
    func saveStoreSummaries(_ summaries: [CachedStoreSummary], accountId: Int64, cityCode: String) {
        let key = Key.storeSummaries(accountId: accountId, cityCode: cityCode)
        guard let data = try? encoder.encode(summaries) else {
            return
        }
        defaults.set(data, forKey: key)
    }

    /// 清理指定账号在当前设备上的地图业务缓存。
    /// - Parameter accountId: 当前账号 ID。
    func clearAccountCache(accountId: Int64) {
        let prefix = "foodmap.cache.storeSummaries.\(accountId)."
        for key in defaults.dictionaryRepresentation().keys where key.hasPrefix(prefix) {
            defaults.removeObject(forKey: key)
        }
    }

    private func seedSummaries(for cityCode: String) -> [CachedStoreSummary] {
        let now = Date()
        let expiresAt = now.addingTimeInterval(24 * 60 * 60)
        return [
            CachedStoreSummary(
                storeId: 9001,
                storeName: cityCode == "330100" ? "湖滨小馆" : "离线小馆",
                area: cityCode == "330100" ? "湖滨银泰附近" : "上次浏览商圈",
                dishName: "缓存推荐菜",
                visibleRecommendationCount: 1,
                scopeRawValue: MapScope.mine.rawValue,
                positionX: 0.38,
                positionY: 0.52,
                cachedAt: now,
                expiresAt: expiresAt
            ),
            CachedStoreSummary(
                storeId: 9002,
                storeName: "公开热汤铺",
                area: "上次公开地图范围",
                dishName: "番茄浓汤",
                visibleRecommendationCount: 3,
                scopeRawValue: MapScope.publicScope.rawValue,
                positionX: 0.58,
                positionY: 0.34,
                cachedAt: now,
                expiresAt: expiresAt
            )
        ]
    }
}
