package com.foodmap.auth.application.port;

import com.foodmap.auth.infrastructure.persistence.entity.AuthCredentialEntity;

import java.util.Optional;

/**
 * 认证凭证仓储端口，应用层通过该接口读写密码哈希等登录凭证。
 */
public interface AuthCredentialRepository {

    /**
     * 保存认证凭证。实现方只能持久化哈希或安全凭证材料，不能保存明文密码。
     */
    void save(AuthCredentialEntity entity);

    /**
     * 根据账号业务主键查询密码凭证，用于登录密码校验。
     */
    Optional<AuthCredentialEntity> findPasswordByAccountId(Long accountId);
}
