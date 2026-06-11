package com.foodmap.common.redis.redisson;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FoodMapRedissonPropertiesTest {

    @Test
    void defaultsShouldUseSmallPoolForMvpStage() {
        FoodMapRedissonProperties properties = new FoodMapRedissonProperties();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getAddress()).isEqualTo("redis://127.0.0.1:6379");
        assertThat(properties.getConnectionPoolSize()).isEqualTo(8);
        assertThat(properties.getConnectionMinimumIdleSize()).isEqualTo(1);
        assertThat(properties.getConnectTimeout()).isEqualTo(3000);
        assertThat(properties.getTimeout()).isEqualTo(2000);
        assertThat(properties.getRetryAttempts()).isEqualTo(2);
        assertThat(properties.getRetryInterval()).isEqualTo(500);
    }
}
