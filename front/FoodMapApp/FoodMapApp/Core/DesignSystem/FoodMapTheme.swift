import SwiftUI

/// FoodMap 视觉主题，集中承载第一阶段 UI 的温馨色彩、圆角和基础组件样式。
enum FoodMapTheme {
    /// 页面主背景，接近米白，减少工具感。
    static let ricePaper = Color(red: 0.99, green: 0.96, blue: 0.90)
    /// 卡片背景，保持内容区清爽。
    static let card = Color(red: 1.00, green: 0.99, blue: 0.96)
    /// 品牌主色，用于主按钮和关键点位。
    static let persimmon = Color(red: 0.91, green: 0.37, blue: 0.22)
    /// 温暖强调色，用于提示和选中态背景。
    static let simmer = Color(red: 0.98, green: 0.70, blue: 0.35)
    /// 可信任的绿色，用于地图和低优先级正向信息。
    static let teaGreen = Color(red: 0.38, green: 0.57, blue: 0.43)
    /// 深色文字，避免纯黑带来的生硬感。
    static let ink = Color(red: 0.19, green: 0.16, blue: 0.13)
    /// 次级文字。
    static let mutedInk = Color(red: 0.48, green: 0.42, blue: 0.35)
    /// 分割线和轻量边框。
    static let hairline = Color(red: 0.87, green: 0.79, blue: 0.68)

    /// 页面中重复卡片的统一圆角，保持克制。
    static let cardCornerRadius: CGFloat = 8
}

/// FoodMap 主按钮样式，适合提交、添加推荐等核心动作。
struct FoodMapPrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundStyle(.white)
            .padding(.horizontal, 18)
            .frame(minHeight: 48)
            .background(FoodMapTheme.persimmon.opacity(configuration.isPressed ? 0.82 : 1))
            .clipShape(RoundedRectangle(cornerRadius: FoodMapTheme.cardCornerRadius, style: .continuous))
            .shadow(color: FoodMapTheme.persimmon.opacity(configuration.isPressed ? 0.10 : 0.22), radius: 12, x: 0, y: 6)
    }
}

/// FoodMap 图标按钮样式，用于筛选、定位、退出登录等工具动作。
struct FoodMapIconButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 17, weight: .semibold))
            .foregroundStyle(FoodMapTheme.ink)
            .frame(width: 44, height: 44)
            .background(FoodMapTheme.card.opacity(configuration.isPressed ? 0.76 : 0.96))
            .clipShape(RoundedRectangle(cornerRadius: FoodMapTheme.cardCornerRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: FoodMapTheme.cardCornerRadius, style: .continuous)
                    .stroke(FoodMapTheme.hairline.opacity(0.55), lineWidth: 1)
            )
    }
}

extension View {
    /// 首页浮层卡片样式。
    func foodMapCard(padding: CGFloat = 14) -> some View {
        self
            .padding(padding)
            .background(FoodMapTheme.card.opacity(0.96))
            .clipShape(RoundedRectangle(cornerRadius: FoodMapTheme.cardCornerRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: FoodMapTheme.cardCornerRadius, style: .continuous)
                    .stroke(FoodMapTheme.hairline.opacity(0.45), lineWidth: 1)
            )
            .shadow(color: FoodMapTheme.ink.opacity(0.08), radius: 16, x: 0, y: 8)
    }
}
