package com.foodmap.admin.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodmap.admin.application.port.LogApiAccessLogClient;
import com.foodmap.common.security.InternalServiceAuthAutoConfiguration;
import com.foodmap.common.security.InternalServiceSigner;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 管理后台真实依赖边界回归：Java HTTP 客户端不能因缺少 optional Feign 而丢失签名器。
 */
class AdminLogClientConfigurationTest {

    @Test
    void shouldCreateSignedLogClientWithoutFeignOnClasspath() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("feign", "org.springframework.cloud.openfeign"))
                .withConfiguration(AutoConfigurations.of(InternalServiceAuthAutoConfiguration.class))
                .withUserConfiguration(AdminLogClientConfiguration.class, JsonConfiguration.class)
                .withPropertyValues(
                        "foodmap.internal.enabled=true",
                        "foodmap.internal.secret=0123456789abcdef0123456789abcdef",
                        "spring.application.name=foodmap-admin-service")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(InternalServiceSigner.class);
                    assertThat(context).hasSingleBean(LogApiAccessLogClient.class);
                    assertThat(context).doesNotHaveBean("internalServiceFeignRequestInterceptor");
                    assertThat(context.getBean(InternalServiceSigner.class).getServiceName())
                            .isEqualTo("foodmap-admin-service");
                });
    }

    /** 测试只装配 JSON 与日志客户端，不连接数据库、Nacos 或外部 HTTP 服务。 */
    @Configuration(proxyBeanMethods = false)
    static class JsonConfiguration {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }
}
