package com.foodmap.common.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * FoodMap 日志基础能力配置项，控制服务名、访问日志开关和慢请求阈值。
 *
 * <p>B1.5-a 阶段默认开启 Servlet 访问摘要日志；如果某服务需要临时关闭，可配置
 * {@code foodmap.logging.access-log-enabled=false}。</p>
 */
@ConfigurationProperties(prefix = "foodmap.logging")
public class FoodMapLoggingProperties {
    /**
     * 日志上下文中的服务名，未配置时回退到 spring.application.name。
     */
    private String serviceName;
    /**
     * 是否启用 API 访问摘要日志。
     */
    private boolean accessLogEnabled = true;
    /**
     * 慢请求阈值，单位毫秒。
     */
    private long slowThresholdMs = 1000L;
    /**
     * MyBatis SQL 日志配置。
     */
    private Sql sql = new Sql();

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public boolean isAccessLogEnabled() {
        return accessLogEnabled;
    }

    public void setAccessLogEnabled(boolean accessLogEnabled) {
        this.accessLogEnabled = accessLogEnabled;
    }

    public long getSlowThresholdMs() {
        return slowThresholdMs;
    }

    public void setSlowThresholdMs(long slowThresholdMs) {
        this.slowThresholdMs = slowThresholdMs;
    }

    public Sql getSql() {
        return sql;
    }

    public void setSql(Sql sql) {
        this.sql = sql;
    }

    /**
     * MyBatis SQL 日志配置项，B1.5-b 阶段先支持启动配置，后续接入 Nacos 动态刷新。
     */
    public static class Sql {
        /**
         * 是否注册 SQL 日志拦截器。
         */
        private boolean enabled = true;
        /**
         * 是否输出全量 SQL DEBUG 明细，生产默认关闭。
         */
        private boolean debugEnabled = false;
        /**
         * 慢 SQL 阈值，单位毫秒。
         */
        private long slowThresholdMs = 500L;
        /**
         * DEBUG 采样率，范围 0 到 1。
         */
        private double sampleRate = 1.0D;
        /**
         * Mapper ID 白名单，支持 `*` 通配。
         */
        private List<String> mapperIncludes = new ArrayList<>();
        /**
         * Mapper ID 黑名单，支持 `*` 通配。
         */
        private List<String> mapperExcludes = new ArrayList<>();
        /**
         * 定向打开 SQL DEBUG 的 requestId 列表。
         */
        private List<String> requestIds = new ArrayList<>();
        /**
         * 定向打开 SQL DEBUG 的 traceId 列表。
         */
        private List<String> traceIds = new ArrayList<>();
        /**
         * SQL 输出最大长度，超出后截断。
         */
        private int maxSqlLength = 2000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isDebugEnabled() {
            return debugEnabled;
        }

        public void setDebugEnabled(boolean debugEnabled) {
            this.debugEnabled = debugEnabled;
        }

        public long getSlowThresholdMs() {
            return slowThresholdMs;
        }

        public void setSlowThresholdMs(long slowThresholdMs) {
            this.slowThresholdMs = slowThresholdMs;
        }

        public double getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(double sampleRate) {
            this.sampleRate = sampleRate;
        }

        public List<String> getMapperIncludes() {
            return mapperIncludes;
        }

        public void setMapperIncludes(List<String> mapperIncludes) {
            this.mapperIncludes = mapperIncludes;
        }

        public List<String> getMapperExcludes() {
            return mapperExcludes;
        }

        public void setMapperExcludes(List<String> mapperExcludes) {
            this.mapperExcludes = mapperExcludes;
        }

        public List<String> getRequestIds() {
            return requestIds;
        }

        public void setRequestIds(List<String> requestIds) {
            this.requestIds = requestIds;
        }

        public List<String> getTraceIds() {
            return traceIds;
        }

        public void setTraceIds(List<String> traceIds) {
            this.traceIds = traceIds;
        }

        public int getMaxSqlLength() {
            return maxSqlLength;
        }

        public void setMaxSqlLength(int maxSqlLength) {
            this.maxSqlLength = maxSqlLength;
        }
    }
}
