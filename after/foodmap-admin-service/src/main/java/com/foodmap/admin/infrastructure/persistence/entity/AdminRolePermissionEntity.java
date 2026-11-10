package com.foodmap.admin.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

import java.time.OffsetDateTime;

/**
 * 后台角色权限关联表持久化实体，对应 `admin_role_permissions` 表，不能作为 API 响应直接返回。
 */
public class AdminRolePermissionEntity extends BaseEntity {
    /**
     * 角色权限关联业务主键。
     */
    private Long relationId;
    /**
     * 后台角色业务主键。
     */
    private Long roleId;
    /**
     * 后台权限业务主键。
     */
    private Long permissionId;
    /**
     * 授权人后台管理员业务主键。
     */
    private Long grantedBy;
    /**
     * 授权时间。
     */
    private OffsetDateTime grantedTime;

    public Long getRelationId() {
        return relationId;
    }

    public void setRelationId(Long relationId) {
        this.relationId = relationId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }

    public Long getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(Long grantedBy) {
        this.grantedBy = grantedBy;
    }

    public OffsetDateTime getGrantedTime() {
        return grantedTime;
    }

    public void setGrantedTime(OffsetDateTime grantedTime) {
        this.grantedTime = grantedTime;
    }
}
