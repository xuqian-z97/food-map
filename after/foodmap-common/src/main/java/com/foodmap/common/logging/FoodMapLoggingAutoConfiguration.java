package com.foodmap.common.logging;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

/**
 * FoodMap Servlet 日志自动配置，为业务服务注册日志上下文和访问摘要过滤器。
 *
 * <p>本配置仅在 Servlet Web 应用生效，避免影响 Spring Cloud Gateway 的 WebFlux 模型。网关链路 ID 由
 * {@code GatewayTraceFilter} 负责，业务服务从请求头接收并写入 MDC。</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(FoodMapLoggingProperties.class)
public class FoodMapLoggingAutoConfiguration {
    private static final String DEFAULT_SERVICE_NAME = "foodmap-service";

    /**
     * 注册 MDC 上下文过滤器，顺序靠前以覆盖后续业务日志。
     *
     * @param properties FoodMap 日志配置。
     * @param environment Spring 环境变量，用于回退读取服务名。
     * @return MDC 过滤器注册 Bean。
     */
    @Bean
    @ConditionalOnMissingBean(LogMdcFilter.class)
    public FilterRegistrationBean<LogMdcFilter> foodMapLogMdcFilter(FoodMapLoggingProperties properties,
                                                                    Environment environment) {
        FilterRegistrationBean<LogMdcFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new LogMdcFilter(resolveServiceName(properties, environment)));
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setName("foodMapLogMdcFilter");
        return registrationBean;
    }

    /**
     * 注册 API 访问摘要日志过滤器。
     *
     * @param properties FoodMap 日志配置。
     * @return API 访问日志过滤器注册 Bean。
     */
    @Bean
    @ConditionalOnMissingBean(ApiAccessLogFilter.class)
    public FilterRegistrationBean<ApiAccessLogFilter> foodMapApiAccessLogFilter(FoodMapLoggingProperties properties) {
        FilterRegistrationBean<ApiAccessLogFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new ApiAccessLogFilter(properties.getSlowThresholdMs()));
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setName("foodMapApiAccessLogFilter");
        registrationBean.setEnabled(properties.isAccessLogEnabled());
        return registrationBean;
    }

    /**
     * 解析当前服务名，优先使用 foodmap.logging.service-name，其次使用 spring.application.name。
     *
     * @param properties FoodMap 日志配置。
     * @param environment Spring 环境变量。
     * @return 当前服务名。
     */
    private String resolveServiceName(FoodMapLoggingProperties properties, Environment environment) {
        if (properties.getServiceName() != null && !properties.getServiceName().isBlank()) {
            return properties.getServiceName().trim();
        }
        String applicationName = environment.getProperty("spring.application.name");
        if (applicationName != null && !applicationName.isBlank()) {
            return applicationName.trim();
        }
        return DEFAULT_SERVICE_NAME;
    }
}
