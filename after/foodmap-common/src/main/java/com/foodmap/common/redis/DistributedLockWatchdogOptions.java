package com.foodmap.common.redis;

import java.time.Duration;

/**
 * 分布式锁看门狗配置，用于业务临界区耗时不稳定时定期续期 Redis 锁。
 *
 * <p>看门狗只能作为长临界区保护手段，不能掩盖慢外部调用或缺失的状态机设计；必须设置最大续期次数避免无限占锁。</p>
 *
 * @param enabled 是否启用看门狗续期。
 * @param renewInterval 续期间隔，启用时必须大于零。
 * @param renewLeaseTime 每次续期写入的租约时间，启用时必须大于零。
 * @param maxRenewCount 最大续期次数，启用时必须大于零。
 */
public record DistributedLockWatchdogOptions(
        boolean enabled,
        Duration renewInterval,
        Duration renewLeaseTime,
        int maxRenewCount
) {

    /**
     * 创建禁用看门狗的配置，适合大多数短事务临界区。
     *
     * @return 禁用看门狗的配置。
     */
    public static DistributedLockWatchdogOptions disabled() {
        return new DistributedLockWatchdogOptions(false, Duration.ZERO, Duration.ZERO, 0);
    }

    /**
     * 创建启用看门狗的配置，适合执行时间有波动但必须串行的临界区。
     *
     * @param renewInterval 续期间隔。
     * @param renewLeaseTime 每次续期写入的租约时间。
     * @param maxRenewCount 最大续期次数。
     * @return 启用看门狗的配置。
     */
    public static DistributedLockWatchdogOptions enabled(Duration renewInterval, Duration renewLeaseTime, int maxRenewCount) {
        return new DistributedLockWatchdogOptions(true, renewInterval, renewLeaseTime, maxRenewCount);
    }

    /**
     * 校验看门狗配置，避免出现无限续期或零租约配置。
     */
    public DistributedLockWatchdogOptions {
        if (!enabled) {
            renewInterval = Duration.ZERO;
            renewLeaseTime = Duration.ZERO;
            maxRenewCount = 0;
        } else {
            if (renewInterval == null || renewInterval.isZero() || renewInterval.isNegative()) {
                throw new IllegalArgumentException("renewInterval must be positive when watchdog is enabled");
            }
            if (renewLeaseTime == null || renewLeaseTime.isZero() || renewLeaseTime.isNegative()) {
                throw new IllegalArgumentException("renewLeaseTime must be positive when watchdog is enabled");
            }
            if (maxRenewCount <= 0) {
                throw new IllegalArgumentException("maxRenewCount must be positive when watchdog is enabled");
            }
        }
    }
}
