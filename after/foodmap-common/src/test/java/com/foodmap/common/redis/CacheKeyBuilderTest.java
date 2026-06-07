package com.foodmap.common.redis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CacheKeyBuilderTest {

    @Test
    void shouldBuildFoodMapCacheKeyWithServiceBizVersionAndKey() {
        String key = CacheKeyBuilder.builder()
                .service("recommendation")
                .biz("detail")
                .version("v1")
                .key("90001")
                .build();

        assertThat(key).isEqualTo("foodmap:recommendation:detail:v1:90001");
    }

    @Test
    void shouldRejectBlankCacheKeyPart() {
        assertThatThrownBy(() -> CacheKeyBuilder.builder()
                .service("recommendation")
                .biz(" ")
                .version("v1")
                .key("90001")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("biz");
    }
}
