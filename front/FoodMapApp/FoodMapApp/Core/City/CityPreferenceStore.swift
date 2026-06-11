import Foundation

/// 城市和离线地图下载偏好的轻量本地存储。
final class CityPreferenceStore {
    private enum Key {
        static let selectedCity = "foodmap.city.selected"
        static let allowsCellularOfflineMapDownload = "foodmap.offlineMap.allowsCellularDownload"
    }

    private let defaults: UserDefaults
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    /// 创建城市偏好存储。
    /// - Parameter defaults: 用于持久化轻量偏好的 UserDefaults。
    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    /// 读取当前选择城市；没有历史选择时返回默认城市。
    /// - Returns: 当前选择城市。
    func loadSelectedCity() -> FoodMapCity {
        guard
            let data = defaults.data(forKey: Key.selectedCity),
            let city = try? decoder.decode(FoodMapCity.self, from: data)
        else {
            return FoodMapCity.defaultCity
        }
        return city
    }

    /// 保存当前选择城市。
    /// - Parameter city: 用户选择或定位确认后的城市。
    func saveSelectedCity(_ city: FoodMapCity) {
        guard let data = try? encoder.encode(city) else {
            return
        }
        defaults.set(data, forKey: Key.selectedCity)
    }

    /// 读取蜂窝网络离线地图下载开关，默认关闭。
    /// - Returns: 是否允许使用蜂窝网络下载离线地图。
    func allowsCellularOfflineMapDownload() -> Bool {
        defaults.bool(forKey: Key.allowsCellularOfflineMapDownload)
    }

    /// 保存蜂窝网络离线地图下载开关。
    /// - Parameter isAllowed: 是否允许使用蜂窝网络下载。
    func setAllowsCellularOfflineMapDownload(_ isAllowed: Bool) {
        defaults.set(isAllowed, forKey: Key.allowsCellularOfflineMapDownload)
    }
}
