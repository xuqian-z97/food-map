package com.foodmap.common.redis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DistributedLockKeyBuilderTest {

    @Test
    void shouldBuildUnifiedLockKey() {
        String key = DistributedLockKeyBuilder.builder()
                .service("recommendation")
                .version("v1")
                .bizKey("store-100-dish-hash")
                .build();

        assertThat(key).isEqualTo("foodmap:recommendation:lock:v1:store-100-dish-hash");
    }

    @Test
    void shouldRejectColonInBizKey() {
        DistributedLockKeyBuilder builder = DistributedLockKeyBuilder.builder()
                .service("recommendation")
                .version("v1")
                .bizKey("store:100");

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key must not contain ':'");
    }
}
