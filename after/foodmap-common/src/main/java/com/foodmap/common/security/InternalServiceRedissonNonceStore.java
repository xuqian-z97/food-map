package com.foodmap.common.security;

import com.foodmap.common.redis.CacheKeyBuilder;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redisson 的内部服务 nonce 去重存储。
 *
 * <p>Key 统一为 {@code foodmap:internal:nonce:v1:{service}:{nonce}}，Value 只保存固定标记，
 * 不包含签名或请求内容。通过 {@code trySet} 的原子语义判断是否为首次请求。</p>
 */
public class InternalServiceRedissonNonceStore implements InternalServiceNonceStore {

    /**
     * Redis Key 服务片段，标识该状态属于内部服务调用基础设施。
     */
    private static final String SERVICE = "internal";

    /**
     * Redis Key 业务片段，用于排查 nonce 去重状态来源。
     */
    private static final String BIZ = "nonce";

    /**
     * Redis Key 版本，结构变化时通过版本隔离旧数据。
     */
    private static final String VERSION = "v1";

    /**
     * nonce 占位 Value，不包含任何敏感信息。
     */
    private static final String CLAIMED = "1";

    private final RedissonClient redissonClient;

    /**
     * 创建基于 Redisson 的 nonce 去重存储。
     *
     * @param redissonClient Redisson 客户端。
     */
    public InternalServiceRedissonNonceStore(RedissonClient redissonClient) {
        if (redissonClient == null) {
            throw new IllegalArgumentException("redissonClient must not be null");
        }
        this.redissonClient = redissonClient;
    }

    /**
     * 原子占用 nonce，首次写入成功返回 true。
     *
     * @param serviceName 调用方服务名。
     * @param nonce 一次性随机串。
     * @param ttlSeconds nonce 保留时间，单位秒。
     * @return 首次占用返回 true，已存在返回 false。
     */
    @Override
    public boolean claim(String serviceName, String nonce, long ttlSeconds) {
        String key = CacheKeyBuilder.builder()
                .service(SERVICE)
                .biz(BIZ)
                .version(VERSION)
                .key(serviceName + "-" + nonce)
                .build();
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        return bucket.trySet(CLAIMED, ttlSeconds, TimeUnit.SECONDS);
    }
}
