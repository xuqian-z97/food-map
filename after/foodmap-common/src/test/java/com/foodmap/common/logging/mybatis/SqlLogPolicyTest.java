package com.foodmap.common.logging.mybatis;

import com.foodmap.common.logging.FoodMapLoggingProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SqlLogPolicyTest {

    @Test
    void shouldOnlyWriteDebugWhenEnabledAndMapperAllowed() {
        FoodMapLoggingProperties.Sql properties = new FoodMapLoggingProperties.Sql();
        properties.setDebugEnabled(true);
        properties.setMapperIncludes(List.of("com.foodmap.auth.*"));
        properties.setMapperExcludes(List.of("*.AuthBusinessIdMapper.*"));
        SqlLogPolicy policy = new SqlLogPolicy(properties);

        assertThat(policy.shouldWriteDebug(
                "com.foodmap.auth.infrastructure.persistence.mapper.AuthAccountMapper.selectByBizId",
                null,
                null
        )).isTrue();
        assertThat(policy.shouldWriteDebug(
                "com.foodmap.auth.infrastructure.persistence.mapper.AuthBusinessIdMapper.nextAccountId",
                null,
                null
        )).isFalse();
        assertThat(policy.shouldWriteDebug(
                "com.foodmap.user.infrastructure.persistence.mapper.UserMapper.selectByBizId",
                null,
                null
        )).isFalse();
    }

    @Test
    void shouldAllowRequestOrTraceTargetingAndDetectSlowSql() {
        FoodMapLoggingProperties.Sql properties = new FoodMapLoggingProperties.Sql();
        properties.setDebugEnabled(false);
        properties.setRequestIds(List.of("req-b15b"));
        properties.setTraceIds(List.of("trace-b15b"));
        properties.setSlowThresholdMs(300L);
        SqlLogPolicy policy = new SqlLogPolicy(properties);

        assertThat(policy.shouldWriteDebug("mapper.any", "req-b15b", null)).isTrue();
        assertThat(policy.shouldWriteDebug("mapper.any", null, "trace-b15b")).isTrue();
        assertThat(policy.shouldWriteDebug("mapper.any", "other", "other")).isFalse();
        assertThat(policy.isSlow(301L)).isTrue();
        assertThat(policy.isSlow(299L)).isFalse();
    }
}
