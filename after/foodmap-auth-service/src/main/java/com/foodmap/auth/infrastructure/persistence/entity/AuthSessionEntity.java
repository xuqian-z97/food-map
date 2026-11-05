package com.foodmap.auth.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

import java.time.OffsetDateTime;

/**
 * 认证会话持久化实体，对应 `auth_sessions` 表。
 *
 * <p>Access Token 不落库，Refresh Token 只保存哈希。字段 Javadoc 必须与 V4 迁移脚本中的
 * `comment on column` 保持一致；排查刷新或退出登录问题时，优先核对 refresh_token_hash、
 * expires_time、revoked_time 和 session_status 四个字段。</p>
 */
public class AuthSessionEntity extends BaseEntity {

    /**
     * 会话 ID，用于多端会话管理和 Refresh Token 轮换。
     */
    private String sessionId;

    /**
     * 用户业务主键，关联用户服务的 users.user_id。
     */
    private Long userId;

    /**
     * Refresh Token 哈希值，禁止保存明文 Refresh Token。
     */
    private String refreshTokenHash;

    /**
     * 设备类型，如 IOS、WEB、ADMIN。
     */
    private String deviceType;

    /**
     * 设备名称或脱敏设备摘要。
     */
    private String deviceName;

    /**
     * Refresh Token 过期时间。
     */
    private OffsetDateTime expiresTime;

    /**
     * 会话被撤销时间，空值表示未撤销。
     */
    private OffsetDateTime revokedTime;

    /**
     * 会话状态，如 ACTIVE、REVOKED、EXPIRED。
     */
    private String sessionStatus;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public void setRefreshTokenHash(String refreshTokenHash) {
        this.refreshTokenHash = refreshTokenHash;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
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

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }
}
