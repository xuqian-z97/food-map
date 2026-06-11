import Foundation

/// 地图首页状态模型，第一阶段使用本地样例数据承载 UI 验证，后续接入地图门店查询 API。
@MainActor
final class MapHomeViewModel: ObservableObject {
    @Published var selectedScope: MapScope = .mine
    @Published var contentState: MapHomeContentState = .loaded
    @Published var markers: [MapStoreMarker] = MapHomeViewModel.sampleMarkers
    @Published var selectedMarker: MapStoreMarker? = MapHomeViewModel.sampleMarkers.first
    @Published var selectedCity: FoodMapCity
    @Published var offlineMapRecords: [OfflineMapCityRecord]
    @Published var allowsCellularOfflineMapDownload: Bool
    @Published var isCityPickerPresented = false
    @Published var selectedSummaryMarker: MapStoreMarker?
    @Published var keyword = ""

    private let session: AuthSession
    private let cityPreferenceStore: CityPreferenceStore
    private let offlineMapStateStore: OfflineMapStateStore
    private let cacheStore: MapBusinessCacheStore

    /// 创建地图首页 ViewModel。
    /// - Parameters:
    ///   - session: 当前认证会话，用于隔离业务缓存。
    ///   - cityPreferenceStore: 城市偏好存储。
    ///   - offlineMapStateStore: 离线地图元数据存储。
    ///   - cacheStore: 地图业务缓存存储。
    init(
        session: AuthSession,
        cityPreferenceStore: CityPreferenceStore = CityPreferenceStore(),
        offlineMapStateStore: OfflineMapStateStore = OfflineMapStateStore(),
        cacheStore: MapBusinessCacheStore = MapBusinessCacheStore()
    ) {
        self.session = session
        self.cityPreferenceStore = cityPreferenceStore
        self.offlineMapStateStore = offlineMapStateStore
        self.cacheStore = cacheStore
        self.selectedCity = cityPreferenceStore.loadSelectedCity()
        self.offlineMapRecords = offlineMapStateStore.loadRecords()
        self.allowsCellularOfflineMapDownload = cityPreferenceStore.allowsCellularOfflineMapDownload()
        ensureOfflineMapPrepared()
    }

    /// 根据范围切换首页展示内容，当前不做本地权限推断，只切换 UI 样例状态。
    func selectScope(_ scope: MapScope) {
        selectedScope = scope

        if contentState == .offlineCache || markers.contains(where: \.isOfflineCache) {
            loadCachedStores()
            return
        }

        switch scope {
        case .mine:
            markers = Self.sampleMarkers.filter { $0.scope == .mine }
            contentState = .loaded
        case .friends:
            markers = Self.sampleMarkers.filter { $0.scope == .friends }
            contentState = .loaded
        case .couple:
            markers = []
            contentState = .empty
        case .publicScope:
            markers = Self.sampleMarkers.filter { $0.scope == .publicScope }
            contentState = .loaded
        }

        selectedMarker = markers.first
    }

    /// 重新加载当前地图视野内的数据，后续替换为 API 调用。
    func reloadVisibleStores() {
        loadCachedStores()
    }

    /// 切换当前城市并触发离线底图准备。
    /// - Parameter city: 用户选择的城市。
    func selectCity(_ city: FoodMapCity) {
        selectedCity = city
        cityPreferenceStore.saveSelectedCity(city)
        ensureOfflineMapPrepared()
        selectScope(selectedScope)
    }

    /// 设置蜂窝网络离线地图下载偏好。
    /// - Parameter isAllowed: 是否允许蜂窝网络下载。
    func setAllowsCellularOfflineMapDownload(_ isAllowed: Bool) {
        allowsCellularOfflineMapDownload = isAllowed
        cityPreferenceStore.setAllowsCellularOfflineMapDownload(isAllowed)
    }

    /// 打开选中门店摘要。
    /// - Parameter marker: 地图点位摘要。
    func openSummary(for marker: MapStoreMarker) {
        selectedSummaryMarker = marker
    }

    /// 当前城市离线地图状态。
    var currentOfflineMapRecord: OfflineMapCityRecord? {
        offlineMapRecords.first { $0.city.cityCode == selectedCity.cityCode }
    }

    /// 当前城市离线地图的简短状态文案。
    var offlineMapStatusText: String {
        guard let record = currentOfflineMapRecord else {
            return "离线底图待下载"
        }
        return "\(record.city.cityName)离线底图\(record.status.title)"
    }

    private func ensureOfflineMapPrepared() {
        offlineMapRecords = offlineMapStateStore.upsert(city: selectedCity, status: .downloaded)
    }

    private func loadCachedStores() {
        let cachedSummaries = cacheStore.loadStoreSummaries(
            accountId: session.accountId,
            cityCode: selectedCity.cityCode
        )

        markers = cachedSummaries.compactMap { summary in
            guard let scope = MapScope(rawValue: summary.scopeRawValue) else {
                return nil
            }
            return MapStoreMarker(
                id: summary.storeId,
                name: summary.storeName,
                dishName: summary.dishName,
                area: summary.area,
                visibleRecommendationCount: summary.visibleRecommendationCount,
                scope: scope,
                position: CGPoint(x: summary.positionX, y: summary.positionY),
                isOfflineCache: true
            )
        }
        .filter { selectedScope == .mine ? $0.scope == .mine : $0.scope == selectedScope }

        contentState = markers.isEmpty ? .networkUnavailable : .offlineCache
        selectedMarker = markers.first
    }

    private static let sampleMarkers: [MapStoreMarker] = [
        MapStoreMarker(
            id: 1001,
            name: "巷口小面",
            dishName: "番茄牛腩面",
            area: "人民公园附近",
            visibleRecommendationCount: 2,
            scope: .mine,
            position: CGPoint(x: 0.29, y: 0.36)
        ),
        MapStoreMarker(
            id: 1002,
            name: "禾木咖啡",
            dishName: "海盐拿铁",
            area: "老街转角",
            visibleRecommendationCount: 1,
            scope: .friends,
            position: CGPoint(x: 0.64, y: 0.43)
        ),
        MapStoreMarker(
            id: 1003,
            name: "晚风烧鸟",
            dishName: "鸡肉葱串",
            area: "湖边商业街",
            visibleRecommendationCount: 4,
            scope: .publicScope,
            position: CGPoint(x: 0.48, y: 0.68)
        ),
        MapStoreMarker(
            id: 1004,
            name: "南山甜品铺",
            dishName: "桂花酒酿圆子",
            area: "南山路",
            visibleRecommendationCount: 1,
            scope: .mine,
            position: CGPoint(x: 0.76, y: 0.58)
        )
    ]
}
