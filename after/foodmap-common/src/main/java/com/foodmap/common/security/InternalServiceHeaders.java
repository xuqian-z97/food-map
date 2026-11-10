package com.foodmap.common.security;

/**
 * 内部服务调用签名请求头名称常量。
 *
 * <p>内部调用方在发起请求时写入这四个请求头，服务端过滤器和调用方共用同一套常量，
 * 避免一端改名另一端悄悄失效。排查内部接口 401 时优先确认四个头是否齐全。</p>
 */
public final class InternalServiceHeaders {

    /**
     * 调用方服务名，必须与 Nacos 注册名一致，服务端按路径白名单校验该值。
     */
    public static final String SERVICE = "X-FoodMap-Service";

    /**
     * 请求发起时间，Unix 毫秒时间戳，服务端只接受时间窗内的请求。
     */
    public static final String TIMESTAMP = "X-FoodMap-Timestamp";

    /**
     * 一次性随机串，服务端通过 Redis 去重，同一 nonce 重复出现按重放拒绝。
     */
    public static final String NONCE = "X-FoodMap-Nonce";

    /**
     * HMAC-SHA256 签名，覆盖服务名、时间戳、nonce、方法、路径和请求体摘要。
     */
    public static final String SIGNATURE = "X-FoodMap-Signature";

    private InternalServiceHeaders() {
    }
}
