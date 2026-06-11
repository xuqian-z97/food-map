package com.foodmap.common.redis;

import com.foodmap.common.validation.Check;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * 分布式锁持有凭证，释放锁时必须携带同一个 owner token 防止误删其他请求持有的锁。
 *
 * <p>该对象只表达锁持有事实，不应被持久化为业务事实；排查锁泄漏时关注 lockKey、lockedAt 和 leaseTime。</p>
 *
 * @param lockKey 已获取的 Redis 锁 Key。
 * @param ownerToken 锁持有人唯一令牌。
 * @param lockedAt 获取锁的时间。
 * @param leaseTime 锁租约时间。
 */
public record DistributedLockToken(
        String lockKey,
        String ownerToken,
        OffsetDateTime lockedAt,
        Duration leaseTime
) {

    /**
     * 校验锁持有凭证，避免释放锁时缺少归属校验字段。
     */
    public DistributedLockToken {
        lockKey = Check.notBlank("lockKey", lockKey);
        ownerToken = Check.notBlank("ownerToken", ownerToken);
        if (lockedAt == null) {
            throw new IllegalArgumentException("lockedAt must not be null");
        }
        if (leaseTime == null || leaseTime.isZero() || leaseTime.isNegative()) {
            throw new IllegalArgumentException("leaseTime must be positive");
        }
    }
}
