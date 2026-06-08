import SwiftUI

/// 地图首页占位页，用于验证登录成功后的路由，后续替换为高德地图实现。
struct MapHomePlaceholderView: View {
    let session: AuthSession
    let onSignOut: () -> Void

    var body: some View {
        NavigationStack {
            ZStack {
                mapSurface
                VStack {
                    Spacer()
                    statusPanel
                }
                .padding(20)
            }
            .navigationTitle("FoodMap")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: onSignOut) {
                        Image(systemName: "rectangle.portrait.and.arrow.right")
                    }
                    .accessibilityLabel("退出登录")
                }
            }
        }
    }

    private var mapSurface: some View {
        GeometryReader { proxy in
            ZStack {
                LinearGradient(
                    colors: [Color.green.opacity(0.18), Color.blue.opacity(0.14)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                ForEach(0..<8, id: \.self) { index in
                    Path { path in
                        let y = proxy.size.height * CGFloat(index + 1) / 9
                        path.move(to: CGPoint(x: 0, y: y))
                        path.addCurve(
                            to: CGPoint(x: proxy.size.width, y: y + CGFloat(index % 2 == 0 ? 28 : -28)),
                            control1: CGPoint(x: proxy.size.width * 0.3, y: y - 36),
                            control2: CGPoint(x: proxy.size.width * 0.7, y: y + 36)
                        )
                    }
                    .stroke(Color.white.opacity(0.75), lineWidth: 8)
                }
                pin(x: 0.28, y: 0.36, color: .red, size: proxy.size)
                pin(x: 0.62, y: 0.48, color: .orange, size: proxy.size)
                pin(x: 0.44, y: 0.68, color: .pink, size: proxy.size)
            }
            .ignoresSafeArea()
        }
    }

    private func pin(x: CGFloat, y: CGFloat, color: Color, size: CGSize) -> some View {
        Image(systemName: "fork.knife.circle.fill")
            .font(.system(size: 38))
            .foregroundStyle(color)
            .shadow(radius: 8)
            .position(x: size.width * x, y: size.height * y)
    }

    private var statusPanel: some View {
        VStack(alignment: .leading, spacing: 10) {
            Label("已登录", systemImage: "checkmark.seal.fill")
                .font(.headline)
                .foregroundStyle(.green)
            Text("userId: \(session.userId)")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Text("accountId: \(session.accountId)")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
    }
}

#Preview {
    MapHomePlaceholderView(
        session: AuthSession(
            accountId: 100001,
            userId: 200001,
            accessTokenExpiresTime: "",
            refreshTokenExpiresTime: ""
        ),
        onSignOut: {}
    )
}
