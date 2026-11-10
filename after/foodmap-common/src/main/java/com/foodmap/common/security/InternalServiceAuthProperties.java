package com.foodmap.common.security;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 内部服务身份配置，承载共享密钥、时间窗和 nonce 有效期。
 *
 * <p>两个开关的职责必须区分清楚：</p>
 * <ul>
 *   <li>{@code enabled}：本服务作为调用方，为出站内部请求签名。</li>
 *   <li>{@code verify-enabled}：本服务作为被调方，校验入站内部请求。</li>
 * </ul>
 *
 * <p>生产环境必须通过 {@code FOODMAP_INTERNAL_SECRET} 注入密钥，缺失或长度不足时启动失败；
 * local/orbstack 的开发默认值只写在 profile 配置里，本类不提供默认密钥。</p>
 */
@ConfigurationProperties(prefix = "foodmap.internal")
public class InternalServiceAuthProperties implements InitializingBean {

    /**
     * 内部服务共享密钥最小长度，低于该长度的 HMAC 密钥抗暴力破解能力不足。
     */
    private static final int MIN_SECRET_LENGTH = 32;

    /**
     * 是否启用出站请求签名。
     */
    private boolean enabled;

    /**
     * 是否启用入站请求校验。
     */
    private boolean verifyEnabled;

    /**
     * 内部服务共享密钥，调用方和服务端必须一致。
     */
    private String secret;

    /**
     * 允许的客户端与服务端时间差，单位秒，超过该窗口的请求按过期拒绝。
     */
    private long maxClockSkewSeconds = 300;

    /**
     * nonce 在 Redis 中的保留时间，单位秒，必须大于时间窗，避免窗口内重放漏判。
     */
    private long nonceTtlSeconds = 600;

    /**
     * 配置绑定完成后校验密钥；两个开关都关闭时不要求密钥。
     */
    @Override
    public void afterPropertiesSet() {
        validate();
    }

    /**
     * 校验启用内部服务身份时密钥已配置且长度达标。
     *
     * <p>异常信息不输出密钥内容，只说明缺失或长度不足。</p>
     */
    public void validate() {
        if (!enabled && !verifyEnabled) {
            return;
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("foodmap.internal.secret 未配置：必须通过环境变量 FOODMAP_INTERNAL_SECRET 注入");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("foodmap.internal.secret 长度不足：至少需要 " + MIN_SECRET_LENGTH
                    + " 字符；请检查环境变量 FOODMAP_INTERNAL_SECRET");
        }
        if (maxClockSkewSeconds <= 0) {
            throw new IllegalStateException("foodmap.internal.max-clock-skew-seconds 必须大于 0");
        }
        if (nonceTtlSeconds <= maxClockSkewSeconds) {
            throw new IllegalStateException("foodmap.internal.nonce-ttl-seconds 必须大于 max-clock-skew-seconds");
        }
    }

    /**
     * 判断是否启用出站请求签名。
     *
     * @return 启用时返回 true。
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用出站请求签名。
     *
     * @param enabled 是否启用。
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 判断是否启用入站请求校验。
     *
     * @return 启用时返回 true。
     */
    public boolean isVerifyEnabled() {
        return verifyEnabled;
    }

    /**
     * 设置是否启用入站请求校验。
     *
     * @param verifyEnabled 是否启用。
     */
    public void setVerifyEnabled(boolean verifyEnabled) {
        this.verifyEnabled = verifyEnabled;
    }

    /**
     * 获取内部服务共享密钥。
     *
     * @return 内部服务共享密钥。
     */
    public String getSecret() {
        return secret;
    }

    /**
     * 设置内部服务共享密钥。
     *
     * @param secret 内部服务共享密钥。
     */
    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * 获取允许的客户端与服务端时间差。
     *
     * @return 时间差，单位秒。
     */
    public long getMaxClockSkewSeconds() {
        return maxClockSkewSeconds;
    }

    /**
     * 设置允许的客户端与服务端时间差。
     *
     * @param maxClockSkewSeconds 时间差，单位秒。
     */
    public void setMaxClockSkewSeconds(long maxClockSkewSeconds) {
        this.maxClockSkewSeconds = maxClockSkewSeconds;
    }

    /**
     * 获取 nonce 保留时间。
     *
     * @return nonce 保留时间，单位秒。
     */
    public long getNonceTtlSeconds() {
        return nonceTtlSeconds;
    }

    /**
     * 设置 nonce 保留时间。
     *
     * @param nonceTtlSeconds nonce 保留时间，单位秒。
     */
    public void setNonceTtlSeconds(long nonceTtlSeconds) {
        this.nonceTtlSeconds = nonceTtlSeconds;
    }
}
