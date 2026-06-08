import Foundation
import Security

/// 基于 iOS Keychain 的 Token 存储实现，避免将 Token 暴露在 UserDefaults 或明文文件中。
final class KeychainTokenStore: AuthTokenStore {
    private let service = "com.foodmap.auth"
    private let account = "tokens"

    /// 从 Keychain 读取 Token；若用户尚未登录或数据损坏，则返回 nil。
    func load() -> AuthTokens? {
        var query = baseQuery()
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        query[kSecReturnData as String] = true

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess, let data = result as? Data else {
            return nil
        }
        return try? JSONDecoder().decode(AuthTokens.self, from: data)
    }

    /// 覆盖保存 Token；先删除旧项再写入，避免重复 Keychain 项导致保存失败。
    func save(_ tokens: AuthTokens) throws {
        let data = try JSONEncoder().encode(tokens)
        clear()

        var item = baseQuery()
        item[kSecValueData as String] = data
        item[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly

        let status = SecItemAdd(item as CFDictionary, nil)
        guard status == errSecSuccess else {
            throw KeychainError.unhandled(status: status)
        }
    }

    /// 删除本地 Token；Keychain 删除失败不向上抛出，退出登录保持幂等。
    func clear() {
        SecItemDelete(baseQuery() as CFDictionary)
    }

    /// 生成统一 Keychain 查询条件，保证读取、保存和删除命中同一条记录。
    private func baseQuery() -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
    }
}

/// Keychain 操作错误，用于将系统 OSStatus 转换为用户可读错误。
enum KeychainError: LocalizedError {
    /// 未覆盖的 Keychain 系统错误，排查时可结合 OSStatus 定位。
    case unhandled(status: OSStatus)

    var errorDescription: String? {
        switch self {
        case .unhandled:
            return "Token 保存失败"
        }
    }
}
