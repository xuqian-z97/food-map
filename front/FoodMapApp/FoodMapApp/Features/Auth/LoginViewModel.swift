import Foundation

/// 登录页状态模型，负责表单校验、登录请求编排和错误提示。
@MainActor
final class LoginViewModel: ObservableObject {
    @Published var loginIdentifier = ""
    @Published var password = ""
    @Published var apiBaseURL = APIClient.preferredBaseURLString()
    @Published var isLoading = false
    @Published var errorMessage: String?

    /// 判断当前表单是否具备提交条件，避免空账号、空密码或空服务地址请求后端。
    var canSubmit: Bool {
        !loginIdentifier.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        && !password.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        && !apiBaseURL.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    /// 执行登录流程；成功后由 AuthSessionStore 触发根路由切换。
    func login(sessionStore: AuthSessionStore) async {
        guard canSubmit else {
            errorMessage = "请填写账号和密码"
            return
        }

        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            UserDefaults.standard.set(apiBaseURL, forKey: APIClient.baseURLDefaultsKey)
            try await sessionStore.login(
                identifier: loginIdentifier,
                password: password,
                baseURL: apiBaseURL
            )
            password = ""
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
