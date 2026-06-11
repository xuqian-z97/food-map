package com.foodmap.common.redis;

import java.util.function.Supplier;

/**
 * 分布式锁客户端接口，统一封装 Redis 锁获取、释放和临界区执行能力。
 *
 * <p>Redis 实现必须使用唯一 owner token、明确 lease time，并通过 Lua 脚本或等价机制保证释放锁时的原子校验。</p>
 */
public interface DistributedLockClient {

    /**
     * 获取分布式锁；获取失败时应抛出 {@link DistributedLockException} 或其子类。
     *
     * @param command 分布式锁获取命令。
     * @return 锁持有凭证。
     */
    DistributedLockToken acquire(DistributedLockCommand command);

    /**
     * 尝试获取分布式锁，不直接抛出并发冲突异常，适合接口返回“稍后重试”的场景。
     *
     * @param command 分布式锁获取命令。
     * @return 锁获取结果。
     */
    default DistributedLockAcquireResult tryAcquire(DistributedLockCommand command) {
        try {
            return DistributedLockAcquireResult.locked(acquire(command));
        } catch (DistributedLockException ex) {
            if (ex.status() >= 500) {
                throw ex;
            }
            return DistributedLockAcquireResult.notLocked(ex.getMessage());
        }
    }

    /**
     * 续期分布式锁；实现必须校验 owner token 后再延长 TTL。
     *
     * @param token 锁持有凭证。
     * @return 是否续期成功。
     */
    boolean renew(DistributedLockToken token);

    /**
     * 释放分布式锁；实现必须校验 owner token，不能直接按 Key 删除。
     *
     * @param token 锁持有凭证。
     */
    void release(DistributedLockToken token);

    /**
     * 启动分布式锁看门狗续期；实现必须按配置限制最大续期次数。
     *
     * @param token 锁持有凭证。
     * @param options 看门狗续期配置。
     * @return 看门狗句柄。
     */
    DistributedLockWatchdogHandle startWatchdog(DistributedLockToken token, DistributedLockWatchdogOptions options);

    /**
     * 停止分布式锁看门狗续期，业务完成或异常退出时必须调用。
     *
     * @param handle 看门狗句柄。
     */
    void stopWatchdog(DistributedLockWatchdogHandle handle);

    /**
     * 在分布式锁保护下执行有返回值的临界区，执行完成后释放锁。
     *
     * @param command 分布式锁获取命令。
     * @param supplier 需要在锁内执行的业务逻辑。
     * @param <T> 业务返回值类型。
     * @return 业务逻辑返回值。
     */
    default <T> T executeWithLock(DistributedLockCommand command, Supplier<T> supplier) {
        DistributedLockToken token = acquire(command);
        try {
            return supplier.get();
        } finally {
            release(token);
        }
    }

    /**
     * 在分布式锁保护下执行无返回值的临界区，执行完成后释放锁。
     *
     * @param command 分布式锁获取命令。
     * @param runnable 需要在锁内执行的业务逻辑。
     */
    default void executeWithLock(DistributedLockCommand command, Runnable runnable) {
        DistributedLockToken token = acquire(command);
        try {
            runnable.run();
        } finally {
            release(token);
        }
    }

    /**
     * 尝试在分布式锁保护下执行有返回值的临界区；未获得锁时执行 fallback。
     *
     * @param command 分布式锁获取命令。
     * @param supplier 获得锁后执行的业务逻辑。
     * @param fallback 未获得锁时执行的降级逻辑。
     * @param <T> 业务返回值类型。
     * @return 业务结果或降级结果。
     */
    default <T> T tryExecuteWithLock(DistributedLockCommand command, Supplier<T> supplier, Supplier<T> fallback) {
        DistributedLockAcquireResult result = tryAcquire(command);
        if (!result.locked()) {
            return fallback.get();
        }
        try {
            return supplier.get();
        } finally {
            release(result.token());
        }
    }

    /**
     * 尝试在分布式锁保护下执行无返回值的临界区。
     *
     * @param command 分布式锁获取命令。
     * @param runnable 获得锁后执行的业务逻辑。
     * @return 是否获得锁并执行了业务逻辑。
     */
    default boolean tryExecuteWithLock(DistributedLockCommand command, Runnable runnable) {
        DistributedLockAcquireResult result = tryAcquire(command);
        if (!result.locked()) {
            return false;
        }
        try {
            runnable.run();
            return true;
        } finally {
            release(result.token());
        }
    }

    /**
     * 在看门狗续期保护下执行有返回值的临界区，适合耗时不稳定但必须串行的业务。
     *
     * @param command 分布式锁获取命令，必须启用看门狗配置。
     * @param supplier 需要在锁内执行的业务逻辑。
     * @param <T> 业务返回值类型。
     * @return 业务逻辑返回值。
     */
    default <T> T executeWithWatchdog(DistributedLockCommand command, Supplier<T> supplier) {
        ensureWatchdogEnabled(command);
        DistributedLockToken token = acquire(command);
        DistributedLockWatchdogHandle handle = startWatchdog(token, command.watchdogOptions());
        try {
            return supplier.get();
        } finally {
            stopWatchdog(handle);
            release(token);
        }
    }

    /**
     * 在看门狗续期保护下执行无返回值的临界区，适合耗时不稳定但必须串行的业务。
     *
     * @param command 分布式锁获取命令，必须启用看门狗配置。
     * @param runnable 需要在锁内执行的业务逻辑。
     */
    default void executeWithWatchdog(DistributedLockCommand command, Runnable runnable) {
        ensureWatchdogEnabled(command);
        DistributedLockToken token = acquire(command);
        DistributedLockWatchdogHandle handle = startWatchdog(token, command.watchdogOptions());
        try {
            runnable.run();
        } finally {
            stopWatchdog(handle);
            release(token);
        }
    }

    private void ensureWatchdogEnabled(DistributedLockCommand command) {
        if (command == null || !command.watchdogOptions().enabled()) {
            throw new DistributedLockException("分布式锁看门狗未启用");
        }
    }
}
