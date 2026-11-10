package com.foodmap.common.security;

import feign.RequestInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 内部签名自动配置边界测试：通用签名器不依赖 Feign，Feign 拦截器才按类路径选择装配。
 */
class InternalServiceAuthAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(InternalServiceAuthAutoConfiguration.class))
            .withPropertyValues("spring.application.name=foodmap-auth-service",
                    "foodmap.internal.secret=0123456789abcdef0123456789abcdef");

    @Test
    void shouldRegisterSignerAndFeignInterceptorWhenEnabled() {
        runner.withPropertyValues("foodmap.internal.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(InternalServiceSigner.class);
            assertThat(context).hasSingleBean(RequestInterceptor.class);
            assertThat(context.getBean(RequestInterceptor.class))
                    .isInstanceOf(InternalServiceFeignRequestInterceptor.class);
        });
    }

    @Test
    void shouldRegisterSignerWithoutFeign() {
        runner.withClassLoader(new FilteredClassLoader("feign"))
                .withPropertyValues("foodmap.internal.enabled=true").run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(InternalServiceSigner.class);
                    assertThat(context).doesNotHaveBean("internalServiceFeignRequestInterceptor");
                });
    }

    @Test
    void disabledShouldNotCreateSigningBeans() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(InternalServiceSigner.class);
            assertThat(context).doesNotHaveBean(RequestInterceptor.class);
        });
    }
}
