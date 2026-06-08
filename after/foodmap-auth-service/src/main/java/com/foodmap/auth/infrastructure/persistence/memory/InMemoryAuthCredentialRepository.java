package com.foodmap.auth.infrastructure.persistence.memory;

import com.foodmap.auth.domain.CredentialType;
import com.foodmap.auth.infrastructure.persistence.entity.AuthCredentialEntity;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证凭证内存仓储，用于 B1 阶段保存密码哈希并验证登录链路。
 */
@Repository
public class InMemoryAuthCredentialRepository {
    private final Map<Long, AuthCredentialEntity> passwordCredentialsByAccountId = new ConcurrentHashMap<>();

    /**
     * 保存密码凭证。该方法只保存哈希，不接收明文密码。
     */
    public void save(AuthCredentialEntity entity) {
        if (CredentialType.PASSWORD.name().equals(entity.getCredentialType())) {
            passwordCredentialsByAccountId.put(entity.getAccountId(), entity);
        }
    }

    /**
     * 根据账号业务主键查找密码凭证。
     */
    public Optional<AuthCredentialEntity> findPasswordByAccountId(Long accountId) {
        return Optional.ofNullable(passwordCredentialsByAccountId.get(accountId));
    }
}
