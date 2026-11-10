package com.foodmap.admin.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

import java.time.OffsetDateTime;

/**
 * 后台 Token 会话登记表持久化实体，对应 `admin_access_sessions` 表，不能作为 API 响应直接返回。
 */
public class AdminAccessSessionEntity extends BaseEntity {
    /**
     * 后台会话业务主键。
     */
    private Long sessionId;
    /**
     * 后台 Access Token 的 JWT jti，全局唯一。
     */
    private String tokenId;
    /**
     * 后台管理员业务主键。
     */
    private Long adminUserId;
    /**
     * 会话过期时间，与 Token 过期时间一致。
     */
    private OffsetDateTime expiresTime;
    /**
     * 会话撤销时间，登出后写入；空表示未撤销。
     */
    private OffsetDateTime revokedTime;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public Long getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(Long adminUserId) {
        this.adminUserId = adminUserId;
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
}
