package com.foodmap.admin.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

import java.time.OffsetDateTime;

/**
 * 后台管理员角色关联表持久化实体，对应 `admin_user_roles` 表，不能作为 API 响应直接返回。
 */
public class AdminUserRoleEntity extends BaseEntity {
    /**
     * 管理员角色关联业务主键。
     */
    private Long relationId;
    /**
     * 后台管理员业务主键。
     */
    private Long adminUserId;
    /**
     * 后台角色业务主键。
     */
    private Long roleId;
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

    public Long getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(Long adminUserId) {
        this.adminUserId = adminUserId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
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
