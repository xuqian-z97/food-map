package com.foodmap.common.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Feign 内部服务签名拦截器，只为 {@code /internal/**} 出站请求写入签名头。
 *
 * <p>业务 Feign 调用外部服务或非内部路径时不会附加签名头，避免把内部身份扩散到无关注册调用。
 * 健康检查路径同样跳过，保持与网关预检一致。</p>
 */
public class InternalServiceFeignRequestInterceptor implements RequestInterceptor {

    private final InternalServiceSigner signer;

    /**
     * 创建 Feign 内部服务签名拦截器。
     *
     * @param signer 内部服务请求签名器。
     */
    public InternalServiceFeignRequestInterceptor(InternalServiceSigner signer) {
        if (signer == null) {
            throw new IllegalArgumentException("signer must not be null");
        }
        this.signer = signer;
    }

    /**
     * 为内部路径的 Feign 请求补充签名头。
     *
     * @param template Feign 请求模板。
     */
    @Override
    public void apply(RequestTemplate template) {
        String path = template.path();
        if (!InternalServiceAccessPolicy.isInternalPath(path)
                || InternalServiceAccessPolicy.isHealthPath(path)) {
            return;
        }
        signer.signHeaders(template.method(), path, template.body())
                .forEach(template::header);
    }
}
