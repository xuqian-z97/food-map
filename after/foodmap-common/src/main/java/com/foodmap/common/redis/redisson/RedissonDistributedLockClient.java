package com.foodmap.common.redis.redisson;

import com.foodmap.common.redis.DistributedLockClient;
import com.foodmap.common.redis.DistributedLockCommand;
import com.foodmap.common.redis.DistributedLockException;
import com.foodmap.common.redis.DistributedLockToken;
import com.foodmap.common.redis.DistributedLockWatchdogHandle;
import com.foodmap.common.redis.DistributedLockWatchdogOptions;
import com.foodmap.common.exception.CommonErrorCode;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于 Redisson 的 FoodMap 分布式锁实现。
 *
 * <p>本类不向业务层暴露 Redisson API，而是用 Redisson 执行 Redis 原子命令，保留 FoodMap owner token、租约和看门狗最大续期次数语义。</p>
 */
public class RedissonDistributedLockClient implements DistributedLockClient {

    /**
     * 原子释放锁脚本，只允许锁持有人删除自己的锁。
     */
    private static final String RELEASE_SCRIPT = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """;

    /**
     * 原子续期脚本，只允许锁持有人延长自己的锁 TTL。
     */
    private static final String RENEW_SCRIPT = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('pexpire', KEYS[1], ARGV[2])
            end
            return 0
            """;

    /**
     * 未获取锁时的短暂等待间隔，避免等待锁时出现忙等。
     */
    private static final long RETRY_SLEEP_MILLIS = 50L;

    private final RedissonClient redissonClient;

    private final ScheduledExecutorService watchdogExecutor;

    private final Map<String, ScheduledFuture<?>> watchdogTasks = new ConcurrentHashMap<>();

    /**
     * 创建 Redisson 分布式锁客户端。
     *
     * @param redissonClient Redisson 客户端。
     */
    public RedissonDistributedLockClient(RedissonClient redissonClient) {
        if (redissonClient == null) {
            throw new IllegalArgumentException("redissonClient must not be null");
        }
        this.redissonClient = redissonClient;
        this.watchdogExecutor = Executors.newScheduledThreadPool(1, new WatchdogThreadFactory());
    }

    /**
     * 获取分布式锁，支持按命令中的等待时间短轮询等待。
     *
     * @param command 分布式锁获取命令。
     * @return 锁持有凭证。
     */
    @Override
    public DistributedLockToken acquire(DistributedLockCommand command) {
        long deadlineMillis = System.currentTimeMillis() + command.waitTime().toMillis();
        do {
            if (trySetLock(command)) {
                return new DistributedLockToken(
                        command.lockKey(),
                        command.ownerToken(),
                        OffsetDateTime.now(),
                        command.leaseTime());
            }
            if (command.waitTime().isZero()) {
                break;
            }
            sleepBeforeRetry(deadlineMillis);
        } while (System.currentTimeMillis() <= deadlineMillis);
        throw new DistributedLockException("分布式锁已被其他请求持有，请稍后重试");
    }

    /**
     * 续期分布式锁，使用当前锁凭证中的租约时间。
     *
     * @param token 锁持有凭证。
     * @return 是否续期成功。
     */
    @Override
    public boolean renew(DistributedLockToken token) {
        return renew(token, token.leaseTime());
    }

    /**
     * 释放分布式锁，只有 owner token 匹配时才会删除 Redis Key。
     *
     * @param token 锁持有凭证。
     */
    @Override
    public void release(DistributedLockToken token) {
        stopWatchdog(new DistributedLockWatchdogHandle(token.lockKey(), token.ownerToken(), OffsetDateTime.now()));
        try {
            redissonClient.getScript(StringCodec.INSTANCE).eval(
                    RScript.Mode.READ_WRITE,
                    RELEASE_SCRIPT,
                    RScript.ReturnType.BOOLEAN,
                    Collections.singletonList(token.lockKey()),
                    token.ownerToken());
        } catch (RuntimeException ex) {
            throw unavailable(ex);
        }
    }

    /**
     * 启动有限次数的看门狗续期任务。
     *
     * @param token 锁持有凭证。
     * @param options 看门狗续期配置。
     * @return 看门狗句柄。
     */
    @Override
    public DistributedLockWatchdogHandle startWatchdog(DistributedLockToken token, DistributedLockWatchdogOptions options) {
        if (options == null || !options.enabled()) {
            throw new DistributedLockException("分布式锁看门狗未启用");
        }
        String taskKey = watchdogTaskKey(token.lockKey(), token.ownerToken());
        AtomicInteger renewCount = new AtomicInteger(0);
        ScheduledFuture<?> future = watchdogExecutor.scheduleAtFixedRate(
                () -> renewWithLimit(token, options, renewCount),
                options.renewInterval().toMillis(),
                options.renewInterval().toMillis(),
                TimeUnit.MILLISECONDS);
        ScheduledFuture<?> oldFuture = watchdogTasks.putIfAbsent(taskKey, future);
        if (oldFuture != null) {
            future.cancel(false);
            throw new DistributedLockException("分布式锁看门狗已经启动");
        }
        return new DistributedLockWatchdogHandle(token.lockKey(), token.ownerToken(), OffsetDateTime.now());
    }

