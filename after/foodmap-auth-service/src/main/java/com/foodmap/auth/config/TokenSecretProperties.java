package com.foodmap.auth.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Token 签名密钥配置，统一承载认证服务签发 Access Token / Refresh Token 使用的 HMAC 密钥。
 *
 * <p>该配置与网关 {@code GatewayAuthFilter} 共用同一个配置键 {@code foodmap.security.token-secret}，
 * 生产环境必须通过环境变量 {@code FOODMAP_TOKEN_SECRET} 注入。配置缺失或长度不足时会在 Bean 初始化阶段
 * 直接抛出异常阻止服务启动，避免服务带着开发默认密钥对外运行。</p>
 *
 * <p>排查登录后立即 401 的问题时，应先确认认证服务和网关解析到的是同一个密钥，
 * 再确认密钥长度是否满足 {@value #MIN_SECRET_LENGTH} 字符下限。</p>
 */
@Component
@ConfigurationProperties(prefix = "foodmap.security")
public class TokenSecretProperties implements InitializingBean {

    /**
     * Token 密钥最小长度，低于该长度的 HMAC 密钥抗暴力破解能力不足，禁止启动。
     */
    private static final int MIN_SECRET_LENGTH = 32;

    /**
     * 配置键名，用于启动失败时明确指出需要检查的配置项。
     */
    private static final String TOKEN_SECRET_KEY = "foodmap.security.token-secret";

    /**
     * 生产环境必须注入的环境变量名，用于启动失败时给出可执行的修复提示。
     */
    private static final String TOKEN_SECRET_ENV = "FOODMAP_TOKEN_SECRET";

    /**
     * HMAC 签名密钥，由 {@code foodmap.security.token-secret} 绑定，认证服务与网关必须一致。
     */
    private String tokenSecret;

    /**
     * Bean 初始化回调，在配置绑定完成后立即校验密钥，校验失败直接阻止服务启动。
     */
    @Override
    public void afterPropertiesSet() {
        validate();
    }

    /**
     * 校验 Token 密钥已配置且长度满足下限，失败时抛出启动期异常。
     *
     * <p>异常信息不输出密钥内容，只说明缺失或长度不足，避免密钥进入日志。</p>
     */
    public void validate() {
        if (tokenSecret == null || tokenSecret.isBlank()) {
            throw new IllegalStateException(TOKEN_SECRET_KEY + " 未配置：必须通过环境变量 " + TOKEN_SECRET_ENV
                    + " 注入至少 " + MIN_SECRET_LENGTH + " 字符的 HMAC 签名密钥");
        }
        if (tokenSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(TOKEN_SECRET_KEY + " 长度不足：当前 " + tokenSecret.length()
                    + " 字符，至少需要 " + MIN_SECRET_LENGTH + " 字符；请检查环境变量 " + TOKEN_SECRET_ENV);
        }
    }

    /**
     * 获取 HMAC 签名密钥。
     *
     * @return 已绑定到配置的 Token 签名密钥。
     */
    public String getTokenSecret() {
        return tokenSecret;
    }

    /**
     * 绑定 HMAC 签名密钥。
     *
     * @param tokenSecret {@code foodmap.security.token-secret} 配置值。
     */
    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }
}
