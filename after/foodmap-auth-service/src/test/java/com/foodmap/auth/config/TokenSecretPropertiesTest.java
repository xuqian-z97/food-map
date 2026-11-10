package com.foodmap.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TokenSecretProperties} 单测，覆盖密钥缺失、长度不足、合法密钥绑定三种启动期行为。
 *
 * <p>这些用例只验证配置绑定和 fail-fast 语义，不启动真实认证服务，也不访问数据库和注册中心。</p>
 */
class TokenSecretPropertiesTest {

    /**
     * 合法密钥固定测试值，长度正好 32 字符，用于覆盖长度下限边界。
     */
    private static final String VALID_SECRET = "0123456789abcdef0123456789abcdef";

    private static final String TOKEN_SECRET_PROPERTY = "foodmap.security.token-secret";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TokenSecretPropertiesOnlyConfiguration.class);

    @Test
    void shouldFailStartupWhenTokenSecretIsMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("FOODMAP_TOKEN_SECRET");
        });
    }

    @Test
    void shouldFailStartupWhenTokenSecretIsShorterThanMinimumLength() {
        contextRunner
                .withPropertyValues(TOKEN_SECRET_PROPERTY + "=0123456789abcdef0123456789abcde")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining(TOKEN_SECRET_PROPERTY)
                            .hasStackTraceContaining("32");
                });
    }

    @Test
    void shouldBindTokenSecretWhenLengthMeetsMinimum() {
        contextRunner
                .withPropertyValues(TOKEN_SECRET_PROPERTY + "=" + VALID_SECRET)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(TokenSecretProperties.class).getTokenSecret()).isEqualTo(VALID_SECRET);
                });
    }

    @Test
    void shouldRejectBlankTokenSecretWhenValidatedDirectly() {
        TokenSecretProperties properties = new TokenSecretProperties();
        properties.setTokenSecret("   ");

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(TOKEN_SECRET_PROPERTY)
                .hasMessageContaining("FOODMAP_TOKEN_SECRET");
    }

    @Test
    void shouldRejectShortTokenSecretWhenValidatedDirectly() {
        TokenSecretProperties properties = new TokenSecretProperties();
        properties.setTokenSecret("0123456789abc");

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(TOKEN_SECRET_PROPERTY)
                .hasMessageContaining("32");
    }

    @Test
    void shouldPassValidationWhenTokenSecretMeetsMinimumLength() {
        TokenSecretProperties properties = new TokenSecretProperties();
        properties.setTokenSecret(VALID_SECRET);

        properties.validate();

        assertThat(properties.getTokenSecret()).isEqualTo(VALID_SECRET);
    }

    /**
     * 最小 Spring 配置，只注册配置属性绑定，不加载认证服务其它 Bean。
     */
    @Configuration
    @EnableConfigurationProperties(TokenSecretProperties.class)
    static class TokenSecretPropertiesOnlyConfiguration {
    }
}
