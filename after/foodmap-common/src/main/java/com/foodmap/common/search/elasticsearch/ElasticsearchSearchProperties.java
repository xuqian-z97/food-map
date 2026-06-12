package com.foodmap.common.search.elasticsearch;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Elasticsearch 通用搜索客户端配置，统一管理地址、超时和认证信息。
 */
@ConfigurationProperties(prefix = "foodmap.search.elasticsearch")
public class ElasticsearchSearchProperties {
    /**
     * 是否启用通用 Elasticsearch SearchClient，默认关闭，避免未使用 ES 的服务误连。
     */
    private boolean enabled;
    /**
     * Elasticsearch HTTP 基础地址。
     */
    private String baseUrl = "http://127.0.0.1:9200";
    /**
     * HTTP 连接超时时间。
     */
    private Duration connectTimeout = Duration.ofSeconds(3);
    /**
     * HTTP 请求超时时间。
     */
    private Duration requestTimeout = Duration.ofSeconds(30);
    /**
     * Elasticsearch Basic Auth 用户名，为空时不发送 Basic Auth。
     */
    private String username;
    /**
     * Elasticsearch Basic Auth 密码，为空时不发送 Basic Auth。
     */
    private String password;
    /**
     * Elasticsearch API Key，配置后优先于 Basic Auth。
     */
    private String apiKey;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
