package com.foodmap.common.security;

import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * 内部服务请求校验器，依次校验访问策略、时间窗、签名和 nonce 重放。
 *
 * <p>校验顺序为：请求头完整性 → 调用方白名单 → 服务名匹配 → 时间窗 → 签名 → nonce。
 * 缺少身份头统一 401，已登记路径但调用方不在白名单统一 403；白名单放在签名计算之前，
 * 未登记的内部接口不会进入 HMAC 计算。</p>
 *
 * <p>Redis 不可用时按 fail-closed 返回 401，不能把依赖故障降级成「放行」。日志只记录路径和
 * 服务名，不记录签名、nonce 明文或密钥。</p>
 */
public class InternalServiceRequestVerifier {

    /**
     * 日志组件，用于记录依赖故障，不记录敏感值。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalServiceRequestVerifier.class);

    /**
     * nonce 最大长度，防止超长请求头放大 Redis Key。
     */
    private static final int MAX_NONCE_LENGTH = 64;

    private final InternalServiceAuthProperties properties;
    private final InternalServiceNonceStore nonceStore;

    /**
     * 创建内部服务请求校验器。
     *
     * @param properties 内部服务身份配置。
     * @param nonceStore nonce 去重存储。
     */
    public InternalServiceRequestVerifier(
            InternalServiceAuthProperties properties,
            InternalServiceNonceStore nonceStore
    ) {
        if (properties == null) {
            throw new IllegalArgumentException("properties must not be null");
        }
        if (nonceStore == null) {
            throw new IllegalArgumentException("nonceStore must not be null");
        }
        this.properties = properties;
        this.nonceStore = nonceStore;
    }

    /**
     * 校验一次内部请求。
     *
     * @param serviceName 请求头中的调用方服务名。
     * @param timestamp 请求头中的 Unix 毫秒时间戳。
     * @param nonce 请求头中的一次性随机串。
     * @param signature 请求头中的 HMAC 签名。
     * @param method HTTP 方法。
     * @param path 请求路径，不包含查询字符串。
     * @param body 请求体字节，允许为 null 或空数组。
     */
    public void verify(
            String serviceName,
            String timestamp,
            String nonce,
            String signature,
            String method,
            String path,
            byte[] body
    ) {
        if (isBlank(serviceName) || isBlank(timestamp) || isBlank(nonce) || isBlank(signature)) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "缺少内部服务身份");
        }
        Optional<String> allowedService = InternalServiceAccessPolicy.allowedService(path);
        if (allowedService.isEmpty()) {
            throw new FoodMapException(CommonErrorCode.FORBIDDEN, "内部接口未登记调用方");
        }
        if (!allowedService.get().equals(serviceName)) {
            throw new FoodMapException(CommonErrorCode.FORBIDDEN, "内部接口不允许当前服务调用");
        }
        long timestampMillis = parseTimestamp(timestamp);
        long now = System.currentTimeMillis();
        long maxSkewMillis = properties.getMaxClockSkewSeconds() * 1000L;
        if (timestampMillis < now - maxSkewMillis || timestampMillis > now + maxSkewMillis) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "内部服务请求已过期");
        }
        if (nonce.length() > MAX_NONCE_LENGTH) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "内部服务 nonce 非法");
        }
        String expected = InternalServiceSignature.sign(
                properties.getSecret(), serviceName, timestampMillis, nonce, method, path, body);
        if (!InternalServiceSignature.constantTimeEquals(expected, signature)) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "内部服务签名无效");
        }
        boolean firstUse;
        try {
            firstUse = nonceStore.claim(serviceName, nonce, properties.getNonceTtlSeconds());
        } catch (RuntimeException ex) {
            LOGGER.warn("内部服务 nonce 存储不可用，按 fail-closed 拒绝 path={} service={}", path, serviceName);
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "内部服务身份校验暂不可用", ex);
        }
        if (!firstUse) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "内部服务请求重放");
        }
    }

    /**
     * 解析请求头中的毫秒时间戳。
     *
     * @param timestamp 时间戳文本。
     * @return 解析后的毫秒时间戳。
     */
    private long parseTimestamp(String timestamp) {
        try {
            long value = Long.parseLong(timestamp);
            if (value <= 0) {
                throw new NumberFormatException("timestamp must be positive");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "内部服务时间戳非法");
        }
    }

    /**
     * 判断请求头值是否为空。
     *
     * @param value 请求头值。
     * @return 为 null 或空白时返回 true。
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
