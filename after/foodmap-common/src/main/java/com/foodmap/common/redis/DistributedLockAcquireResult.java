package com.foodmap.common.redis;

/**
 * 分布式锁尝试获取结果，用于不希望直接抛异常的并发保护场景。
 *
 * <p>业务代码可以根据 locked 判断是否进入临界区；未获得锁时应返回稍后重试或幂等结果。</p>
 *
 * @param locked 是否成功获得锁。
 * @param token 锁持有凭证，只有 locked 为 true 时不为空。
 * @param message 未获得锁时的可排查提示。
 */
public record DistributedLockAcquireResult(
        boolean locked,
        DistributedLockToken token,
        String message
) {

    /**
     * 创建成功获取锁的结果。
     *
     * @param token 锁持有凭证。
     * @return 成功结果。
     */
    public static DistributedLockAcquireResult locked(DistributedLockToken token) {
        if (token == null) {
            throw new IllegalArgumentException("token must not be null when lock is acquired");
        }
        return new DistributedLockAcquireResult(true, token, "locked");
    }

    /**
     * 创建未获得锁的结果。
     *
     * @param message 未获得锁的提示。
     * @return 未获得锁结果。
     */
    public static DistributedLockAcquireResult notLocked(String message) {
        return new DistributedLockAcquireResult(false, null, message == null || message.isBlank() ? "lock not acquired" : message);
    }
}
