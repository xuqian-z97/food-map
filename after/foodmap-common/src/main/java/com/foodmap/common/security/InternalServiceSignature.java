package com.foodmap.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 内部服务签名算法，调用方和服务端共用同一套规范化字符串和 HMAC 计算。
 *
 * <p>签名原文固定为六行，使用换行分隔，避免字段拼接产生歧义：</p>
 * <pre>
 * serviceName
 * timestampMillis
 * nonce
 * HTTP_METHOD
 * requestPath
 * sha256Hex(body)
 * </pre>
 *
 * <p>请求体只参与摘要，不在签名串中保存明文；空请求体的摘要固定为 SHA-256 空串。
 * 排查签名不一致时，先逐行比对这六段，再确认双方密钥是否相同。</p>
 */
public final class InternalServiceSignature {

    /**
     * HMAC 算法，调用方与服务端必须保持一致。
     */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * 签名结果使用 URL 安全 Base64 且不带填充，便于放入 HTTP 头。
     */
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    /**
     * 十六进制字符表，用于输出请求体摘要。
     */
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private InternalServiceSignature() {
    }

    /**
     * 计算内部服务调用签名。
     *
     * @param secret 内部服务共享密钥。
     * @param serviceName 调用方服务名。
     * @param timestampMillis 请求发起时间，Unix 毫秒时间戳。
     * @param nonce 一次性随机串。
     * @param method HTTP 方法。
     * @param path 请求路径，不包含查询字符串。
     * @param body 请求体字节，允许为 null 或空数组。
     * @return URL 安全 Base64 编码的 HMAC 签名。
     */
    public static String sign(
            String secret,
            String serviceName,
            long timestampMillis,
            String nonce,
            String method,
            String path,
            byte[] body
    ) {
        String canonical = canonicalString(serviceName, timestampMillis, nonce, method, path, body);
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return ENCODER.encodeToString(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("内部服务签名计算失败", ex);
        }
    }

    /**
     * 生成签名原文，便于调用方与服务端在排查时逐行比对。
     *
     * @param serviceName 调用方服务名。
     * @param timestampMillis 请求发起时间，Unix 毫秒时间戳。
     * @param nonce 一次性随机串。
     * @param method HTTP 方法。
     * @param path 请求路径，不包含查询字符串。
     * @param body 请求体字节，允许为 null 或空数组。
     * @return 六行签名原文。
     */
    public static String canonicalString(
            String serviceName,
            long timestampMillis,
            String nonce,
            String method,
            String path,
            byte[] body
    ) {
        return serviceName + "\n"
                + timestampMillis + "\n"
                + nonce + "\n"
                + method.toUpperCase(Locale.ROOT) + "\n"
                + path + "\n"
                + sha256Hex(body);
    }

    /**
     * 计算请求体的 SHA-256 十六进制摘要。
     *
     * <p>空请求体统一返回 SHA-256 空串摘要，保证 GET 请求也有稳定摘要参与签名。</p>
     *
     * @param body 请求体字节，允许为 null。
     * @return 小写十六进制摘要。
     */
    public static String sha256Hex(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(body == null ? new byte[0] : body);
            char[] chars = new char[hash.length * 2];
            for (int index = 0; index < hash.length; index++) {
                int value = hash[index] & 0xFF;
                chars[index * 2] = HEX_DIGITS[value >>> 4];
                chars[index * 2 + 1] = HEX_DIGITS[value & 0x0F];
            }
            return new String(chars);
        } catch (Exception ex) {
            throw new IllegalStateException("内部服务请求体摘要计算失败", ex);
        }
    }

    /**
     * 使用常量时间比较签名，避免通过响应时间差探测签名前缀。
     *
     * @param expected 服务端计算的签名。
     * @param actual 请求携带的签名。
     * @return 两个签名完全一致时返回 true。
     */
    public static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }
}
