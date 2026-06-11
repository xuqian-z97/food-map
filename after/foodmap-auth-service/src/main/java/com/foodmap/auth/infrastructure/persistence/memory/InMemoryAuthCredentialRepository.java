package com.foodmap.auth.infrastructure.persistence.memory;

import com.foodmap.auth.application.port.AuthCredentialRepository;
import com.foodmap.auth.domain.CredentialType;
import com.foodmap.auth.infrastructure.persistence.entity.AuthCredentialEntity;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证凭证内存仓储，用于 B1 阶段保存密码哈希并验证登录链路。
 */
public class InMemoryAuthCredentialRepository implements AuthCredentialRepository {
    private final Map<Long, AuthCredentialEntity> passwordCredentialsByAccountId = new ConcurrentHashMap<>();

    /**
     * 保存密码凭证。该方法只保存哈希，不接收明文密码。
     *
     * @param entity 待保存的认证凭证持久化实体。
     */
    @Override
    public void save(AuthCredentialEntity entity) {
        if (CredentialType.PASSWORD.name().equals(entity.getCredentialType())) {
            passwordCredentialsByAccountId.put(entity.getAccountId(), entity);
        }
    }

    /**
     * 根据账号业务主键查找密码凭证。
     *
     * @param accountId 账号业务主键。
     * @return 查询到的密码凭证实体，未命中时返回空 Optional。
     */
    @Override
    public Optional<AuthCredentialEntity> findPasswordByAccountId(Long accountId) {
        return Optional.ofNullable(passwordCredentialsByAccountId.get(accountId));
    }
}
