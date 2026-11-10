package com.foodmap.common.security;

import com.foodmap.common.validation.Check;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 内部服务请求签名器，统一生成四个签名请求头。
 *
 * <p>调用方只依赖本类生成请求头，不直接拼接 nonce、时间戳或签名原文，避免不同客户端出现格式漂移。
 * 密钥只在内存中使用，不会写入日志或响应。</p>
 */
public class InternalServiceSigner {

    private final String secret;
    private final String serviceName;

    /**
     * 创建内部服务请求签名器。
     *
     * @param secret 内部服务共享密钥。
     * @param serviceName 当前服务名，必须与 Nacos 注册名一致。
     */
    public InternalServiceSigner(String secret, String serviceName) {
        this.secret = Check.notBlank("secret", secret);
        this.serviceName = Check.notBlank("serviceName", serviceName);
    }

    /**
     * 为一次内部请求生成签名请求头。
     *
     * @param method HTTP 方法。
     * @param path 请求路径，不包含查询字符串。
     * @param body 请求体字节，允许为 null 或空数组。
     * @return 按请求头名称组织的签名头，调用方按顺序写入即可。
     */
    public Map<String, String> signHeaders(String method, String path, byte[] body) {
        long timestampMillis = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString();
        String signature = InternalServiceSignature.sign(
                secret, serviceName, timestampMillis, nonce, method, path, body);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(InternalServiceHeaders.SERVICE, serviceName);
        headers.put(InternalServiceHeaders.TIMESTAMP, Long.toString(timestampMillis));
        headers.put(InternalServiceHeaders.NONCE, nonce);
        headers.put(InternalServiceHeaders.SIGNATURE, signature);
        return headers;
    }

    /**
     * 获取当前服务名，便于调用方排查签名头中的服务标识。
     *
     * @return 当前服务名。
     */
    public String getServiceName() {
        return serviceName;
    }
}
