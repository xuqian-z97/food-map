package com.foodmap.admin.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

import java.time.OffsetDateTime;

/**
 * 后台管理员表持久化实体，对应 `admin_users` 表，不能作为 API 响应直接返回。
 */
public class AdminUserEntity extends BaseEntity {
    /**
     * 后台管理员业务主键。
     */
    private Long adminUserId;

    /**
     * 后台登录账号名。
     */
    private String username;

    /**
     * 后台登录密码哈希。
     */
    private String passwordHash;

    /**
     * 后台展示名称。
     */
    private String displayName;

    /**
     * 脱敏手机号，用于后台安全联系和审计展示。
     */
    private String mobileMasked;

    /**
     * 脱敏邮箱，用于后台安全联系和审计展示。
     */
    private String emailMasked;

    /**
     * 管理员状态，如 ACTIVE、DISABLED、LOCKED。
     */
    private String adminStatus;

    /**
     * 权限版本，角色或权限变更后递增，用于使旧 Token 失效。
     */
    private Long permissionVersion;

    /**
     * 最近一次后台登录时间。
     */
    private OffsetDateTime lastLoginTime;

    public Long getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(Long adminUserId) {
        this.adminUserId = adminUserId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getMobileMasked() {
        return mobileMasked;
    }

    public void setMobileMasked(String mobileMasked) {
        this.mobileMasked = mobileMasked;
    }

    public String getEmailMasked() {
        return emailMasked;
    }

    public void setEmailMasked(String emailMasked) {
        this.emailMasked = emailMasked;
    }

    public String getAdminStatus() {
        return adminStatus;
    }

    public void setAdminStatus(String adminStatus) {
        this.adminStatus = adminStatus;
    }

    public Long getPermissionVersion() {
        return permissionVersion;
    }

    public void setPermissionVersion(Long permissionVersion) {
        this.permissionVersion = permissionVersion;
    }

    public OffsetDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(OffsetDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }
}
