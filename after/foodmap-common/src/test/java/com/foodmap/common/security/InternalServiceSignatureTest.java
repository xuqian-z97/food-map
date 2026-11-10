package com.foodmap.common.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class InternalServiceSignatureTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final String SERVICE = "foodmap-auth-service";
    private static final String PATH = "/internal/users/provision";

    @Test
    void shouldProduceStableCanonicalString() {
        String canonical = InternalServiceSignature.canonicalString(
                SERVICE, 1700000000000L, "nonce-1", "post", PATH,
                "{\"userId\":1}".getBytes(StandardCharsets.UTF_8));

        assertThat(canonical).isEqualTo(
                SERVICE + "\n1700000000000\nnonce-1\nPOST\n" + PATH + "\n"
                        + InternalServiceSignature.sha256Hex("{\"userId\":1}".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void sameInputShouldProduceSameSignature() {
        byte[] body = "{\"userId\":1}".getBytes(StandardCharsets.UTF_8);

        String first = InternalServiceSignature.sign(SECRET, SERVICE, 1700000000000L, "nonce-1", "POST", PATH, body);
        String second = InternalServiceSignature.sign(SECRET, SERVICE, 1700000000000L, "nonce-1", "POST", PATH, body);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void changedBodyShouldChangeSignature() {
        String first = InternalServiceSignature.sign(SECRET, SERVICE, 1700000000000L, "nonce-1", "POST", PATH,
                "{\"userId\":1}".getBytes(StandardCharsets.UTF_8));
        String second = InternalServiceSignature.sign(SECRET, SERVICE, 1700000000000L, "nonce-1", "POST", PATH,
                "{\"userId\":2}".getBytes(StandardCharsets.UTF_8));

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void emptyBodyShouldUseStableHash() {
        assertThat(InternalServiceSignature.sha256Hex(null))
                .isEqualTo(InternalServiceSignature.sha256Hex(new byte[0]))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void constantTimeEqualsShouldRejectDifferentValues() {
        assertThat(InternalServiceSignature.constantTimeEquals("abc", "abc")).isTrue();
        assertThat(InternalServiceSignature.constantTimeEquals("abc", "abd")).isFalse();
        assertThat(InternalServiceSignature.constantTimeEquals(null, "abc")).isFalse();
        assertThat(InternalServiceSignature.constantTimeEquals("abc", null)).isFalse();
    }
}
