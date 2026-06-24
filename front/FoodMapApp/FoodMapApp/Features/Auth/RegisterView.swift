import SwiftUI

/// 注册页面，作为当前阶段的本地联调入口，成功后展示后端返回的账号 ID。
struct RegisterView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var sessionStore: AuthSessionStore

    @State private var accountName = ""
    @State private var phone = ""
    @State private var email = ""
    @State private var nickname = ""
    @State private var password = ""
    @State private var apiBaseURL: String
    @State private var isLoading = false
    @State private var message: String?

    init(apiBaseURL: String) {
        _apiBaseURL = State(initialValue: apiBaseURL)
    }

    var body: some View {
        NavigationStack {
            ZStack {
                FoodMapTheme.ricePaper
                    .ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 22) {
                        header
                        accountFields
                        connectionFields
                        messageView
                        submitButton
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 24)
                    .padding(.bottom, 28)
                    .frame(maxWidth: 520, alignment: .leading)
                }
            }
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark")
                    }
                    .buttonStyle(FoodMapIconButtonStyle())
                    .accessibilityLabel("关闭注册页")
                }
            }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("创建账号")
                .font(.system(size: 34, weight: .bold, design: .rounded))
                .foregroundStyle(FoodMapTheme.ink)
            Text("把喜欢的店、想复吃的菜，慢慢收进自己的地图。")
                .font(.subheadline)
                .foregroundStyle(FoodMapTheme.mutedInk)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var accountFields: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionTitle("账号信息")
            authField(icon: "person.text.rectangle.fill") {
                TextField("账号名", text: $accountName)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            }
            authField(icon: "phone.fill") {
                TextField("手机号", text: $phone)
                    .keyboardType(.phonePad)
            }
            authField(icon: "envelope.fill") {
                TextField("邮箱", text: $email)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.emailAddress)
            }
            authField(icon: "leaf.fill") {
                TextField("昵称", text: $nickname)
            }
            authField(icon: "lock.fill") {
                SecureField("密码", text: $password)
            }
        }
    }

    private var connectionFields: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionTitle("联调连接")
            authField(icon: "server.rack") {
                TextField("服务地址", text: $apiBaseURL)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)
            }
        }
    }

    @ViewBuilder
    private var messageView: some View {
        if let message {
            Label(
                message,
                systemImage: message.contains("成功") ? "checkmark.seal.fill" : "exclamationmark.triangle.fill"
            )
            .font(.callout)
            .foregroundStyle(message.contains("成功") ? FoodMapTheme.teaGreen : FoodMapTheme.persimmon)
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background((message.contains("成功") ? FoodMapTheme.teaGreen : FoodMapTheme.persimmon).opacity(0.10))
            .clipShape(RoundedRectangle(cornerRadius: FoodMapTheme.cardCornerRadius, style: .continuous))
        }
    }

    private var submitButton: some View {
        Button {
            Task { await register() }
        } label: {
            HStack {
                if isLoading {
                    ProgressView()
                        .tint(.white)
                } else {
                    Image(systemName: "checkmark")
                }
                Text(isLoading ? "提交中" : "注册")
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(FoodMapPrimaryButtonStyle())
        .disabled(!canSubmit || isLoading)
    }

    private var canSubmit: Bool {
        !accountName.trimmed.isEmpty
        && !nickname.trimmed.isEmpty
        && !password.trimmed.isEmpty
        && !apiBaseURL.trimmed.isEmpty
    }

    private func register() async {
        isLoading = true
        message = nil
        defer { isLoading = false }

        do {
            let request = RegisterRequest(
                accountName: accountName.trimmed,
                phone: phone.trimmedNilIfEmpty,
                email: email.trimmedNilIfEmpty,
                password: password,
                nickname: nickname.trimmed,
                registeredChannel: "IOS"
            )
            UserDefaults.standard.set(apiBaseURL, forKey: APIClient.baseURLDefaultsKey)
            let response = try await sessionStore.register(request: request, baseURL: apiBaseURL)
            message = "注册成功：\(response.accountId)"
        } catch {
            message = error.localizedDescription
        }
    }

    private func sectionTitle(_ title: String) -> some View {
        Text(title)
            .font(.subheadline.weight(.bold))
            .foregroundStyle(FoodMapTheme.mutedInk)
    }

    private func authField<Content: View>(
        icon: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(FoodMapTheme.teaGreen)
                .frame(width: 22)
            content()
        }
        .foodMapInputField()
    }
}

private extension String {
    var trimmed: String {
        trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var trimmedNilIfEmpty: String? {
        let value = trimmed
        return value.isEmpty ? nil : value
    }
}

#Preview {
    RegisterView(apiBaseURL: APIClient.defaultBaseURLString)
        .environmentObject(AuthSessionStore(tokenStore: PreviewRegisterTokenStore()))
}

private final class PreviewRegisterTokenStore: AuthTokenStore {
    func load() -> AuthTokens? { nil }
    func save(_ tokens: AuthTokens) throws {}
    func clear() {}
}
