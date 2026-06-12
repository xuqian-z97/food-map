package com.foodmap.common.logging.mybatis;

import com.foodmap.common.logging.FoodMapLoggingProperties;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * SQL 日志输出策略，集中判断 DEBUG 开关、Mapper 范围、定向 requestId/traceId 和慢 SQL。
 */
public class SqlLogPolicy {
    private final FoodMapLoggingProperties.Sql properties;

    /**
     * 创建 SQL 日志策略。
     *
     * @param properties SQL 日志配置。
     */
    public SqlLogPolicy(FoodMapLoggingProperties.Sql properties) {
        this.properties = properties;
    }

    /**
     * 判断当前 SQL 是否应输出 DEBUG 明细。
     *
     * @param mapperId MyBatis Mapper 方法 ID。
     * @param requestId 当前请求流水号。
     * @param traceId 当前链路号。
     * @return 需要输出 DEBUG 明细时返回 true。
     */
    public boolean shouldWriteDebug(String mapperId, String requestId, String traceId) {
        if (matchesAny(properties.getRequestIds(), requestId) || matchesAny(properties.getTraceIds(), traceId)) {
            return mapperAllowed(mapperId);
        }
        if (!properties.isDebugEnabled()) {
            return false;
        }
        return mapperAllowed(mapperId) && sampled();
    }

    /**
     * 判断执行耗时是否达到慢 SQL 阈值。
     *
     * @param durationMs SQL 执行耗时，单位毫秒。
     * @return 达到慢 SQL 阈值时返回 true。
     */
    public boolean isSlow(long durationMs) {
        return durationMs >= properties.getSlowThresholdMs();
    }

    private boolean mapperAllowed(String mapperId) {
        if (matchesAny(properties.getMapperExcludes(), mapperId)) {
            return false;
        }
        List<String> includes = properties.getMapperIncludes();
        return includes == null || includes.isEmpty() || matchesAny(includes, mapperId);
    }

    private boolean sampled() {
        double sampleRate = properties.getSampleRate();
        if (sampleRate >= 1.0D) {
            return true;
        }
        if (sampleRate <= 0.0D) {
            return false;
        }
        return ThreadLocalRandom.current().nextDouble() < sampleRate;
    }

    private boolean matchesAny(List<String> patterns, String value) {
        if (patterns == null || patterns.isEmpty() || value == null || value.isBlank()) {
            return false;
        }
        return patterns.stream().anyMatch(pattern -> matches(pattern, value));
    }

    private boolean matches(String pattern, String value) {
        if (pattern == null || pattern.isBlank()) {
            return false;
        }
        if ("*".equals(pattern)) {
            return true;
        }
        boolean startsWithWildcard = pattern.startsWith("*");
        boolean endsWithWildcard = pattern.endsWith("*");
        String body = pattern;
        if (startsWithWildcard) {
            body = body.substring(1);
        }
        if (endsWithWildcard && !body.isEmpty()) {
            body = body.substring(0, body.length() - 1);
        }
        if (startsWithWildcard && endsWithWildcard) {
            return value.contains(body);
        }
        if (startsWithWildcard) {
            return value.endsWith(body);
        }
        if (endsWithWildcard) {
            return value.startsWith(body);
        }
        return value.equals(pattern);
    }
}
