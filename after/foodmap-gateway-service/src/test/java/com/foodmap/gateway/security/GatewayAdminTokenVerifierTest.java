package com.foodmap.gateway.security;

import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关侧后台 Token 验签测试：算法、签名、签发方、受众、类型和必填声明契约。
 */
class GatewayAdminTokenVerifierTest {

    @Test
    void verifyShouldAcceptContractCompliantToken() {
        RSAKey keyPair = TestAdminTokens.generateKey();
        GatewayAdminTokenVerifier verifier = TestAdminTokens.verifierFor(keyPair);

        assertThat(verifier.verify(TestAdminTokens.issueToken(keyPair, 100001L))).isTrue();
    }

    @Test
    void verifyShouldRejectTokenSignedByDifferentKey() {
        GatewayAdminTokenVerifier verifier = TestAdminTokens.verifierFor(TestAdminTokens.generateKey());
        String token = TestAdminTokens.issueToken(TestAdminTokens.generateKey(), 100001L);

        assertThat(verifier.verify(token)).isFalse();
    }

    @Test
    void verifyShouldRejectTamperedOrMalformedToken() {
        RSAKey keyPair = TestAdminTokens.generateKey();
        GatewayAdminTokenVerifier verifier = TestAdminTokens.verifierFor(keyPair);
        String token = TestAdminTokens.issueToken(keyPair, 100001L);

        assertThat(verifier.verify(token + "x")).isFalse();
        assertThat(verifier.verify(token.substring(0, token.length() - 2))).isFalse();
        assertThat(verifier.verify("not-a-jwt")).isFalse();
        assertThat(verifier.verify(null)).isFalse();
        assertThat(verifier.verify("")).isFalse();
    }

    @Test
    void verifyShouldRejectExpiredToken() {
        RSAKey keyPair = TestAdminTokens.generateKey();
        GatewayAdminTokenVerifier verifier = TestAdminTokens.verifierFor(keyPair);

        assertThat(verifier.verify(TestAdminTokens.issueToken(keyPair, 100001L,
                System.currentTimeMillis() - 60_000))).isFalse();
    }

    @Test
    void verifyShouldRejectWrongIssuerAudienceOrType() {
        RSAKey keyPair = TestAdminTokens.generateKey();
        GatewayAdminTokenVerifier verifier = TestAdminTokens.verifierFor(keyPair);

        assertThat(verifier.verify(TestAdminTokens.issueCustomToken(keyPair,
                TestAdminTokens.validClaims().issuer("foodmap-auth-service").build()))).isFalse();
        assertThat(verifier.verify(TestAdminTokens.issueCustomToken(keyPair,
                TestAdminTokens.validClaims().audience(java.util.List.of("foodmap-api")).build()))).isFalse();
        assertThat(verifier.verify(TestAdminTokens.issueCustomToken(keyPair,
                TestAdminTokens.validClaims().claim("typ", "app+jwt").build()))).isFalse();
    }

    @Test
    void verifyShouldRejectMissingOrInvalidRequiredClaims() {
        RSAKey keyPair = TestAdminTokens.generateKey();
        GatewayAdminTokenVerifier verifier = TestAdminTokens.verifierFor(keyPair);

        assertThat(verifier.verify(TestAdminTokens.issueCustomToken(keyPair,
                TestAdminTokens.validClaims().subject("not-a-number").build()))).isFalse();
        assertThat(verifier.verify(TestAdminTokens.issueCustomToken(keyPair,
                TestAdminTokens.validClaims().subject("0").build()))).isFalse();
        assertThat(verifier.verify(TestAdminTokens.issueCustomToken(keyPair,
                TestAdminTokens.validClaims().claim("permissionVersion", "abc").build()))).isFalse();
        assertThat(verifier.verify(TestAdminTokens.issueCustomToken(keyPair,
                TestAdminTokens.validClaims().claim("roles", "OPS_VIEWER").build()))).isFalse();
        assertThat(verifier.verify(TestAdminTokens.issueCustomToken(keyPair,
                TestAdminTokens.validClaims().claim("typ", "foodmap-admin+jwt").jwtID(null).build()))).isFalse();
    }
}
