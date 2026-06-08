package com.foodmap.auth.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

import java.time.OffsetDateTime;

/**
 * Refresh Token 持久化实体，对应 `refresh_tokens` 表，只保存令牌哈希和状态。
 */
public class RefreshTokenEntity extends BaseEntity {
    private Long tokenId;
    private Long accountId;
    private String tokenHash;
    private OffsetDateTime expiresTime;
    private OffsetDateTime revokedTime;
    private String tokenStatus;

    public Long getTokenId() {
        return tokenId;
    }

    public void setTokenId(Long tokenId) {
        this.tokenId = tokenId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public OffsetDateTime getExpiresTime() {
        return expiresTime;
    }

    public void setExpiresTime(OffsetDateTime expiresTime) {
        this.expiresTime = expiresTime;
    }

    public OffsetDateTime getRevokedTime() {
        return revokedTime;
    }

    public void setRevokedTime(OffsetDateTime revokedTime) {
        this.revokedTime = revokedTime;
    }

    public String getTokenStatus() {
        return tokenStatus;
    }

    public void setTokenStatus(String tokenStatus) {
        this.tokenStatus = tokenStatus;
    }
}
