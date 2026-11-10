package com.foodmap.log.infrastructure.archive;

import com.foodmap.log.application.port.LogArchiveExportClient;
import com.foodmap.log.application.port.LogArchiveObjectStorageClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证默认关闭归档时的组件扫描能创建两个占位端口，避免摘要服务因缺 Bean 无法启动。
 */
class LogArchiveFallbackConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(FallbackScan.class);

    /** 验证缺省开关时两个占位端口均可用。 */
    @Test
    void registersFallbacksWhenPropertiesAreMissing() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(LogArchiveExportClient.class);
            assertThat(context).hasSingleBean(LogArchiveObjectStorageClient.class);
        });
    }

    /** 验证显式关闭归档与缺省行为一致。 */
    @Test
    void registersFallbacksWhenDisabledExplicitly() {
        runner.withPropertyValues("foodmap.log.archive.elasticsearch.enabled=false",
                        "foodmap.log.archive.storage.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(NoopLogArchiveExportClient.class);
                    assertThat(context).hasSingleBean(NoopLogArchiveObjectStorageClient.class);
                });
    }

    /** 验证显式开启时不会意外使用占位实现掩盖真实适配器缺失。 */
    @Test
    void excludesFallbacksWhenRealAdaptersAreRequested() {
        runner.withPropertyValues("foodmap.log.archive.elasticsearch.enabled=true",
                        "foodmap.log.archive.storage.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(LogArchiveExportClient.class);
                    assertThat(context).doesNotHaveBean(LogArchiveObjectStorageClient.class);
                });
    }

    /** 使用真实组件扫描复现条件注册顺序，不以手动注册 Bean 替代。 */
    @Configuration(proxyBeanMethods = false)
    @ComponentScan(basePackageClasses = NoopLogArchiveExportClient.class, useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = {NoopLogArchiveExportClient.class, NoopLogArchiveObjectStorageClient.class}))
    static class FallbackScan {
    }
}
