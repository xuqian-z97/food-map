package com.foodmap.common.feign;

import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * FoodMap Feign 公共自动配置，负责为内部服务调用提供统一链路头透传能力。
 */
@AutoConfiguration
public class FoodMapFeignAutoConfiguration {

    /**
     * Feign 存在时注册 FoodMap 标准 Feign 拦截器。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RequestInterceptor.class)
    static class FeignTraceRequestInterceptorConfiguration {

        /**
         * 注册 FoodMap 标准 Feign 链路头透传拦截器。
         *
         * @return Feign 链路头透传拦截器。
         */
        @Bean(name = "foodMapFeignTraceRequestInterceptor")
        @ConditionalOnMissingBean(name = "foodMapFeignTraceRequestInterceptor")
        RequestInterceptor foodMapFeignTraceRequestInterceptor() {
            return new FoodMapFeignTraceRequestInterceptor();
        }
    }
}
