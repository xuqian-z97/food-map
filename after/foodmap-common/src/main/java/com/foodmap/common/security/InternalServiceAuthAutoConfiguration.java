package com.foodmap.common.security;

import com.foodmap.common.redis.redisson.FoodMapRedissonAutoConfiguration;
import feign.RequestInterceptor;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 内部服务身份自动配置，按调用方与被调方两个开关分别装配。
 *
 * <p>{@code foodmap.internal.enabled=true} 时装配出站签名器和 Feign 拦截器；</p>
 * <p>{@code foodmap.internal.verify-enabled=true} 时装配入站过滤器、校验器和 Redisson nonce 存储。</p>
 *
 * <p>两个开关分离的原因是：auth/admin 只发内部请求，不需要 Redis；user/log 只收内部请求，
 * 需要 nonce 去重。网关既不签发也不校验内部签名，保持关闭。</p>
 */
@AutoConfiguration(after = FoodMapRedissonAutoConfiguration.class)
@EnableConfigurationProperties(InternalServiceAuthProperties.class)
public class InternalServiceAuthAutoConfiguration {

    /**
     * 出站签名配置，供认证服务和管理后台构造内部请求签名头。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RequestInterceptor.class)
    @ConditionalOnProperty(prefix = "foodmap.internal", name = "enabled", havingValue = "true")
    static class SigningConfiguration {

        /**
         * 创建内部服务请求签名器。
         *
         * @param properties 内部服务身份配置。
         * @param serviceName 当前服务名，来自 {@code spring.application.name}。
         * @return 内部服务请求签名器。
         */
        @Bean
        @ConditionalOnMissingBean
        InternalServiceSigner internalServiceSigner(
                InternalServiceAuthProperties properties,
                @Value("${spring.application.name:unknown-service}") String serviceName
        ) {
            return new InternalServiceSigner(properties.getSecret(), serviceName);
        }

        /**
         * 注册 Feign 内部服务签名拦截器，只对 {@code /internal/**} 请求生效。
         *
         * @param signer 内部服务请求签名器。
         * @return Feign 签名拦截器。
         */
        @Bean
        @ConditionalOnMissingBean(name = "internalServiceFeignRequestInterceptor")
        RequestInterceptor internalServiceFeignRequestInterceptor(InternalServiceSigner signer) {
            return new InternalServiceFeignRequestInterceptor(signer);
        }
    }

    /**
     * 入站校验配置，供用户服务和日志服务校验内部请求。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = "foodmap.internal", name = "verify-enabled", havingValue = "true")
    static class VerificationConfiguration {

        /**
         * 创建基于 Redisson 的 nonce 去重存储；Redis 缺失时启动失败，避免静默放行。
         *
         * @param redissonClient Redisson 客户端。
         * @return nonce 去重存储。
         */
        @Bean
        @ConditionalOnMissingBean
        InternalServiceNonceStore internalServiceNonceStore(RedissonClient redissonClient) {
            return new InternalServiceRedissonNonceStore(redissonClient);
        }

        /**
         * 创建内部服务请求校验器。
         *
         * @param properties 内部服务身份配置。
         * @param nonceStore nonce 去重存储。
         * @return 内部服务请求校验器。
         */
        @Bean
        @ConditionalOnMissingBean
        InternalServiceRequestVerifier internalServiceRequestVerifier(
                InternalServiceAuthProperties properties,
                InternalServiceNonceStore nonceStore
        ) {
            return new InternalServiceRequestVerifier(properties, nonceStore);
        }

        /**
         * 注册内部服务签名校验过滤器。
         *
         * @param verifier 内部服务请求校验器。
         * @return 过滤器注册对象。
         */
        @Bean
        FilterRegistrationBean<InternalServiceSignatureFilter> internalServiceSignatureFilter(
                InternalServiceRequestVerifier verifier
        ) {
            FilterRegistrationBean<InternalServiceSignatureFilter> registration =
                    new FilterRegistrationBean<>(new InternalServiceSignatureFilter(verifier));
            registration.setName("internalServiceSignatureFilter");
            registration.addUrlPatterns("/*");
            registration.setOrder(InternalServiceSignatureFilter.FILTER_ORDER);
            return registration;
        }
    }
}
