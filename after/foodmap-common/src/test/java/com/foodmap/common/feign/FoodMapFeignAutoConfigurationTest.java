package com.foodmap.common.feign;

import feign.RequestInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FoodMapFeignAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FoodMapFeignAutoConfiguration.class));

    @Test
    void registersTraceInterceptorWhenFeignIsAvailable() {
        contextRunner.run(context -> assertThat(context)
                .hasBean("foodMapFeignTraceRequestInterceptor")
                .hasSingleBean(RequestInterceptor.class)
                .getBean("foodMapFeignTraceRequestInterceptor")
                .isInstanceOf(RequestInterceptor.class));
    }

    @Test
    void backsOffWhenFeignIsNotAvailable() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("feign"))
                .run(context -> assertThat(context).doesNotHaveBean("foodMapFeignTraceRequestInterceptor"));
    }
}
