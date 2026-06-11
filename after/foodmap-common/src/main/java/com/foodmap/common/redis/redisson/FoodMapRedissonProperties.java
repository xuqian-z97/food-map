package com.foodmap.common.redis.redisson;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FoodMap Redisson 客户端配置属性，只用于分布式锁基础设施适配器。
 *
 * <p>业务缓存仍优先使用 Spring Data Redis + Lettuce；排查 Redis 连接问题时需要同时核对本配置和 Lettuce pool 配置。</p>
 */
@ConfigurationProperties(prefix = "foodmap.redis.redisson")
public class FoodMapRedissonProperties {

    /**
     * 是否启用 Redisson 锁适配器，默认关闭以避免未配置 Redis 时服务启动即连接 Redis。
     */
    private boolean enabled;

    /**
     * Redis 单节点地址，必须包含协议，例如 redis://127.0.0.1:6379。
     */
    private String address = "redis://127.0.0.1:6379";

    /**
     * Redis 访问密码，空值表示不使用密码。
     */
    private String password;

    /**
     * Redis database 编号，默认使用 0。
     */
    private int database;

    /**
     * Redisson 连接池最大连接数，MVP 阶段默认保持小池化。
     */
    private int connectionPoolSize = 8;

    /**
     * Redisson 连接池最小空闲连接数。
     */
    private int connectionMinimumIdleSize = 1;

    /**
     * 空闲连接回收时间，单位毫秒。
     */
    private int idleConnectionTimeout = 10000;

    /**
     * 建立 Redis 连接的超时时间，单位毫秒。
     */
    private int connectTimeout = 3000;

    /**
     * Redis 命令执行超时时间，单位毫秒。
     */
    private int timeout = 2000;

    /**
     * Redis 命令失败后的重试次数。
     */
    private int retryAttempts = 2;

    /**
     * Redis 命令重试间隔，单位毫秒。
     */
    private int retryInterval = 500;

    /**
     * Redisson 业务线程数量，0 表示使用 Redisson 默认值。
     */
    private int threads;

    /**
     * Redisson Netty 线程数量，0 表示使用 Redisson 默认值。
     */
    private int nettyThreads;

    /**
     * Redis 客户端名称，排查 Redis 连接来源时使用。
     */
    private String clientName = "foodmap-redisson";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public int getConnectionPoolSize() {
        return connectionPoolSize;
    }

    public void setConnectionPoolSize(int connectionPoolSize) {
        this.connectionPoolSize = connectionPoolSize;
    }

    public int getConnectionMinimumIdleSize() {
        return connectionMinimumIdleSize;
    }

    public void setConnectionMinimumIdleSize(int connectionMinimumIdleSize) {
        this.connectionMinimumIdleSize = connectionMinimumIdleSize;
    }

    public int getIdleConnectionTimeout() {
        return idleConnectionTimeout;
    }

    public void setIdleConnectionTimeout(int idleConnectionTimeout) {
        this.idleConnectionTimeout = idleConnectionTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public int getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public int getNettyThreads() {
        return nettyThreads;
    }

    public void setNettyThreads(int nettyThreads) {
        this.nettyThreads = nettyThreads;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
}
