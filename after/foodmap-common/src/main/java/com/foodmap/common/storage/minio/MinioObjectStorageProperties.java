package com.foodmap.common.storage.minio;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * MinIO 对象存储客户端配置，集中管理连接地址、凭证和公开访问地址。
 */
@ConfigurationProperties(prefix = "foodmap.storage.minio")
public class MinioObjectStorageProperties {
    /**
     * 是否启用 MinIO ObjectStorageClient，默认关闭，避免未使用对象存储的服务误连。
     */
    private boolean enabled;
    /**
     * MinIO 服务地址。
     */
    private String endpoint = "http://127.0.0.1:9000";
    /**
     * MinIO Access Key。
     */
    private String accessKey;
    /**
     * MinIO Secret Key。
     */
    private String secretKey;
    /**
     * S3 签名区域，本地 MinIO 默认使用 us-east-1。
     */
    private String region = "us-east-1";
    /**
     * HTTP 连接超时时间。
     */
    private Duration connectTimeout = Duration.ofSeconds(3);
    /**
     * HTTP 请求超时时间。
     */
    private Duration requestTimeout = Duration.ofSeconds(60);
    /**
     * 公开访问地址基础路径，为空时上传结果不返回 publicUrl。
     */
    private String publicUrlBase;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
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

    public String getPublicUrlBase() {
        return publicUrlBase;
    }

    public void setPublicUrlBase(String publicUrlBase) {
        this.publicUrlBase = publicUrlBase;
    }
}
