package com.foodmap.gateway.filter;

import com.foodmap.gateway.security.GatewayAdminTokenVerifier;
import com.foodmap.gateway.security.TestAdminTokens;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link GatewayAuthFilter} 密钥注入单测，覆盖网关启动期的 fail-fast 行为。
 *
 * <p>网关不再内置任何默认密钥：密钥缺失、空白或短于 32 字符时必须在 Bean 创建阶段失败，
 * 避免与认证服务签验不一致或使用弱密钥对外提供认证能力。</p>
 */
class GatewayAuthFilterSecretTest {

    /**
     * 合法密钥固定测试值，长度正好 32 字符，用于覆盖长度下限边界。
     */
    private static final String VALID_SECRET = "0123456789abcdef0123456789abcdef";

    /**
     * 后台测试密钥与验签器，密钥注入失败发生在验签器使用之前，与验签结果无关。
     */
    private static final RSAKey ADMIN_KEY = TestAdminTokens.generateKey();
    private static final GatewayAdminTokenVerifier ADMIN_VERIFIER = TestAdminTokens.verifierFor(ADMIN_KEY);

    @Test
    void shouldRejectMissingTokenSecret() {
        assertThatThrownBy(() -> new GatewayAuthFilter(null, ADMIN_VERIFIER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("foodmap.security.token-secret")
                .hasMessageContaining("FOODMAP_TOKEN_SECRET");
    }

    @Test
    void shouldRejectBlankTokenSecret() {
        assertThatThrownBy(() -> new GatewayAuthFilter("   ", ADMIN_VERIFIER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FOODMAP_TOKEN_SECRET");
    }

    @Test
    void shouldRejectTokenSecretShorterThanMinimumLength() {
        assertThatThrownBy(() -> new GatewayAuthFilter("0123456789abcdef0123456789abcde", ADMIN_VERIFIER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("foodmap.security.token-secret")
                .hasMessageContaining("32");
    }

    @Test
    void shouldAcceptTokenSecretMeetingMinimumLength() {
        GatewayAuthFilter filter = new GatewayAuthFilter(VALID_SECRET, ADMIN_VERIFIER);

        assertThat(filter.getOrder()).isEqualTo(-100);
    }
}
