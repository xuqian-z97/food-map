package com.foodmap.common.redis.redisson;

import com.foodmap.common.redis.DistributedLockCommand;
import com.foodmap.common.redis.DistributedLockException;
import com.foodmap.common.redis.DistributedLockToken;
import com.foodmap.common.redis.DistributedLockWatchdogHandle;
import com.foodmap.common.redis.DistributedLockWatchdogOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedissonDistributedLockClientTest {

    private final RedissonClient redissonClient = mock(RedissonClient.class);

    private final RBucket<Object> bucket = mock(RBucket.class);

    private final RScript script = mock(RScript.class);

    private RedissonDistributedLockClient lockClient;

    @AfterEach
    void tearDown() {
        if (lockClient != null) {
            lockClient.shutdown();
        }
    }

    @Test
    void acquireShouldReturnTokenWhenRedisSetNxSucceeds() {
        when(redissonClient.getBucket("foodmap:auth:lock:v1:phone:hash", StringCodec.INSTANCE)).thenReturn(bucket);
        when(bucket.trySet("owner-1", 5000L, TimeUnit.MILLISECONDS)).thenReturn(true);
        lockClient = new RedissonDistributedLockClient(redissonClient);

        DistributedLockToken token = lockClient.acquire(command(Duration.ZERO, Duration.ofSeconds(5)));

        assertThat(token.lockKey()).isEqualTo("foodmap:auth:lock:v1:phone:hash");
        assertThat(token.ownerToken()).isEqualTo("owner-1");
        assertThat(token.leaseTime()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void acquireShouldThrowWhenLockIsHeldByAnotherOwner() {
        when(redissonClient.getBucket("foodmap:auth:lock:v1:phone:hash", StringCodec.INSTANCE)).thenReturn(bucket);
        when(bucket.trySet("owner-1", 5000L, TimeUnit.MILLISECONDS)).thenReturn(false);
        lockClient = new RedissonDistributedLockClient(redissonClient);

        assertThatThrownBy(() -> lockClient.acquire(command(Duration.ZERO, Duration.ofSeconds(5))))
                .isInstanceOf(DistributedLockException.class)
                .hasMessageContaining("分布式锁已被其他请求持有");
    }

    @Test
    void acquireShouldWrapRedissonFailureAsServiceUnavailable() {
        when(redissonClient.getBucket("foodmap:auth:lock:v1:phone:hash", StringCodec.INSTANCE)).thenReturn(bucket);
        when(bucket.trySet("owner-1", 5000L, TimeUnit.MILLISECONDS)).thenThrow(new IllegalStateException("redis down"));
        lockClient = new RedissonDistributedLockClient(redissonClient);

        assertThatThrownBy(() -> lockClient.acquire(command(Duration.ZERO, Duration.ofSeconds(5))))
                .isInstanceOf(DistributedLockException.class)
                .hasMessageContaining("分布式锁服务暂时不可用");
    }

    @Test
    void releaseShouldUseTokenCheckedLuaScript() {
        when(redissonClient.getScript(StringCodec.INSTANCE)).thenReturn(script);
        when(script.eval(
                eq(RScript.Mode.READ_WRITE),
                eq(releaseScript()),
                eq(RScript.ReturnType.BOOLEAN),
                anyList(),
                eq("owner-1"))).thenReturn(true);
        lockClient = new RedissonDistributedLockClient(redissonClient);

        lockClient.release(token(Duration.ofSeconds(5)));

        verify(script).eval(
                eq(RScript.Mode.READ_WRITE),
                eq(releaseScript()),
                eq(RScript.ReturnType.BOOLEAN),
                eq(List.of("foodmap:auth:lock:v1:phone:hash")),
                eq("owner-1"));
    }

    @Test
    void renewShouldUseTokenCheckedLuaScript() {
        when(redissonClient.getScript(StringCodec.INSTANCE)).thenReturn(script);
        when(script.eval(
                eq(RScript.Mode.READ_WRITE),
                eq(renewScript()),
                eq(RScript.ReturnType.BOOLEAN),
                anyList(),
                eq("owner-1"),
                eq("5000"))).thenReturn(true);
        lockClient = new RedissonDistributedLockClient(redissonClient);

        boolean renewed = lockClient.renew(token(Duration.ofSeconds(5)));

        assertThat(renewed).isTrue();
        verify(script).eval(
                eq(RScript.Mode.READ_WRITE),
                eq(renewScript()),
                eq(RScript.ReturnType.BOOLEAN),
                eq(List.of("foodmap:auth:lock:v1:phone:hash")),
                eq("owner-1"),
                eq("5000"));
    }

    @Test
    void startWatchdogShouldRejectDisabledOptions() {
        lockClient = new RedissonDistributedLockClient(redissonClient);

        assertThatThrownBy(() -> lockClient.startWatchdog(token(Duration.ofSeconds(5)), DistributedLockWatchdogOptions.disabled()))
                .isInstanceOf(DistributedLockException.class)
                .hasMessageContaining("分布式锁看门狗未启用");
    }

    @Test
    void startAndStopWatchdogShouldReturnHandle() {
        lockClient = new RedissonDistributedLockClient(redissonClient);

        DistributedLockWatchdogHandle handle = lockClient.startWatchdog(
                token(Duration.ofSeconds(5)),
                DistributedLockWatchdogOptions.enabled(Duration.ofSeconds(1), Duration.ofSeconds(5), 1));
        lockClient.stopWatchdog(handle);

        assertThat(handle.lockKey()).isEqualTo("foodmap:auth:lock:v1:phone:hash");
        assertThat(handle.ownerToken()).isEqualTo("owner-1");
    }

    /**
     * 创建测试用分布式锁命令。
     *
     * @param waitTime 等待获取锁时间。
     * @param leaseTime 锁租约时间。
     * @return 分布式锁命令。
     */
    private DistributedLockCommand command(Duration waitTime, Duration leaseTime) {
        return new DistributedLockCommand(
                "foodmap:auth:lock:v1:phone:hash",
                "owner-1",
                waitTime,
                leaseTime);
    }

    /**
     * 创建测试用分布式锁凭证。
     *
     * @param leaseTime 锁租约时间。
     * @return 分布式锁凭证。
     */
    private DistributedLockToken token(Duration leaseTime) {
        return new DistributedLockToken(
                "foodmap:auth:lock:v1:phone:hash",
                "owner-1",
                OffsetDateTime.now(),
                leaseTime);
    }

    /**
     * 返回与生产代码一致的释放锁脚本，确保测试校验 owner token 释放语义。
     *
     * @return 释放锁脚本。
     */
    private String releaseScript() {
        return """
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('del', KEYS[1])
                end
                return 0
                """;
    }

    /**
     * 返回与生产代码一致的续期脚本，确保测试校验 owner token 续期语义。
     *
     * @return 续期脚本。
     */
    private String renewScript() {
        return """
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('pexpire', KEYS[1], ARGV[2])
                end
                return 0
                """;
    }
}
