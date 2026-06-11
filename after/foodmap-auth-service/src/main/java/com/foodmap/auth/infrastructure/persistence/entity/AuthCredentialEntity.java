package com.foodmap.auth.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

/**
 * 认证凭证持久化实体，对应 `auth_credentials` 表，密码字段只能保存强哈希。
 */
public class AuthCredentialEntity extends BaseEntity {
    /**
     * 凭证业务主键。
     */
    private Long credentialId;

    /**
     * 账号业务主键，关联 auth_accounts.account_id。
     */
    private Long accountId;

    /**
     * 凭证类型，如 PASSWORD。
     */
    private String credentialType;

    /**
     * 密码哈希值，禁止保存明文密码。
     */
    private String passwordHash;

    /**
     * 密码哈希算法标识，如 PBKDF2WithHmacSHA256。
     */
    private String hashAlgorithm;

    public Long getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(Long credentialId) {
        this.credentialId = credentialId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }
}
