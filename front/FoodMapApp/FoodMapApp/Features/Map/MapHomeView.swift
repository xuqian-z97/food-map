import SwiftUI

/// 登录后的地图首页壳，承载地图优先、温馨私密和轻量添加推荐的核心体验。
struct MapHomeView: View {
    let session: AuthSession
    let onSignOut: () -> Void

    @StateObject private var viewModel: MapHomeViewModel

    /// 创建地图首页。
    /// - Parameters:
    ///   - session: 当前认证会话。
    ///   - onSignOut: 退出登录动作。
    init(session: AuthSession, onSignOut: @escaping () -> Void) {
        self.session = session
        self.onSignOut = onSignOut
        _viewModel = StateObject(wrappedValue: MapHomeViewModel(session: session))
    }

    var body: some View {
        NavigationStack {
            ZStack {
                WarmMapSurface(
                    markers: viewModel.markers,
                    selectedMarker: viewModel.selectedMarker,
                    onSelect: { viewModel.selectedMarker = $0 }
                )

                VStack(spacing: 12) {
                    topBar
                    offlineMapBar
                    scopePicker
                    Spacer()
                    bottomContent
                }
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .padding(.bottom, 18)

                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        addRecommendationButton
                    }
                }
                .padding(.trailing, 16)
                .padding(.bottom, viewModel.selectedMarker == nil ? 28 : 156)
            }
            .background(FoodMapTheme.ricePaper)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar(.hidden, for: .navigationBar)
            .sheet(isPresented: $viewModel.isCityPickerPresented) {
                CityPickerView(
                    selectedCity: viewModel.selectedCity,
                    allowsCellularDownload: viewModel.allowsCellularOfflineMapDownload,
                    onToggleCellular: viewModel.setAllowsCellularOfflineMapDownload,
                    onSelectCity: { city in
                        viewModel.selectCity(city)
                        viewModel.isCityPickerPresented = false
                    }
                )
            }
            .sheet(item: $viewModel.selectedSummaryMarker) { marker in
                OfflineStoreSummaryView(marker: marker)
            }
        }
    }

    private var topBar: some View {
        HStack(spacing: 10) {
            VStack(alignment: .leading, spacing: 5) {
                Text("FoodMap")
                    .font(.system(size: 28, weight: .bold, design: .rounded))
                    .foregroundStyle(FoodMapTheme.ink)
                Text(session.userId == 0 ? "继续完善你的美食地图" : "今天也记下一家值得再来的店")
                    .font(.subheadline)
                    .foregroundStyle(FoodMapTheme.mutedInk)
                    .lineLimit(1)
                    .minimumScaleFactor(0.78)
            }

            Spacer()

            Button {
                viewModel.isCityPickerPresented = true
            } label: {
                Label(viewModel.selectedCity.cityName, systemImage: "mappin.and.ellipse")
                    .labelStyle(.iconOnly)
            }
            .buttonStyle(FoodMapIconButtonStyle())
            .accessibilityLabel("切换城市")

            Button(action: viewModel.reloadVisibleStores) {
                Image(systemName: "arrow.clockwise")
            }
            .buttonStyle(FoodMapIconButtonStyle())
            .accessibilityLabel("刷新当前地图范围")

            Button(action: onSignOut) {
                Image(systemName: "rectangle.portrait.and.arrow.right")
            }
            .buttonStyle(FoodMapIconButtonStyle())
            .accessibilityLabel("退出登录")
        }
        .foodMapCard(padding: 14)
    }

    private var offlineMapBar: some View {
        HStack(spacing: 10) {
            Image(systemName: "arrow.down.circle.fill")
                .font(.headline.weight(.semibold))
                .foregroundStyle(FoodMapTheme.teaGreen)
                .frame(width: 30, height: 30)
                .background(FoodMapTheme.teaGreen.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 7, style: .continuous))

            VStack(alignment: .leading, spacing: 3) {
                Text(viewModel.offlineMapStatusText)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(FoodMapTheme.ink)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
                Text(viewModel.allowsCellularOfflineMapDownload ? "Wi-Fi 自动更新，蜂窝网络已允许" : "Wi-Fi 自动静默更新，蜂窝网络默认关闭")
                    .font(.caption)
                    .foregroundStyle(FoodMapTheme.mutedInk)
                    .lineLimit(1)
                    .minimumScaleFactor(0.78)
            }

            Spacer(minLength: 0)
        }
        .foodMapCard(padding: 12)
    }

    private var scopePicker: some View {
        HStack(spacing: 6) {
            ForEach(MapScope.allCases) { scope in
                Button {
                    viewModel.selectScope(scope)
                } label: {
                    VStack(spacing: 5) {
                        Image(systemName: scope.systemImage)
                            .font(.system(size: 14, weight: .semibold))
                        Text(scope.title)
                            .font(.caption.weight(.semibold))
                    }
                    .foregroundStyle(viewModel.selectedScope == scope ? Color.white : FoodMapTheme.ink)
                    .frame(maxWidth: .infinity, minHeight: 54)
                    .background(viewModel.selectedScope == scope ? FoodMapTheme.persimmon : FoodMapTheme.card)
                    .clipShape(RoundedRectangle(cornerRadius: FoodMapTheme.cardCornerRadius, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: FoodMapTheme.cardCornerRadius, style: .continuous)
                            .stroke(FoodMapTheme.hairline.opacity(viewModel.selectedScope == scope ? 0 : 0.55), lineWidth: 1)
                    )
                }
                .buttonStyle(.plain)
            }
        }
        .accessibilityElement(children: .contain)
    }

    @ViewBuilder
    private var bottomContent: some View {
        switch viewModel.contentState {
        case .loading:
            MapStatusPanel(
                systemImage: "hourglass",
                title: "正在找附近的推荐",
                message: "稍等一下，地图马上热起来。"
            )
        case .loaded:
            if let selectedMarker = viewModel.selectedMarker {
                StoreMarkerPreviewView(marker: selectedMarker)
            }
        case .empty:
            MapStatusPanel(
                systemImage: "map",
                title: "\(viewModel.selectedScope.title)范围还没有推荐",
                message: "可以先从自己喜欢的一家店开始记录。"
            )
        case .noPermission:
            MapStatusPanel(
                systemImage: "lock.fill",
                title: "当前内容不可见",
                message: "FoodMap 只展示后端返回给你的授权内容。"
            )
        case .offlineCache:
            if let selectedMarker = viewModel.selectedMarker {
                StoreMarkerPreviewView(marker: selectedMarker, onOpenSummary: viewModel.openSummary)
            }
        case .networkUnavailable:
            MapStatusPanel(
                systemImage: "wifi.slash",
                title: "网络暂时不可用",
                message: "高德离线底图可继续查看，门店和推荐内容联网后更新。"
            )
        case .failed(let message):
            MapStatusPanel(
                systemImage: "exclamationmark.triangle.fill",
                title: "加载失败",
                message: message
            )
        }
    }

    private var addRecommendationButton: some View {
        Button {
            viewModel.contentState = .noPermission
        } label: {
            Label("添加推荐", systemImage: "plus")
                .labelStyle(.titleAndIcon)
                .padding(.horizontal, 2)
        }
        .buttonStyle(FoodMapPrimaryButtonStyle())
        .accessibilityLabel("添加推荐")
    }
}

