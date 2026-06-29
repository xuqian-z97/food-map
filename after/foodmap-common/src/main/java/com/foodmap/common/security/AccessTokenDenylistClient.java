package com.foodmap.common.security;

import java.time.OffsetDateTime;

/**
 * Access Token 拒绝名单客户端，统一认证服务写入和网关读取登出后的短期失效状态。
 *
 * <p>调用方只能传入 Token 摘要，不能传入明文 Access Token。真实 Redis 实现必须为写入设置 TTL，
 * TTL 截止到 Access Token 原过期时间，避免拒绝名单长期膨胀。</p>
 */
public interface AccessTokenDenylistClient {

    /**
     * 将 Access Token 摘要写入拒绝名单。
     *
     * @param accessTokenHash Access Token 哈希摘要，禁止传入明文 Token。
     * @param expiresTime Access Token 原过期时间，用于计算 Redis TTL。
     */
    void deny(String accessTokenHash, OffsetDateTime expiresTime);

    /**
     * 判断 Access Token 摘要是否已经进入拒绝名单。
     *
     * @param accessTokenHash Access Token 哈希摘要，禁止传入明文 Token。
     * @return 如果摘要命中拒绝名单则返回 true。
     */
    boolean contains(String accessTokenHash);
}
