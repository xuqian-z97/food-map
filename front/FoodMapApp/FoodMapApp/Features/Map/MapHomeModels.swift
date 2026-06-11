import Foundation
import SwiftUI

/// 地图首页的数据范围，对应后端地图查询的 scope 入参。
enum MapScope: String, CaseIterable, Identifiable {
    case mine = "MINE"
    case friends = "FRIENDS"
    case couple = "COUPLE"
    case publicScope = "PUBLIC"

    var id: String { rawValue }

    /// 面向用户的范围名称。
    var title: String {
        switch self {
        case .mine:
            return "我的"
        case .friends:
            return "好友"
        case .couple:
            return "情侣"
        case .publicScope:
            return "公开"
        }
    }

    /// 范围图标，用于降低文字按钮的拥挤感。
    var systemImage: String {
        switch self {
        case .mine:
            return "person.fill"
        case .friends:
            return "person.2.fill"
        case .couple:
            return "heart.fill"
        case .publicScope:
            return "sparkles"
        }
    }
}

/// 地图首页门店点位摘要，后续会由门店地图查询 API 返回。
struct MapStoreMarker: Identifiable, Equatable {
    /// 门店业务 ID。
    let id: Int64
    /// 门店名称。
    let name: String
    /// 推荐菜名摘要。
    let dishName: String
    /// 展示地址或商圈。
    let area: String
    /// 当前用户可见推荐数量。
    let visibleRecommendationCount: Int
    /// 可见范围。
    let scope: MapScope
    /// 地图占位图中的相对位置，接入高德后替换为经纬度渲染。
    let position: CGPoint
}

/// 地图首页可视状态，保证加载、空、错误和无权限状态可明确表达。
enum MapHomeContentState: Equatable {
    case loading
    case loaded
    case empty
    case noPermission
    case networkUnavailable
    case failed(String)
}
