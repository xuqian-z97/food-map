package com.foodmap.common.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * FoodMap Web 异常处理自动配置，确保各 Servlet 微服务引入 common 后自动获得统一异常响应。
 *
 * <p>该配置只在 Servlet Web 应用中生效，避免影响 Spring Cloud Gateway 的 WebFlux 运行模型。</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class FoodMapWebExceptionAutoConfiguration {

    /**
     * 注册统一异常处理器；服务如果确有特殊场景，可以自定义同类型 Bean 覆盖。
     *
     * @return FoodMap 全局异常处理器。
     */
    @Bean
    @ConditionalOnMissingBean(FoodMapGlobalExceptionHandler.class)
    public FoodMapGlobalExceptionHandler foodMapGlobalExceptionHandler() {
        return new FoodMapGlobalExceptionHandler();
    }
}
