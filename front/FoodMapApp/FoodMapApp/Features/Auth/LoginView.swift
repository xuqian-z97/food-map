import SwiftUI

/// 登录页面，提供账号名、手机号或邮箱登录入口，并允许本地联调时切换服务地址。
struct LoginView: View {
    @EnvironmentObject private var sessionStore: AuthSessionStore
    @StateObject private var viewModel = LoginViewModel()
    @State private var showsRegister = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    header
                    fields
                    actions
                    errorText
                }
                .padding(24)
                .frame(maxWidth: 520, alignment: .leading)
            }
            .frame(maxWidth: .infinity)
            .background(Color(.systemGroupedBackground))
            .sheet(isPresented: $showsRegister) {
                RegisterView(apiBaseURL: viewModel.apiBaseURL)
                    .environmentObject(sessionStore)
            }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("FoodMap")
                .font(.system(size: 42, weight: .bold, design: .rounded))
            Text("登录")
                .font(.title2.weight(.semibold))
                .foregroundStyle(.secondary)
        }
        .padding(.top, 28)
    }

    private var fields: some View {
        VStack(spacing: 14) {
            TextField("账号名 / 手机号 / 邮箱", text: $viewModel.loginIdentifier)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .keyboardType(.emailAddress)
                .textContentType(.username)
                .submitLabel(.next)
                .fieldStyle()

            SecureField("密码", text: $viewModel.password)
                .textContentType(.password)
                .submitLabel(.go)
                .fieldStyle()

            TextField("服务地址", text: $viewModel.apiBaseURL)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .keyboardType(.URL)
                .textContentType(.URL)
                .fieldStyle()
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
                        Image(systemName: "arrow.right.circle.fill")
                    }
                    Text(viewModel.isLoading ? "登录中" : "登录")
                        .fontWeight(.semibold)
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .disabled(!viewModel.canSubmit || viewModel.isLoading)

            Button {
                showsRegister = true
            } label: {
                Label("注册账号", systemImage: "person.badge.plus")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
            .controlSize(.large)
        }
    }

    @ViewBuilder
    private var errorText: some View {
        if let errorMessage = viewModel.errorMessage {
            Text(errorMessage)
                .font(.callout)
                .foregroundStyle(.red)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

private extension View {
    func fieldStyle() -> some View {
        self
            .padding(.horizontal, 14)
            .frame(height: 48)
            .background(Color(.secondarySystemGroupedBackground))
            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
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
