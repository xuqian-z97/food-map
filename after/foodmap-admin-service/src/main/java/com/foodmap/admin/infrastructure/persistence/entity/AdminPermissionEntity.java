package com.foodmap.admin.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

/**
 * 后台权限表持久化实体，对应 `admin_permissions` 表，不能作为 API 响应直接返回。
 */
public class AdminPermissionEntity extends BaseEntity {
    /**
     * 后台权限业务主键。
     */
    private Long permissionId;
    /**
     * 权限编码，如 CONTENT_REVIEW_READ、CONTENT_REVIEW_DECIDE。
     */
    private String permissionCode;
    /**
     * 权限名称。
     */
    private String permissionName;
    /**
     * 权限所属模块，如 CONTENT、USER、STORE、LOG。
     */
    private String permissionModule;
    /**
     * 权限用途和排查说明。
     */
    private String permissionDescription;
    /**
     * 权限状态，如 ACTIVE、DISABLED。
     */
    private String permissionStatus;

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public String getPermissionModule() {
        return permissionModule;
    }

    public void setPermissionModule(String permissionModule) {
        this.permissionModule = permissionModule;
    }

    public String getPermissionDescription() {
        return permissionDescription;
    }

    public void setPermissionDescription(String permissionDescription) {
        this.permissionDescription = permissionDescription;
    }

    public String getPermissionStatus() {
        return permissionStatus;
    }

    public void setPermissionStatus(String permissionStatus) {
        this.permissionStatus = permissionStatus;
    }
}
