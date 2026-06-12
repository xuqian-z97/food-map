package com.foodmap.common.logging.mybatis;

import com.foodmap.common.logging.FoodMapLoggingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentSqlLogConfigProviderTest {

    @Test
    void shouldReadLatestSqlLoggingPropertiesFromEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        FoodMapLoggingProperties.Sql defaults = new FoodMapLoggingProperties.Sql();
        EnvironmentSqlLogConfigProvider provider = new EnvironmentSqlLogConfigProvider(environment, defaults);

        assertThat(provider.current().isDebugEnabled()).isFalse();

        environment.withProperty("foodmap.logging.sql.debug-enabled", "true")
                .withProperty("foodmap.logging.sql.slow-threshold-ms", "120")
                .withProperty("foodmap.logging.sql.mapper-includes[0]", "com.foodmap.auth.*")
                .withProperty("foodmap.logging.sql.request-ids[0]", "req-dynamic");

        FoodMapLoggingProperties.Sql current = provider.current();

        assertThat(current.isDebugEnabled()).isTrue();
        assertThat(current.getSlowThresholdMs()).isEqualTo(120L);
        assertThat(current.getMapperIncludes()).containsExactly("com.foodmap.auth.*");
        assertThat(current.getRequestIds()).containsExactly("req-dynamic");
    }

    @Test
    void shouldFallbackToDefaultsWhenEnvironmentHasNoSqlProperties() {
        MockEnvironment environment = new MockEnvironment();
        FoodMapLoggingProperties.Sql defaults = new FoodMapLoggingProperties.Sql();
        defaults.setSlowThresholdMs(700L);
        defaults.setMaxSqlLength(900);
        EnvironmentSqlLogConfigProvider provider = new EnvironmentSqlLogConfigProvider(environment, defaults);

        FoodMapLoggingProperties.Sql current = provider.current();

        assertThat(current.getSlowThresholdMs()).isEqualTo(700L);
        assertThat(current.getMaxSqlLength()).isEqualTo(900);
    }
}
