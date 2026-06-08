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
            Form {
                Section("账号") {
                    TextField("账号名", text: $accountName)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    TextField("手机号", text: $phone)
                        .keyboardType(.phonePad)
                    TextField("邮箱", text: $email)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.emailAddress)
                    TextField("昵称", text: $nickname)
                    SecureField("密码", text: $password)
                }

                Section("连接") {
                    TextField("服务地址", text: $apiBaseURL)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.URL)
                }

                if let message {
                    Section {
                        Text(message)
                            .foregroundStyle(message.contains("成功") ? .green : .red)
                    }
                }

                Section {
                    Button {
                        Task { await register() }
                    } label: {
                        HStack {
                            if isLoading {
                                ProgressView()
                            } else {
                                Image(systemName: "checkmark.circle.fill")
                            }
                            Text(isLoading ? "提交中" : "注册")
                        }
                    }
                    .disabled(!canSubmit || isLoading)
                }
            }
            .navigationTitle("注册")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭") { dismiss() }
                }
            }
        }
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
            UserDefaults.standard.set(apiBaseURL, forKey: "foodmap.apiBaseURL")
            let response = try await sessionStore.register(request: request, baseURL: apiBaseURL)
            message = "注册成功：\(response.accountId)"
        } catch {
            message = error.localizedDescription
        }
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
    RegisterView(apiBaseURL: "http://127.0.0.1:8081")
        .environmentObject(AuthSessionStore(tokenStore: PreviewRegisterTokenStore()))
}

private final class PreviewRegisterTokenStore: AuthTokenStore {
    func load() -> AuthTokens? { nil }
    func save(_ tokens: AuthTokens) throws {}
    func clear() {}
}