    /**
     * 停止看门狗续期任务。
     *
     * @param handle 看门狗句柄。
     */
    @Override
    public void stopWatchdog(DistributedLockWatchdogHandle handle) {
        if (handle == null) {
            return;
        }
        ScheduledFuture<?> future = watchdogTasks.remove(watchdogTaskKey(handle.lockKey(), handle.ownerToken()));
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * 关闭看门狗线程池，由 Spring Bean 销毁阶段调用。
     */
    public void shutdown() {
        watchdogTasks.values().forEach(future -> future.cancel(false));
        watchdogTasks.clear();
        watchdogExecutor.shutdownNow();
    }

    /**
     * 尝试写入锁 Key，成功时 Redis 自动按租约过期。
     *
     * @param command 分布式锁获取命令。
     * @return 是否写入成功。
     */
    private boolean trySetLock(DistributedLockCommand command) {
        try {
            RBucket<String> bucket = redissonClient.getBucket(command.lockKey(), StringCodec.INSTANCE);
            return bucket.trySet(command.ownerToken(), command.leaseTime().toMillis(), TimeUnit.MILLISECONDS);
        } catch (RuntimeException ex) {
            throw unavailable(ex);
        }
    }

    /**
     * 按指定租约续期分布式锁。
     *
     * @param token 锁持有凭证。
     * @param leaseTime 本次续期租约。
     * @return 是否续期成功。
     */
    private boolean renew(DistributedLockToken token, Duration leaseTime) {
        try {
            return redissonClient.getScript(StringCodec.INSTANCE).eval(
                    RScript.Mode.READ_WRITE,
                    RENEW_SCRIPT,
                    RScript.ReturnType.BOOLEAN,
                    Collections.singletonList(token.lockKey()),
                    token.ownerToken(),
                    String.valueOf(leaseTime.toMillis()));
        } catch (RuntimeException ex) {
            throw unavailable(ex);
        }
    }

    /**
     * 执行一次有限看门狗续期，达到最大次数或续期失败时停止任务。
     *
     * @param token 锁持有凭证。
     * @param options 看门狗续期配置。
     * @param renewCount 已续期次数计数器。
     */
    private void renewWithLimit(DistributedLockToken token, DistributedLockWatchdogOptions options, AtomicInteger renewCount) {
        int currentCount = renewCount.incrementAndGet();
        boolean shouldStop;
        try {
            shouldStop = currentCount > options.maxRenewCount() || !renew(token, options.renewLeaseTime());
        } catch (DistributedLockException ex) {
            shouldStop = true;
        }
        if (shouldStop) {
            stopWatchdog(new DistributedLockWatchdogHandle(token.lockKey(), token.ownerToken(), OffsetDateTime.now()));
        }
    }

    /**
     * 在下一次重试前短暂休眠，并在等待时间耗尽时直接返回。
     *
     * @param deadlineMillis 等待锁的截止时间戳。
     */
    private void sleepBeforeRetry(long deadlineMillis) {
        long remainingMillis = deadlineMillis - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(Math.min(RETRY_SLEEP_MILLIS, remainingMillis));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new DistributedLockException("等待分布式锁时线程被中断");
        }
    }

    /**
     * 生成看门狗任务索引 Key。
     *
     * @param lockKey Redis 锁 Key。
     * @param ownerToken 锁持有人令牌。
     * @return 看门狗任务索引 Key。
     */
    private String watchdogTaskKey(String lockKey, String ownerToken) {
        return lockKey + "|" + ownerToken;
    }

    /**
     * 将 Redisson 底层异常包装为稳定业务异常，避免客户端细节穿透业务层。
     *
     * @param cause Redisson 原始异常。
     * @return 分布式锁不可用异常。
     */
    private DistributedLockException unavailable(RuntimeException cause) {
        return new DistributedLockException(
                CommonErrorCode.SERVICE_UNAVAILABLE,
                "分布式锁服务暂时不可用，请稍后重试",
                cause);
    }

    /**
     * 看门狗线程工厂，便于排查锁续期线程来源。
     */
    private static final class WatchdogThreadFactory implements ThreadFactory {

        private final AtomicInteger threadIndex = new AtomicInteger(1);

        /**
         * 创建看门狗续期线程。
         *
         * @param runnable 线程执行逻辑。
         * @return 看门狗线程。
         */
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "foodmap-redisson-lock-watchdog-" + threadIndex.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
