package com.foodmap.gateway.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 后台 Token 测试支撑：生成测试密钥、按共享契约签发 Token 和构造网关验签器。
 *
 * <p>签发逻辑独立于后台服务实现，等价于两端共享的签名测试向量来源。</p>
 */
public final class TestAdminTokens {
    /**
     * 后台 Token 签发方常量副本。
     */
    private static final String ISSUER = "foodmap-admin-service";
    /**
     * 后台 Token 受众常量副本。
     */
    private static final String AUDIENCE = "foodmap-admin-api";
    /**
     * 后台 Token 类型常量副本。
     */
    private static final String TOKEN_TYPE = "foodmap-admin+jwt";

    private TestAdminTokens() {
    }

    /**
     * 生成测试用 RSA 密钥对。
     *
     * @return 含私钥的 RSA 密钥。
     */
    public static RSAKey generateKey() {
        try {
            return new RSAKeyGenerator(2048).keyUse(KeyUse.SIGNATURE).keyID("gateway-test-key").generate();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * 构造只使用指定公钥的网关验签器。
     *
     * @param keyPair 含私钥的密钥对。
     * @return 网关侧验签器。
     */
    public static GatewayAdminTokenVerifier verifierFor(RSAKey keyPair) {
        try {
            return new GatewayAdminTokenVerifier(keyPair.toRSAPublicKey());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * 按两端契约签发后台 Token。
     *
     * @param keyPair 签名密钥。
     * @param adminUserId 后台管理员业务主键。
     * @return JWT 文本。
     */
    public static String issueToken(RSAKey keyPair, long adminUserId) {
        return issueToken(keyPair, adminUserId, System.currentTimeMillis() + 900_000);
    }

    /**
     * 按两端契约签发指定过期时间的后台 Token。
     *
     * @param keyPair 签名密钥。
     * @param adminUserId 后台管理员业务主键。
     * @param expiresAtMillis 过期时间毫秒值。
     * @return JWT 文本。
     */
    public static String issueToken(RSAKey keyPair, long adminUserId, long expiresAtMillis) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(ISSUER)
                    .audience(List.of(AUDIENCE))
                    .subject(String.valueOf(adminUserId))
                    .issueTime(new Date())
                    .expirationTime(new Date(expiresAtMillis))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("typ", TOKEN_TYPE)
                    .claim("roles", List.of("OPS_VIEWER"))
                    .claim("permissionVersion", 1)
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), claims);
            jwt.sign(new RSASSASigner(keyPair.toRSAPrivateKey()));
            return jwt.serialize();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * 使用自定义声明签发后台 Token，用于构造不合规声明场景。
     *
     * @param keyPair 签名密钥。
     * @param claims 自定义声明。
     * @return JWT 文本。
     */
    public static String issueCustomToken(RSAKey keyPair, JWTClaimsSet claims) {
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), claims);
            jwt.sign(new RSASSASigner(keyPair.toRSAPrivateKey()));
            return jwt.serialize();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * 构造契约合规的默认声明构造器。
     *
     * @return 声明构造器。
     */
    public static JWTClaimsSet.Builder validClaims() {
        return new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(List.of(AUDIENCE))
                .subject("100001")
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 900_000))
                .jwtID(UUID.randomUUID().toString())
                .claim("typ", TOKEN_TYPE)
                .claim("roles", List.of("OPS_VIEWER"))
                .claim("permissionVersion", 1);
    }
}
