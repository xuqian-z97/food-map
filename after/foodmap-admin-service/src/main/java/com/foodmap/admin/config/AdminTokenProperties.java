package com.foodmap.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 后台 Access Token 配置，签发与验签两端读取同一前缀 `foodmap.admin.token`。
 *
 * <p>公钥位置两端共用；私钥位置与有效期只有管理后台服务读取。生产环境密钥由部署配置注入外部文件，
 * 缺失或无法解析时拒绝启动。</p>
 */
@ConfigurationProperties(prefix = "foodmap.admin.token")
public class AdminTokenProperties {
    /**
     * RSA 公钥资源位置，例如 classpath:keys/admin-token-dev-public.pem 或 file:/etc/foodmap/...。
     */
    private String publicKeyLocation;
    /**
     * RSA 私钥资源位置，只有管理后台服务允许配置。
     */
    private String privateKeyLocation;
    /**
     * 后台 Access Token 有效期，默认 15 分钟。
     */
    private Duration ttl = Duration.ofMinutes(15);

    public String getPublicKeyLocation() {
        return publicKeyLocation;
    }

    public void setPublicKeyLocation(String publicKeyLocation) {
        this.publicKeyLocation = publicKeyLocation;
    }

    public String getPrivateKeyLocation() {
        return privateKeyLocation;
    }

    public void setPrivateKeyLocation(String privateKeyLocation) {
        this.privateKeyLocation = privateKeyLocation;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }
}