private struct WarmMapSurface: View {
    let markers: [MapStoreMarker]
    let selectedMarker: MapStoreMarker?
    let onSelect: (MapStoreMarker) -> Void

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                LinearGradient(
                    colors: [
                        FoodMapTheme.ricePaper,
                        FoodMapTheme.simmer.opacity(0.28),
                        FoodMapTheme.teaGreen.opacity(0.28)
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )

                mapRoads(in: proxy.size)

                ForEach(markers) { marker in
                    Button {
                        onSelect(marker)
                    } label: {
                        MapMarkerPin(
                            marker: marker,
                            isSelected: marker == selectedMarker
                        )
                    }
                    .buttonStyle(.plain)
                    .position(
                        x: proxy.size.width * marker.position.x,
                        y: proxy.size.height * marker.position.y
                    )
                    .accessibilityLabel("\(marker.name)，推荐\(marker.dishName)")
                }
            }
            .ignoresSafeArea()
        }
    }

    private func mapRoads(in size: CGSize) -> some View {
        ZStack {
            ForEach(0..<7, id: \.self) { index in
                Path { path in
                    let y = size.height * CGFloat(index + 1) / 8
                    path.move(to: CGPoint(x: -20, y: y))
                    path.addCurve(
                        to: CGPoint(x: size.width + 20, y: y + CGFloat(index.isMultiple(of: 2) ? 22 : -26)),
                        control1: CGPoint(x: size.width * 0.24, y: y - 32),
                        control2: CGPoint(x: size.width * 0.72, y: y + 34)
                    )
                }
                .stroke(Color.white.opacity(0.72), lineWidth: 9)
                .shadow(color: FoodMapTheme.ink.opacity(0.04), radius: 1, x: 0, y: 1)
            }

            ForEach(0..<5, id: \.self) { index in
                Path { path in
                    let x = size.width * CGFloat(index + 1) / 6
                    path.move(to: CGPoint(x: x, y: -20))
                    path.addCurve(
                        to: CGPoint(x: x + CGFloat(index.isMultiple(of: 2) ? 28 : -24), y: size.height + 20),
                        control1: CGPoint(x: x + 36, y: size.height * 0.25),
                        control2: CGPoint(x: x - 34, y: size.height * 0.68)
                    )
                }
                .stroke(FoodMapTheme.card.opacity(0.58), lineWidth: 7)
            }
        }
    }
}

