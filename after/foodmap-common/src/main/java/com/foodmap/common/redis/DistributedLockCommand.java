package com.foodmap.common.redis;

import com.foodmap.common.validation.Check;

import java.time.Duration;

/**
 * 分布式锁获取命令，描述一次 Redis 锁请求需要的 Key、持有人令牌、等待时间和租约时间。
 *
 * <p>业务代码应使用业务主键或唯一性字段构造 lockKey；排查并发问题时重点核对 lockKey 粒度是否过粗或过细。</p>
 *
 * @param lockKey 统一格式的 Redis 锁 Key。
 * @param ownerToken 锁持有人唯一令牌，释放锁时必须用它校验归属。
 * @param waitTime 最大等待获取锁时间，零值表示只尝试一次。
 * @param leaseTime 锁自动释放时间，必须大于零，避免服务异常后永久占锁。
 * @param watchdogOptions 看门狗续期配置，未启用时使用 {@link DistributedLockWatchdogOptions#disabled()}。
 */
public record DistributedLockCommand(
        String lockKey,
        String ownerToken,
        Duration waitTime,
        Duration leaseTime,
        DistributedLockWatchdogOptions watchdogOptions
) {

    /**
     * 创建不启用看门狗的分布式锁命令，适合短临界区。
     *
     * @param lockKey 统一格式的 Redis 锁 Key。
     * @param ownerToken 锁持有人唯一令牌。
     * @param waitTime 最大等待获取锁时间。
     * @param leaseTime 锁自动释放时间。
     */
    public DistributedLockCommand(String lockKey, String ownerToken, Duration waitTime, Duration leaseTime) {
        this(lockKey, ownerToken, waitTime, leaseTime, DistributedLockWatchdogOptions.disabled());
    }

    /**
     * 校验分布式锁命令，确保后续 Redis Lua 实现具备安全释放锁所需的最小信息。
     */
    public DistributedLockCommand {
        lockKey = Check.notBlank("lockKey", lockKey);
        ownerToken = Check.notBlank("ownerToken", ownerToken);
        if (waitTime == null || waitTime.isNegative()) {
            throw new IllegalArgumentException("waitTime must not be negative");
        }
        if (leaseTime == null || leaseTime.isZero() || leaseTime.isNegative()) {
            throw new IllegalArgumentException("leaseTime must be positive");
        }
        if (watchdogOptions == null) {
            watchdogOptions = DistributedLockWatchdogOptions.disabled();
        }
    }
}
