import Foundation

/// 离线地图状态的轻量本地存储；真实底图文件由高德 SDK 管理。
final class OfflineMapStateStore {
    private enum Key {
        static let records = "foodmap.offlineMap.records"
    }

    private let defaults: UserDefaults
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()
    private let retentionPolicy = OfflineMapRetentionPolicy(maximumCityCount: 3)

    /// 创建离线地图状态存储。
    /// - Parameter defaults: 用于保存离线地图元数据的 UserDefaults。
    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    /// 读取全部城市离线地图记录。
    /// - Returns: 离线地图元数据记录。
    func loadRecords() -> [OfflineMapCityRecord] {
        guard
            let data = defaults.data(forKey: Key.records),
            let records = try? decoder.decode([OfflineMapCityRecord].self, from: data)
        else {
            return []
        }
        return records
    }

    /// 标记城市正在使用，并按最多 3 城市规则清理元数据。
    /// - Parameters:
    ///   - city: 当前城市。
    ///   - status: 目标状态。
    /// - Returns: 更新后的记录列表。
    @discardableResult
    func upsert(city: FoodMapCity, status: OfflineMapDownloadStatus) -> [OfflineMapCityRecord] {
        var records = loadRecords()
        let now = Date()

        if let index = records.firstIndex(where: { $0.city.cityCode == city.cityCode }) {
            records[index].status = status
            records[index].progress = status == .downloaded ? 1 : records[index].progress
            records[index].lastUsedAt = now
            records[index].lastCheckedAt = now
            records[index].updatedAt = status == .downloaded ? now : records[index].updatedAt
            records[index].errorMessage = nil
        } else {
            records.append(
                OfflineMapCityRecord(
                    city: city,
                    status: status,
                    progress: status == .downloaded ? 1 : 0,
                    lastUsedAt: now,
                    lastCheckedAt: now,
                    updatedAt: status == .downloaded ? now : nil,
                    errorMessage: nil
                )
            )
        }

        records = retentionPolicy.retainedRecords(records, currentCity: city)
        saveRecords(records)
        return records
    }

    /// 保存全部城市离线地图记录。
    /// - Parameter records: 要保存的记录。
    func saveRecords(_ records: [OfflineMapCityRecord]) {
        guard let data = try? encoder.encode(records) else {
            return
        }
        defaults.set(data, forKey: Key.records)
    }
}
