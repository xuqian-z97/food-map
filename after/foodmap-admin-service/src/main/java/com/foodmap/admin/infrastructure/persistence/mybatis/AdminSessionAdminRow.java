package com.foodmap.admin.infrastructure.persistence.mybatis;

import java.time.OffsetDateTime;

/**
 * 后台会话与管理员联合查询行，是认证授权快照 SQL 的持久化投影，只允许仓储实现转换后向上传递。
 */
public class AdminSessionAdminRow {
    /**
     * 后台管理员业务主键。
     */
    private Long adminUserId;
    /**
     * 管理员状态，如 ACTIVE、DISABLED、LOCKED。
     */
    private String adminStatus;
    /**
     * 当前权限版本，角色或权限变更后递增。
     */
    private Long permissionVersion;
    /**
     * 会话过期时间，与 Token 过期时间一致。
     */
    private OffsetDateTime expiresTime;
    /**
     * 会话撤销时间，登出后写入；空表示未撤销。
     */
    private OffsetDateTime revokedTime;

    public Long getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(Long adminUserId) {
        this.adminUserId = adminUserId;
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
