package com.foodmap.common.security;

/**
 * FoodMap 内部可信身份请求头常量，由网关写入、下游服务读取。
 *
 * <p>这些请求头不能被外部客户端直接信任。网关在转发前会覆盖同名头，排查身份问题时优先检查网关日志和这些头值。</p>
 */
public final class FoodMapAuthHeaders {

    /**
     * 当前登录用户业务主键请求头，来自 Access Token 中的 userId。
     */
    public static final String USER_ID = "X-FoodMap-User-Id";

    /**
     * 当前登录账号业务主键请求头，来自 Access Token 中的 accountId。
     */
    public static final String ACCOUNT_ID = "X-FoodMap-Account-Id";

    private FoodMapAuthHeaders() {
    }
}
