package com.foodmap.admin.security;

import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 后台 Token 签发与验签契约测试：算法、签名、签发方、受众、类型和必填声明两端一致。
 */
class AdminTokenIssuerVerifierTest {

    @Test
    void issueAndVerifyShouldRoundTripWithContractClaims() throws Exception {
        RSAKey keyPair = generateKey();
        AdminTokenIssuer issuer = new AdminTokenIssuer(keyPair.toRSAPublicKey(), keyPair.toRSAPrivateKey(), Duration.ofMinutes(15));
        AdminTokenVerifier verifier = new AdminTokenVerifier(keyPair.toRSAPublicKey());

        AdminTokenIssuer.AdminIssuedToken issued = issuer.issue(100001L, List.of("OPS_VIEWER"), 3L);
        VerifiedAdminToken verified = verifier.verify(issued.tokenValue());

        assertThat(verified.adminUserId()).isEqualTo(100001L);
        assertThat(verified.permissionVersion()).isEqualTo(3L);
        assertThat(verified.tokenId()).isEqualTo(issued.tokenId());
        assertThat(verified.expiresTime()).isAfter(java.time.OffsetDateTime.now());
    }

    @Test
    void verifyShouldRejectTokenSignedByDifferentKey() throws Exception {
        AdminTokenIssuer issuer = newIssuerWithFreshKey();
        AdminTokenVerifier verifier = new AdminTokenVerifier(generateKey().toRSAPublicKey());

        assertThatThrownBy(() -> verifier.verify(issuer.issue(100001L, List.of(), 1L).tokenValue()))
                .isInstanceOf(FoodMapException.class)
                .extracting(ex -> ((FoodMapException) ex).errorCode())
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }

    @Test
    void verifyShouldRejectTamperedToken() throws Exception {
        RSAKey keyPair = generateKey();
        AdminTokenIssuer issuer = new AdminTokenIssuer(keyPair.toRSAPublicKey(), keyPair.toRSAPrivateKey(), Duration.ofMinutes(15));
        AdminTokenVerifier verifier = new AdminTokenVerifier(keyPair.toRSAPublicKey());
        String token = issuer.issue(100001L, List.of(), 1L).tokenValue();

        assertThatThrownBy(() -> verifier.verify(token + "x"))
                .isInstanceOf(FoodMapException.class);
        assertThatThrownBy(() -> verifier.verify(token.substring(0, token.length() - 2)))
                .isInstanceOf(FoodMapException.class);
        assertThatThrownBy(() -> verifier.verify("not-a-jwt"))
                .isInstanceOf(FoodMapException.class);
        assertThatThrownBy(() -> verifier.verify(null))
                .isInstanceOf(FoodMapException.class);
    }

    @Test
    void verifyShouldRejectExpiredToken() throws Exception {
        RSAKey keyPair = generateKey();
        AdminTokenVerifier verifier = new AdminTokenVerifier(keyPair.toRSAPublicKey());
        String expired = customToken(keyPair, validClaims().expirationTime(new Date(System.currentTimeMillis() - 60_000)));

        assertThatThrownBy(() -> verifier.verify(expired))
                .isInstanceOf(FoodMapException.class)
                .extracting(ex -> ((FoodMapException) ex).errorCode())
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }

    @Test
    void verifyShouldRejectWrongIssuerAudienceOrTokenType() throws Exception {
        RSAKey keyPair = generateKey();
        AdminTokenVerifier verifier = new AdminTokenVerifier(keyPair.toRSAPublicKey());

        assertThatThrownBy(() -> verifier.verify(customToken(keyPair, validClaims().issuer("foodmap-auth-service"))))
                .isInstanceOf(FoodMapException.class);
        assertThatThrownBy(() -> verifier.verify(customToken(keyPair, validClaims().audience(List.of("foodmap-api")))))
                .isInstanceOf(FoodMapException.class);
        assertThatThrownBy(() -> verifier.verify(customToken(keyPair, validClaims().claim(
                AdminTokenConstants.CLAIM_TOKEN_TYPE, "app+jwt"))))
                .isInstanceOf(FoodMapException.class);
    }

    @Test
    void verifyShouldRejectMissingOrInvalidRequiredClaims() throws Exception {
        RSAKey keyPair = generateKey();
        AdminTokenVerifier verifier = new AdminTokenVerifier(keyPair.toRSAPublicKey());

        assertThatThrownBy(() -> verifier.verify(customToken(keyPair, validClaims().subject("not-a-number"))))
                .isInstanceOf(FoodMapException.class);
        assertThatThrownBy(() -> verifier.verify(customToken(keyPair, validClaims().subject("0"))))
                .isInstanceOf(FoodMapException.class);
        assertThatThrownBy(() -> verifier.verify(customToken(keyPair, validClaims().claim(
                AdminTokenConstants.CLAIM_PERMISSION_VERSION, "not-a-number"))))
                .isInstanceOf(FoodMapException.class);
        assertThatThrownBy(() -> verifier.verify(customToken(keyPair, validClaims().claim(
                AdminTokenConstants.CLAIM_PERMISSION_VERSION, 0))))
                .isInstanceOf(FoodMapException.class);
        assertThatThrownBy(() -> verifier.verify(customToken(keyPair, validClaims().jwtID(null))))
                .isInstanceOf(FoodMapException.class);
        assertThatThrownBy(() -> verifier.verify(customToken(keyPair, validClaims().claim(
                AdminTokenConstants.CLAIM_ROLES, "OPS_VIEWER"))))
                .isInstanceOf(FoodMapException.class);
    }

    /**
     * 用随机密钥构造签发组件。
     *
     * @return 后台 Token 签发组件。
     */
    private static AdminTokenIssuer newIssuerWithFreshKey() {
        try {
            RSAKey keyPair = generateKey();
            return new AdminTokenIssuer(keyPair.toRSAPublicKey(), keyPair.toRSAPrivateKey(), Duration.ofMinutes(15));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * 生成测试用 RSA 密钥。
     *
     * @return 含私钥的 RSA 密钥。
     */
    private static RSAKey generateKey() throws Exception {
        return new RSAKeyGenerator(2048).keyUse(KeyUse.SIGNATURE).keyID("test-key").generate();
    }

    /**
     * 构造契约合规的默认声明。
     *
     * @return 声明构造器。
     */
    private static JWTClaimsSet.Builder validClaims() {
        return new JWTClaimsSet.Builder()
                .issuer(AdminTokenConstants.ISSUER)
                .audience(List.of(AdminTokenConstants.AUDIENCE))
                .subject("100001")
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 900_000))
                .jwtID(UUID.randomUUID().toString())
                .claim(AdminTokenConstants.CLAIM_TOKEN_TYPE, AdminTokenConstants.TOKEN_TYPE)
                .claim(AdminTokenConstants.CLAIM_ROLES, List.of("OPS_VIEWER"))
                .claim(AdminTokenConstants.CLAIM_PERMISSION_VERSION, 1);
    }

    /**
     * 使用指定密钥和声明签发测试 Token。
     *
     * @param keyPair 签名密钥。
     * @param builder 声明构造器。
     * @return JWT 文本。
     */
    private static String customToken(RSAKey keyPair, JWTClaimsSet.Builder builder) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), builder.build());
        jwt.sign(new RSASSASigner(keyPair.toRSAPrivateKey()));
        return jwt.serialize();
    }
}