private struct MapMarkerPin: View {
    let marker: MapStoreMarker
    let isSelected: Bool

    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: "fork.knife.circle.fill")
                .font(.system(size: isSelected ? 38 : 32, weight: .bold))
                .foregroundStyle(markerColor)
                .background(
                    Circle()
                        .fill(Color.white)
                        .frame(width: isSelected ? 34 : 28, height: isSelected ? 34 : 28)
                )
                .shadow(color: FoodMapTheme.ink.opacity(0.18), radius: 8, x: 0, y: 5)

            if isSelected {
                Text(marker.dishName)
                    .font(.caption2.weight(.bold))
                    .foregroundStyle(FoodMapTheme.ink)
                    .padding(.horizontal, 7)
                    .padding(.vertical, 4)
                    .background(FoodMapTheme.card)
                    .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
            }
        }
    }

    private var markerColor: Color {
        switch marker.scope {
        case .mine:
            return FoodMapTheme.persimmon
        case .friends:
            return FoodMapTheme.teaGreen
        case .couple:
            return .pink
        case .publicScope:
            return FoodMapTheme.simmer
        }
    }
}

private struct StoreMarkerPreviewView: View {
    let marker: MapStoreMarker
    let onOpenSummary: (MapStoreMarker) -> Void

    init(marker: MapStoreMarker, onOpenSummary: @escaping (MapStoreMarker) -> Void = { _ in }) {
        self.marker = marker
        self.onOpenSummary = onOpenSummary
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: "fork.knife")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(FoodMapTheme.persimmon)
                    .frame(width: 40, height: 40)
                    .background(FoodMapTheme.persimmon.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: FoodMapTheme.cardCornerRadius, style: .continuous))

                VStack(alignment: .leading, spacing: 5) {
                    Text(marker.name)
                        .font(.headline)
                        .foregroundStyle(FoodMapTheme.ink)
                    Text(marker.area)
                        .font(.subheadline)
                        .foregroundStyle(FoodMapTheme.mutedInk)
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 6) {
                    if marker.isOfflineCache {
                        OfflineCacheBadge()
                    }
                    ScopeBadge(scope: marker.scope)
                }
            }

            HStack(spacing: 8) {
                Text(marker.dishName)
                    .font(.title3.weight(.bold))
                    .foregroundStyle(FoodMapTheme.ink)
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)

                Spacer()

                Label("\(marker.visibleRecommendationCount)", systemImage: "text.bubble.fill")
                    .font(.callout.weight(.semibold))
                    .foregroundStyle(FoodMapTheme.teaGreen)
            }

            Button {
                onOpenSummary(marker)
            } label: {
                Label("查看摘要", systemImage: "chevron.right.circle.fill")
                    .font(.callout.weight(.semibold))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(FoodMapSecondaryButtonStyle())
        }
        .foodMapCard(padding: 16)
    }
}

private struct OfflineCacheBadge: View {
    var body: some View {
        Text("离线缓存")
            .font(.caption2.weight(.bold))
            .foregroundStyle(FoodMapTheme.persimmon)
            .padding(.horizontal, 7)
            .padding(.vertical, 5)
            .background(FoodMapTheme.persimmon.opacity(0.12))
            .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
    }
}

