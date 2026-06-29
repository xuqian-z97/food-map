package com.foodmap.common.redis.redisson;

import com.foodmap.common.redis.CacheKeyBuilder;
import com.foodmap.common.security.AccessTokenDenylistClient;
import com.foodmap.common.validation.Check;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redisson 的 Access Token 拒绝名单客户端。
 *
 * <p>本类是 common 内部基础设施适配器，业务服务和网关只依赖 {@link AccessTokenDenylistClient}。
 * Redis Key 统一使用 {@code foodmap:auth:access-token-denylist:v1:{tokenHash}}，Value 只保存非敏感标记。</p>
 */
public class RedissonAccessTokenDenylistClient implements AccessTokenDenylistClient {

    /**
     * Access Token 拒绝名单所属服务片段，沿用认证服务作为状态事实边界。
     */
    private static final String SERVICE = "auth";

    /**
     * Access Token 拒绝名单业务片段，用于排查 Redis Key 来源。
     */
    private static final String BIZ = "access-token-denylist";

    /**
     * Access Token 拒绝名单 Key 版本，结构变化时通过版本隔离旧数据。
     */
    private static final String VERSION = "v1";

    /**
     * 拒绝名单 Value 固定标记，不包含 Token 明文或用户敏感信息。
     */
    private static final String DENIED_VALUE = "1";

    private final RedissonClient redissonClient;

    /**
     * 创建 Redisson Access Token 拒绝名单客户端。
     *
     * @param redissonClient Redisson 客户端。
     */
    public RedissonAccessTokenDenylistClient(RedissonClient redissonClient) {
        if (redissonClient == null) {
            throw new IllegalArgumentException("redissonClient must not be null");
        }
        this.redissonClient = redissonClient;
    }

    /**
     * 将 Access Token 摘要写入 Redis，并按原 Token 过期时间设置 TTL。
     *
     * @param accessTokenHash Access Token 哈希摘要，禁止传入明文 Token。
     * @param expiresTime Access Token 原过期时间，用于计算 Redis TTL。
     */
    @Override
    public void deny(String accessTokenHash, OffsetDateTime expiresTime) {
        String key = denylistKey(accessTokenHash);
        if (expiresTime == null) {
            throw new IllegalArgumentException("expiresTime must not be null");
        }
        long ttlMillis = Duration.between(OffsetDateTime.now(), expiresTime).toMillis();
        if (ttlMillis <= 0) {
            return;
        }
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        bucket.set(DENIED_VALUE, ttlMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 查询 Access Token 摘要是否命中 Redis 拒绝名单。
     *
     * @param accessTokenHash Access Token 哈希摘要，禁止传入明文 Token。
     * @return 如果摘要存在于 Redis 拒绝名单中则返回 true。
     */
    @Override
    public boolean contains(String accessTokenHash) {
        RBucket<String> bucket = redissonClient.getBucket(denylistKey(accessTokenHash), StringCodec.INSTANCE);
        return bucket.isExists();
    }

    /**
     * 生成 Access Token 拒绝名单 Redis Key。
     *
     * @param accessTokenHash Access Token 哈希摘要。
     * @return 符合 FoodMap Redis Key 规范的拒绝名单 Key。
     */
    private String denylistKey(String accessTokenHash) {
        return CacheKeyBuilder.builder()
                .service(SERVICE)
                .biz(BIZ)
                .version(VERSION)
                .key(Check.notBlank("accessTokenHash", accessTokenHash))
                .build();
    }
}
