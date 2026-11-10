package com.foodmap.common.security;

/**
 * 内部服务 nonce 去重存储，保证同一 nonce 在时间窗内只能成功使用一次。
 *
 * <p>实现必须使用原子写入语义：只有首次写入成功才返回 true。Redis 不可用时由调用方按
 * fail-closed 处理，不能降级为「放行」。</p>
 */
public interface InternalServiceNonceStore {

    /**
     * 尝试占用一个 nonce。
     *
     * @param serviceName 调用方服务名。
     * @param nonce 一次性随机串。
     * @param ttlSeconds nonce 保留时间，单位秒。
     * @return 首次占用返回 true，已存在返回 false。
     */
    boolean claim(String serviceName, String nonce, long ttlSeconds);
}
