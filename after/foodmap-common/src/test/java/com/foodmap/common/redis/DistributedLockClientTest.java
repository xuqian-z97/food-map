package com.foodmap.common.redis;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DistributedLockClientTest {

    @Test
    void shouldReleaseLockAfterSupplierExecution() {
        RecordingLockClient client = new RecordingLockClient();
        DistributedLockCommand command = command();

        String result = client.executeWithLock(command, () -> "done");

        assertThat(result).isEqualTo("done");
        assertThat(client.events).containsExactly("acquire", "release");
    }

    @Test
    void shouldReleaseLockAfterRunnableExecution() {
        RecordingLockClient client = new RecordingLockClient();
        DistributedLockCommand command = command();

        client.executeWithLock(command, () -> client.events.add("business"));

        assertThat(client.events).containsExactly("acquire", "business", "release");
    }

    @Test
    void shouldReturnFallbackWhenTryExecuteCannotAcquireLock() {
        RecordingLockClient client = new RecordingLockClient();
        client.acquireSuccess = false;

        String result = client.tryExecuteWithLock(command(), () -> "locked", () -> "fallback");

        assertThat(result).isEqualTo("fallback");
        assertThat(client.events).containsExactly("try-failed");
    }

    @Test
    void shouldRethrowInfrastructureFailureWhenTryAcquireFailsWithServerError() {
        RecordingLockClient client = new RecordingLockClient();
        client.infrastructureFailure = true;

        assertThatThrownBy(() -> client.tryAcquire(command()))
                .isInstanceOf(DistributedLockException.class)
                .hasMessageContaining("分布式锁服务暂时不可用");
    }

    @Test
    void shouldExecuteWithWatchdogAndStopRenewalBeforeRelease() {
        RecordingLockClient client = new RecordingLockClient();
        DistributedLockCommand command = new DistributedLockCommand(
                "foodmap:auth:lock:v1:account-100",
                "owner-token",
                Duration.ZERO,
                Duration.ofSeconds(10),
                DistributedLockWatchdogOptions.enabled(Duration.ofSeconds(3), Duration.ofSeconds(10), 5));

        String result = client.executeWithWatchdog(command, () -> "done");

        assertThat(result).isEqualTo("done");
        assertThat(client.events).containsExactly("acquire", "watchdog-start", "watchdog-stop", "release");
    }

    private DistributedLockCommand command() {
        return new DistributedLockCommand(
                "foodmap:auth:lock:v1:account-100",
                "owner-token",
                Duration.ZERO,
                Duration.ofSeconds(10));
    }

    private static final class RecordingLockClient implements DistributedLockClient {

        private final List<String> events = new ArrayList<>();
        private boolean acquireSuccess = true;
        private boolean infrastructureFailure;

        @Override
        public DistributedLockToken acquire(DistributedLockCommand command) {
            if (infrastructureFailure) {
                throw new DistributedLockException(
                        com.foodmap.common.exception.CommonErrorCode.SERVICE_UNAVAILABLE,
                        "分布式锁服务暂时不可用");
            }
            if (!acquireSuccess) {
                events.add("try-failed");
                throw new DistributedLockException("锁已被其他请求持有");
            }
            events.add("acquire");
            return new DistributedLockToken(
                    command.lockKey(),
                    command.ownerToken(),
                    OffsetDateTime.now(),
                    command.leaseTime());
        }

        @Override
        public boolean renew(DistributedLockToken token) {
            events.add("renew");
            return true;
        }

        @Override
        public void release(DistributedLockToken token) {
            events.add("release");
        }

        @Override
        public DistributedLockWatchdogHandle startWatchdog(DistributedLockToken token, DistributedLockWatchdogOptions options) {
            events.add("watchdog-start");
            return new DistributedLockWatchdogHandle(token.lockKey(), token.ownerToken(), OffsetDateTime.now());
        }

        @Override
        public void stopWatchdog(DistributedLockWatchdogHandle handle) {
            events.add("watchdog-stop");
        }
    }
}
