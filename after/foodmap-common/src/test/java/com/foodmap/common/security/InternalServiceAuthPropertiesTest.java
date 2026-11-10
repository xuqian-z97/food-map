package com.foodmap.common.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalServiceAuthPropertiesTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void disabledShouldNotRequireSecret() {
        InternalServiceAuthProperties properties = new InternalServiceAuthProperties();

        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    void enabledWithoutSecretShouldFailFast() {
        InternalServiceAuthProperties properties = new InternalServiceAuthProperties();
        properties.setEnabled(true);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FOODMAP_INTERNAL_SECRET");
    }

    @Test
    void shortSecretShouldFailFast() {
        InternalServiceAuthProperties properties = new InternalServiceAuthProperties();
        properties.setVerifyEnabled(true);
        properties.setSecret("too-short");

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("长度不足");
    }

    @Test
    void nonceTtlMustExceedClockSkew() {
        InternalServiceAuthProperties properties = new InternalServiceAuthProperties();
        properties.setEnabled(true);
        properties.setSecret(SECRET);
        properties.setMaxClockSkewSeconds(300);
        properties.setNonceTtlSeconds(300);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nonce-ttl-seconds");
    }
}
