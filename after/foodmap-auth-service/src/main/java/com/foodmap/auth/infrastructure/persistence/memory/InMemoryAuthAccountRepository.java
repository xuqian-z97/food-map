package com.foodmap.auth.infrastructure.persistence.memory;

import com.foodmap.auth.infrastructure.persistence.entity.AuthAccountEntity;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证账号内存仓储，用于 B1 阶段在 Mapper 未落地前验证注册和登录用例。
 *
 * <p>后续替换为 MyBatis Mapper 时，应保持应用层仓储语义不变。</p>
 */
@Repository
public class InMemoryAuthAccountRepository {
    private final Map<Long, AuthAccountEntity> accountsByAccountId = new ConcurrentHashMap<>();

    /**
     * 保存认证账号实体。调用方负责生成账号业务主键。
     */
    public void save(AuthAccountEntity entity) {
        accountsByAccountId.put(entity.getAccountId(), entity);
    }

    /**
     * 根据账号业务主键查找账号，用于登录和 Token 续签链路。
     */
    public Optional<AuthAccountEntity> findByAccountId(Long accountId) {
        return Optional.ofNullable(accountsByAccountId.get(accountId));
    }

    /**
     * 根据账号名、手机号或邮箱查找账号，匹配时忽略邮箱大小写差异。
     */
    public Optional<AuthAccountEntity> findByLoginIdentifier(String loginIdentifier) {
        String normalized = loginIdentifier.trim().toLowerCase();
        return accountsByAccountId.values().stream()
                .filter(entity -> normalized.equals(normalize(entity.getAccountName()))
                        || normalized.equals(normalize(entity.getPhone()))
                        || normalized.equals(normalize(entity.getEmail())))
                .findFirst();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
}
