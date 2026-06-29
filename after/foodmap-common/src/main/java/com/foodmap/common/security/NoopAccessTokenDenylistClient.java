package com.foodmap.common.security;

import java.time.OffsetDateTime;

/**
 * 空操作 Access Token 拒绝名单客户端，用于未启用 Redis 的本地测试和轻量启动场景。
 *
 * <p>该实现不会保存任何拒绝状态。生产或真实联调需要启用 Redisson 适配器，避免登出后的 Access Token
 * 在原过期时间前继续通过网关。</p>
 */
public class NoopAccessTokenDenylistClient implements AccessTokenDenylistClient {

    /**
     * 忽略 Access Token 摘要写入请求，保持无 Redis 环境下服务可启动。
     *
     * @param accessTokenHash Access Token 哈希摘要。
     * @param expiresTime Access Token 原过期时间。
     */
    @Override
    public void deny(String accessTokenHash, OffsetDateTime expiresTime) {
        // Noop fallback for local tests without Redis.
    }

    /**
     * 空实现永远不命中拒绝名单。
     *
     * @param accessTokenHash Access Token 哈希摘要。
     * @return 固定返回 false。
     */
    @Override
    public boolean contains(String accessTokenHash) {
        return false;
    }
}
