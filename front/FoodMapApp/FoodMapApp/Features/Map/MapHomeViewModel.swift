import Foundation

/// 地图首页状态模型，第一阶段使用本地样例数据承载 UI 验证，后续接入地图门店查询 API。
@MainActor
final class MapHomeViewModel: ObservableObject {
    @Published var selectedScope: MapScope = .mine
    @Published var contentState: MapHomeContentState = .loaded
    @Published var markers: [MapStoreMarker] = MapHomeViewModel.sampleMarkers
    @Published var selectedMarker: MapStoreMarker? = MapHomeViewModel.sampleMarkers.first
    @Published var keyword = ""

    /// 根据范围切换首页展示内容，当前不做本地权限推断，只切换 UI 样例状态。
    func selectScope(_ scope: MapScope) {
        selectedScope = scope

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
        selectScope(selectedScope)
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