private struct ScopeBadge: View {
    let scope: MapScope

    var body: some View {
        Label(scope.title, systemImage: scope.systemImage)
            .font(.caption.weight(.bold))
            .foregroundStyle(FoodMapTheme.ink)
            .padding(.horizontal, 8)
            .padding(.vertical, 6)
            .background(FoodMapTheme.simmer.opacity(scope == .publicScope ? 0.40 : 0.22))
            .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
    }
}

private struct MapStatusPanel: View {
    let systemImage: String
    let title: String
    let message: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: systemImage)
                .font(.title3.weight(.semibold))
                .foregroundStyle(FoodMapTheme.persimmon)
                .frame(width: 38, height: 38)
                .background(FoodMapTheme.persimmon.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: FoodMapTheme.cardCornerRadius, style: .continuous))

            VStack(alignment: .leading, spacing: 5) {
                Text(title)
                    .font(.headline)
                    .foregroundStyle(FoodMapTheme.ink)
                Text(message)
                    .font(.subheadline)
                    .foregroundStyle(FoodMapTheme.mutedInk)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
        }
        .foodMapCard(padding: 16)
    }
}

private struct CityPickerView: View {
    let selectedCity: FoodMapCity
    let allowsCellularDownload: Bool
    let onToggleCellular: (Bool) -> Void
    let onSelectCity: (FoodMapCity) -> Void

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Toggle(isOn: Binding(get: {
                        allowsCellularDownload
                    }, set: { newValue in
                        onToggleCellular(newValue)
                    })) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("允许蜂窝网络下载离线底图")
                                .font(.body.weight(.semibold))
                            Text("默认关闭；Wi-Fi 自动更新始终开启。")
                                .font(.caption)
                                .foregroundStyle(FoodMapTheme.mutedInk)
                        }
                    }
                }

                Section("城市") {
                    ForEach(FoodMapCity.supportedCities) { city in
                        Button {
                            onSelectCity(city)
                        } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(city.cityName)
                                        .font(.body.weight(.semibold))
                                        .foregroundStyle(FoodMapTheme.ink)
                                    Text(city.provinceName)
                                        .font(.caption)
                                        .foregroundStyle(FoodMapTheme.mutedInk)
                                }
                                Spacer()
                                if city == selectedCity {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundStyle(FoodMapTheme.teaGreen)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("切换城市")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

private struct OfflineStoreSummaryView: View {
    let marker: MapStoreMarker

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                if marker.isOfflineCache {
                    HStack(spacing: 8) {
                        Image(systemName: "wifi.slash")
                        Text("离线缓存")
                    }
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(FoodMapTheme.persimmon)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 8)
                    .background(FoodMapTheme.persimmon.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: FoodMapTheme.cardCornerRadius, style: .continuous))
                }

                VStack(alignment: .leading, spacing: 10) {
                    Text(marker.name)
                        .font(.title2.weight(.bold))
                        .foregroundStyle(FoodMapTheme.ink)
                    Text(marker.area)
                        .font(.subheadline)
                        .foregroundStyle(FoodMapTheme.mutedInk)
                    Divider()
                    Label(marker.dishName, systemImage: "fork.knife")
                        .font(.headline)
                        .foregroundStyle(FoodMapTheme.ink)
                    Label("\(marker.visibleRecommendationCount) 条可见摘要", systemImage: "text.bubble.fill")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(FoodMapTheme.teaGreen)
                }
                .foodMapCard(padding: 16)

                MapStatusPanel(
                    systemImage: "lock.fill",
                    title: "只读摘要",
                    message: "离线状态下不能新增、编辑、评论或上传图片；好友、情侣和指定用户内容联网后查看最新授权结果。"
                )

                Spacer()
            }
            .padding(16)
            .background(FoodMapTheme.ricePaper.ignoresSafeArea())
            .navigationTitle("门店摘要")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

#Preview {
    MapHomeView(
        session: AuthSession(
            accountId: 100001,
            userId: 200001,
            accessTokenExpiresTime: "",
            refreshTokenExpiresTime: ""
        ),
        onSignOut: {}
    )
}
