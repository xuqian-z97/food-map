package com.foodmap.admin.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

/**
 * 后台角色表持久化实体，对应 `admin_roles` 表，不能作为 API 响应直接返回。
 */
public class AdminRoleEntity extends BaseEntity {
    /**
     * 后台角色业务主键。
     */
    private Long roleId;
    /**
     * 后台角色编码，如 SUPER_ADMIN、CONTENT_REVIEWER、OPS_VIEWER。
     */
    private String roleCode;
    /**
     * 后台角色名称。
     */
    private String roleName;
    /**
     * 角色职责说明。
     */
    private String roleDescription;
    /**
     * 角色状态，如 ACTIVE、DISABLED。
     */
    private String roleStatus;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleDescription() {
        return roleDescription;
    }

    public void setRoleDescription(String roleDescription) {
        this.roleDescription = roleDescription;
    }

    public String getRoleStatus() {
        return roleStatus;
    }

    public void setRoleStatus(String roleStatus) {
        this.roleStatus = roleStatus;
    }
}
