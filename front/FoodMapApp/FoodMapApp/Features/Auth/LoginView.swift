import SwiftUI

/// 登录页面，提供账号名、手机号或邮箱登录入口，并允许本地联调时切换服务地址。
struct LoginView: View {
    @EnvironmentObject private var sessionStore: AuthSessionStore
    @StateObject private var viewModel = LoginViewModel()
    @State private var showsRegister = false

    var body: some View {
        NavigationStack {
            ZStack {
                FoodMapTheme.ricePaper
                    .ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        header
                        fields
                        actions
                        errorText
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 44)
                    .padding(.bottom, 24)
                    .frame(maxWidth: 520, alignment: .leading)
                }
            }
            .frame(maxWidth: .infinity)
            .sheet(isPresented: $showsRegister) {
                RegisterView(apiBaseURL: viewModel.apiBaseURL)
                    .environmentObject(sessionStore)
            }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 10) {
                Image(systemName: "fork.knife.circle.fill")
                    .font(.system(size: 34, weight: .bold))
                    .foregroundStyle(FoodMapTheme.persimmon)

                Text("FoodMap")
                    .font(.system(size: 42, weight: .bold, design: .rounded))
                    .foregroundStyle(FoodMapTheme.ink)
            }

            VStack(alignment: .leading, spacing: 6) {
                Text("登录")
                    .font(.title.weight(.bold))
                    .foregroundStyle(FoodMapTheme.ink)
                Text("回到你的私房美食地图")
                    .font(.subheadline)
                    .foregroundStyle(FoodMapTheme.mutedInk)
            }
        }
    }

    private var fields: some View {
        VStack(spacing: 14) {
            HStack(spacing: 10) {
                fieldIcon("person.fill")
                TextField("账号名 / 手机号 / 邮箱", text: $viewModel.loginIdentifier)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.emailAddress)
                    .textContentType(.username)
                    .submitLabel(.next)
            }
            .foodMapInputField()

            HStack(spacing: 10) {
                fieldIcon("lock.fill")
                SecureField("密码", text: $viewModel.password)
                    .textContentType(.password)
                    .submitLabel(.go)
            }
            .foodMapInputField()

            HStack(spacing: 10) {
                fieldIcon("server.rack")
                TextField("服务地址", text: $viewModel.apiBaseURL)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)
                    .textContentType(.URL)
            }
            .foodMapInputField()
        }
    }

    private var actions: some View {
        VStack(spacing: 12) {
            Button {
                Task { await viewModel.login(sessionStore: sessionStore) }
            } label: {
                HStack {
                    if viewModel.isLoading {
                        ProgressView()
                            .tint(.white)
                    } else {
                        Image(systemName: "arrow.right")
                    }
                    Text(viewModel.isLoading ? "登录中" : "登录")
                        .fontWeight(.semibold)
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(FoodMapPrimaryButtonStyle())
            .disabled(!viewModel.canSubmit || viewModel.isLoading)

            Button {
                showsRegister = true
            } label: {
                Label("注册账号", systemImage: "person.badge.plus")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(FoodMapSecondaryButtonStyle())
        }
    }

    @ViewBuilder
    private var errorText: some View {
        if let errorMessage = viewModel.errorMessage {
            Label(errorMessage, systemImage: "exclamationmark.triangle.fill")
                .font(.callout)
                .foregroundStyle(FoodMapTheme.persimmon)
                .padding(12)
                .background(FoodMapTheme.persimmon.opacity(0.10))
                .clipShape(RoundedRectangle(cornerRadius: FoodMapTheme.cardCornerRadius, style: .continuous))
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func fieldIcon(_ systemName: String) -> some View {
        Image(systemName: systemName)
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(FoodMapTheme.teaGreen)
            .frame(width: 22)
    }
}

#Preview {
    LoginView()
        .environmentObject(AuthSessionStore(tokenStore: PreviewTokenStore()))
}

private final class PreviewTokenStore: AuthTokenStore {
    func load() -> AuthTokens? { nil }
    func save(_ tokens: AuthTokens) throws {}
    func clear() {}
}
