package com.foodmap.admin.config;

import com.foodmap.admin.security.AdminTokenIssuer;
import com.foodmap.admin.security.AdminTokenKeys;
import com.foodmap.admin.security.AdminTokenVerifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * 后台 Token 组件装配，负责加载 RSA 密钥并创建后台签发与验签组件。
 *
 * <p>密钥解析失败会直接阻止服务启动，避免签发与验签使用不一致的密钥。</p>
 */
@Configuration
@EnableConfigurationProperties(AdminTokenProperties.class)
public class AdminTokenConfiguration implements ResourceLoaderAware {
    private ResourceLoader resourceLoader;

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 创建后台 Access Token 验签组件，只使用配置注入的公钥。
     *
     * @param properties 后台 Token 配置。
     * @return 后台 Token 验签组件。
     */
    @Bean
    public AdminTokenVerifier adminTokenVerifier(AdminTokenProperties properties) {
        RSAPublicKey publicKey = AdminTokenKeys.parseRsaPublicKey(
                AdminTokenKeys.PUBLIC_KEY_LOCATION_KEY + "=" + properties.getPublicKeyLocation(),
                readResource(properties.getPublicKeyLocation()));
        return new AdminTokenVerifier(publicKey);
    }

    /**
     * 创建后台 Access Token 签发组件，必须同时持有私钥与公钥。
     *
     * @param properties 后台 Token 配置。
     * @return 后台 Token 签发组件。
     */
    @Bean
    public AdminTokenIssuer adminTokenIssuer(AdminTokenProperties properties) {
        if (properties.getPrivateKeyLocation() == null || properties.getPrivateKeyLocation().isBlank()) {
            throw new IllegalStateException(AdminTokenKeys.PRIVATE_KEY_LOCATION_KEY
                    + " 未配置：管理后台服务必须注入后台 Token 私钥才能签发登录凭证");
        }
        RSAPublicKey publicKey = AdminTokenKeys.parseRsaPublicKey(
                AdminTokenKeys.PUBLIC_KEY_LOCATION_KEY + "=" + properties.getPublicKeyLocation(),
                readResource(properties.getPublicKeyLocation()));
        RSAPrivateKey privateKey = AdminTokenKeys.parseRsaPrivateKey(
                AdminTokenKeys.PRIVATE_KEY_LOCATION_KEY + "=" + properties.getPrivateKeyLocation(),
                readResource(properties.getPrivateKeyLocation()));
        return new AdminTokenIssuer(publicKey, privateKey, properties.getTtl());
    }

    /**
     * 读取密钥资源内容。
     *
     * @param location 资源位置。
     * @return 资源文本内容。
     * @throws IllegalStateException 资源缺失或不可读时抛出，阻止服务启动。
     */
    private String readResource(String location) {
        if (location == null || location.isBlank()) {
            throw new IllegalStateException("后台 Token 密钥位置未配置");
        }
        try {
            Resource resource = resourceLoader.getResource(location);
            if (!resource.exists()) {
                throw new IllegalStateException("后台 Token 密钥文件不存在：" + location);
            }
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("后台 Token 密钥文件读取失败：" + location, ex);
        }
    }
}
