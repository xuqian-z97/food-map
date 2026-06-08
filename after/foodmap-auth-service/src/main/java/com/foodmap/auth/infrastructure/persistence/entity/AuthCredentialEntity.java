package com.foodmap.auth.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

/**
 * 认证凭证持久化实体，对应 `auth_credentials` 表，密码字段只能保存强哈希。
 */
public class AuthCredentialEntity extends BaseEntity {
    private Long credentialId;
    private Long accountId;
    private String credentialType;
    private String passwordHash;
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
