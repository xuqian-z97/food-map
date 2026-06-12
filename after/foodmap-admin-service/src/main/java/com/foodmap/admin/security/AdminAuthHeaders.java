package com.foodmap.admin.security;

/**
 * 管理后台受信内部身份请求头常量，由网关完成后台认证后透传给后台服务。
 */
public final class AdminAuthHeaders {
    /**
     * 后台管理员业务主键请求头。
     */
    public static final String ADMIN_USER_ID = "X-Admin-User-Id";
    /**
     * 后台管理员权限码列表请求头，多个权限使用英文逗号分隔。
     */
    public static final String ADMIN_PERMISSIONS = "X-Admin-Permissions";

    private AdminAuthHeaders() {
    }
}
