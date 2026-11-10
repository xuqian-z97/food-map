package com.foodmap.gateway.security;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 网关后台 Token 验签装配，只加载后台公钥，网关不持有私钥。
 *
 * <p>密钥位置缺失或无法解析时直接阻止网关启动，避免后台路由开放但验签失效。</p>
 */
@Configuration
public class GatewayAdminTokenConfiguration implements ResourceLoaderAware {
    /**
     * 后台 Token 公钥位置配置键，与管理后台服务共用同一前缀。
     */
    public static final String PUBLIC_KEY_LOCATION_KEY = "foodmap.admin.token.public-key-location";

    private ResourceLoader resourceLoader;

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 创建网关侧后台 Token 验签组件。
     *
     * @param publicKeyLocation 公钥资源位置，来自 `foodmap.admin.token.public-key-location`。
     * @return 网关侧验签组件。
     */
    @Bean
    public GatewayAdminTokenVerifier gatewayAdminTokenVerifier(
            @Value("${" + PUBLIC_KEY_LOCATION_KEY + "}") String publicKeyLocation) {
        if (publicKeyLocation == null || publicKeyLocation.isBlank()) {
            throw new IllegalStateException(PUBLIC_KEY_LOCATION_KEY
                    + " 未配置：网关必须注入后台 Token 公钥才能保护 /api/admin/** 路由");
        }
        Resource resource = resourceLoader.getResource(publicKeyLocation);
        if (!resource.exists()) {
            throw new IllegalStateException("后台 Token 公钥文件不存在：" + publicKeyLocation);
        }
        RSAPublicKey publicKey;
        try {
            publicKey = parseRsaPublicKey(publicKeyLocation, resource.getContentAsString(StandardCharsets.UTF_8));
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("后台 Token 公钥文件无法解析：" + publicKeyLocation, ex);
        }
        return new GatewayAdminTokenVerifier(publicKey);
    }

    /**
     * 解析 X.509 PEM 公钥为 RSA 公钥，使用 JDK KeyFactory 避免引入 BouncyCastle 依赖。
     *
     * @param location 公钥资源位置，用于异常信息定位。
     * @param pemContent PEM 文件内容。
     * @return RSA 公钥。
     */
    private static RSAPublicKey parseRsaPublicKey(String location, String pemContent) {
        String begin = "-----BEGIN PUBLIC KEY-----";
        String end = "-----END PUBLIC KEY-----";
        int beginIndex = pemContent.indexOf(begin);
        int endIndex = pemContent.indexOf(end);
        if (pemContent.isBlank() || beginIndex < 0 || endIndex < 0 || endIndex < beginIndex) {
            throw new IllegalStateException("后台 Token 公钥 PEM 格式不正确：" + location);
        }
        try {
            byte[] der = Base64.getDecoder()
                    .decode(pemContent.substring(beginIndex + begin.length(), endIndex).replaceAll("\\s", ""));
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new IllegalStateException("后台 Token 公钥 base64 无法解析：" + location, ex);
        } catch (Exception ex) {
            throw new IllegalStateException("后台 Token 公钥无法解析为 RSA 密钥：" + location, ex);
        }
    }
}
