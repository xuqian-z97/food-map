package com.foodmap.admin.security;

/**
 * 管理后台权限码枚举，用于 Controller 层声明接口所需 RBAC 权限。
 */
public enum AdminPermissionCode {
    /**
     * 查询接口访问摘要日志的权限，仅返回脱敏摘要字段。
     */
    LOG_ACCESS_READ
}
