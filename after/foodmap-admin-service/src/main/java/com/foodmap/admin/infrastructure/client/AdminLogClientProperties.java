package com.foodmap.admin.infrastructure.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 管理后台调用日志平台的客户端配置，集中管理日志服务地址和请求超时。
 */
@ConfigurationProperties(prefix = "foodmap.admin.log-service")
public class AdminLogClientProperties {
    /**
     * 日志平台服务基础地址，本地默认访问 8089 端口。
     */
    private String baseUrl = "http://127.0.0.1:8089";
    /**
     * 调用日志平台的请求超时时间。
     */
    private Duration requestTimeout = Duration.ofSeconds(5);

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }
}
