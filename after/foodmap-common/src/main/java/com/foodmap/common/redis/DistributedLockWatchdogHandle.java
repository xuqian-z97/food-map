package com.foodmap.common.redis;

import com.foodmap.common.validation.Check;

import java.time.OffsetDateTime;

/**
 * 分布式锁看门狗句柄，用于停止后台续期任务并排查续期生命周期。
 *
 * <p>具体 Redis 实现可以把调度任务 ID 或线程句柄放在基础设施层内部，本对象只暴露业务可观察的锁和启动时间。</p>
 *
 * @param lockKey 正在续期的锁 Key。
 * @param ownerToken 锁持有人唯一令牌。
 * @param startedAt 看门狗启动时间。
 */
public record DistributedLockWatchdogHandle(
        String lockKey,
        String ownerToken,
        OffsetDateTime startedAt
) {

    /**
     * 校验看门狗句柄，确保停止续期时仍可校验锁归属。
     */
    public DistributedLockWatchdogHandle {
        lockKey = Check.notBlank("lockKey", lockKey);
        ownerToken = Check.notBlank("ownerToken", ownerToken);
        if (startedAt == null) {
            throw new IllegalArgumentException("startedAt must not be null");
        }
    }
}
