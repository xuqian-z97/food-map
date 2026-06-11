package com.foodmap.common.redis;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DistributedLockCommandTest {

    @Test
    void shouldCreateLockCommand() {
        DistributedLockCommand command = new DistributedLockCommand(
                "foodmap:auth:lock:v1:account-100",
                "owner-token",
                Duration.ZERO,
                Duration.ofSeconds(10));

        assertThat(command.lockKey()).isEqualTo("foodmap:auth:lock:v1:account-100");
        assertThat(command.ownerToken()).isEqualTo("owner-token");
        assertThat(command.waitTime()).isZero();
        assertThat(command.leaseTime()).isEqualTo(Duration.ofSeconds(10));
        assertThat(command.watchdogOptions().enabled()).isFalse();
    }

    @Test
    void shouldCreateLockCommandWithWatchdog() {
        DistributedLockWatchdogOptions watchdogOptions = DistributedLockWatchdogOptions.enabled(
                Duration.ofSeconds(3),
                Duration.ofSeconds(10),
                5);

        DistributedLockCommand command = new DistributedLockCommand(
                "foodmap:auth:lock:v1:account-100",
                "owner-token",
                Duration.ZERO,
                Duration.ofSeconds(10),
                watchdogOptions);

        assertThat(command.watchdogOptions().enabled()).isTrue();
        assertThat(command.watchdogOptions().renewInterval()).isEqualTo(Duration.ofSeconds(3));
        assertThat(command.watchdogOptions().renewLeaseTime()).isEqualTo(Duration.ofSeconds(10));
        assertThat(command.watchdogOptions().maxRenewCount()).isEqualTo(5);
    }

    @Test
    void shouldRejectInvalidLeaseTime() {
        assertThatThrownBy(() -> new DistributedLockCommand(
                "foodmap:auth:lock:v1:account-100",
                "owner-token",
                Duration.ZERO,
                Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("leaseTime must be positive");
    }

    @Test
    void shouldRejectInvalidWatchdogOptions() {
        assertThatThrownBy(() -> DistributedLockWatchdogOptions.enabled(Duration.ZERO, Duration.ofSeconds(10), 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("renewInterval must be positive");
    }
}
