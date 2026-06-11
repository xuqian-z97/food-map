import Foundation

/// FoodMap 支持展示和下载离线底图的城市描述。
struct FoodMapCity: Codable, Equatable, Identifiable {
    /// 高德城市编码或 FoodMap 约定城市编码。
    let cityCode: String
    /// 城市名称。
    let cityName: String
    /// 省份或直辖市名称，用于城市选择列表展示。
    let provinceName: String

    var id: String { cityCode }

    /// MVP 阶段用于城市选择和离线地图状态验证的城市列表，后续可由后端或高德城市列表驱动。
    static let supportedCities: [FoodMapCity] = [
        FoodMapCity(cityCode: "310100", cityName: "上海市", provinceName: "上海市"),
        FoodMapCity(cityCode: "330100", cityName: "杭州市", provinceName: "浙江省"),
        FoodMapCity(cityCode: "110100", cityName: "北京市", provinceName: "北京市"),
        FoodMapCity(cityCode: "440100", cityName: "广州市", provinceName: "广东省"),
        FoodMapCity(cityCode: "440300", cityName: "深圳市", provinceName: "广东省")
    ]

    /// 默认城市用于定位被拒绝前的首屏兜底。
    static let defaultCity = FoodMapCity.supportedCities[0]
}
