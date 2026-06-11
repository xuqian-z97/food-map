import Foundation

/// 高德离线底图在 FoodMap 内部呈现的下载状态。
enum OfflineMapDownloadStatus: String, Codable, Equatable {
    /// 尚未下载。
    case notDownloaded
    /// 下载中。
    case downloading
    /// 已下载。
    case downloaded
    /// 检测到新版本。
    case updateAvailable
    /// 下载或检查失败。
    case failed

    /// 面向用户的状态文案。
    var title: String {
        switch self {
        case .notDownloaded:
            return "待下载"
        case .downloading:
            return "下载中"
        case .downloaded:
            return "已下载"
        case .updateAvailable:
            return "可更新"
        case .failed:
            return "需重试"
        }
    }
}

/// 单个城市离线底图的本地状态记录。
struct OfflineMapCityRecord: Codable, Equatable, Identifiable {
    /// 城市信息。
    var city: FoodMapCity
    /// 下载状态。
    var status: OfflineMapDownloadStatus
    /// 下载进度，范围 0 到 1。
    var progress: Double
    /// 最近使用时间，用于最多 3 城市保留策略。
    var lastUsedAt: Date
    /// 最近版本检查时间。
    var lastCheckedAt: Date?
    /// 最近完成下载或更新的时间。
    var updatedAt: Date?
    /// 最近失败原因，不能包含 Token、URL 密钥或私密业务内容。
    var errorMessage: String?

    var id: String { city.cityCode }

    /// 当前记录是否已经具备可用离线底图。
    var isUsable: Bool {
        status == .downloaded || status == .updateAvailable
    }
}

/// 维护离线底图最多保留城市数量的纯策略。
struct OfflineMapRetentionPolicy {
    /// 最大城市离线包数量。
    let maximumCityCount: Int

    /// 根据最近使用时间清理超出数量的非当前城市记录。
    /// - Parameters:
    ///   - records: 当前本地记录。
    ///   - currentCity: 当前用户选择城市。
    /// - Returns: 应保留的记录。
    func retainedRecords(_ records: [OfflineMapCityRecord], currentCity: FoodMapCity) -> [OfflineMapCityRecord] {
        guard records.count > maximumCityCount else {
            return records
        }

        let currentRecords = records.filter { $0.city.cityCode == currentCity.cityCode }
        let otherRecords = records
            .filter { $0.city.cityCode != currentCity.cityCode }
            .sorted { $0.lastUsedAt > $1.lastUsedAt }

        return Array((currentRecords + otherRecords).prefix(maximumCityCount))
    }
}
