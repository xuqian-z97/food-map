package com.foodmap.common.security;

/**
 * FoodMap 内部可信身份请求头常量，由网关写入、下游服务读取。
 *
 * <p>这些请求头不能被外部客户端直接信任。网关在转发前会移除外部同名头，并只写入 userId 标准身份头。
 * accountId 仅作为 B1 旧链路兼容字段保留。</p>
 */
public final class FoodMapAuthHeaders {

    /**
     * 当前登录用户业务主键请求头，来自 Access Token 中的 userId。
     */
    public static final String USER_ID = "X-FoodMap-User-Id";

    /**
     * B1 旧身份模型账号业务主键请求头，后续新链路不得继续依赖。
     *
     * @deprecated 新身份透传只使用 {@link #USER_ID}。
     */
    @Deprecated
    public static final String ACCOUNT_ID = "X-FoodMap-Account-Id";

    private FoodMapAuthHeaders() {
    }
}
