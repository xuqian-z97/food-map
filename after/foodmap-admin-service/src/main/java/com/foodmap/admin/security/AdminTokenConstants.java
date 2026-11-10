package com.foodmap.admin.security;

/**
 * 后台 Access Token 的 JWT 契约常量，签发与验签两端必须使用同一份取值。
 *
 * <p>网关侧存在同值常量副本（不允许依赖本模块）；调整取值时必须两端同步并让存量管理员重新登录。</p>
 */
public final class AdminTokenConstants {
    /**
     * 后台 Token 签发方，只有管理后台服务允许签发。
     */
    public static final String ISSUER = "foodmap-admin-service";
    /**
     * 后台 Token 受众，只有后台 API 消费方允许验签通过。
     */
    public static final String AUDIENCE = "foodmap-admin-api";
    /**
     * 后台 Token 类型声明值，用于和普通 App Token、其他 JOSE Token 区分。
     */
    public static final String TOKEN_TYPE = "foodmap-admin+jwt";
    /**
     * Token 类型声明名。
     */
    public static final String CLAIM_TOKEN_TYPE = "typ";
    /**
     * 签发时角色编码列表声明名，接口授权仍以数据库当前授权为准。
     */
    public static final String CLAIM_ROLES = "roles";
    /**
     * 签发时权限版本声明名，用于检测授权变更后旧 Token 失效。
     */
    public static final String CLAIM_PERMISSION_VERSION = "permissionVersion";

    private AdminTokenConstants() {
    }
}
