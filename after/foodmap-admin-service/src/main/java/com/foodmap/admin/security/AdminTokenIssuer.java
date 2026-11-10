package com.foodmap.admin.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * 后台 Access Token 签发组件，使用独立 RSA 密钥签发 RS256 JWT。
 *
 * <p>Token 只承载签发时事实（管理员、角色、权限版本），接口授权始终以数据库当前授权为准。</p>
 */
public class AdminTokenIssuer {
    private final NimbusJwtEncoder encoder;
    private final Duration ttl;

    /**
     * 使用 RSA 公私钥和有效期创建签发组件。
     *
     * @param publicKey 后台 Token RSA 公钥。
     * @param privateKey 后台 Token RSA 私钥。
     * @param ttl Token 有效期。
     */
    public AdminTokenIssuer(RSAPublicKey publicKey, RSAPrivateKey privateKey, Duration ttl) {
        RSAKey keyWithId = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("foodmap-admin-token")
                .build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(keyWithId));
        this.encoder = new NimbusJwtEncoder(jwkSource);
        this.ttl = ttl;
    }

    /**
     * 签发后台 Access Token。
     *
     * @param adminUserId 后台管理员业务主键。
     * @param roles 签发时有效角色编码列表，允许为空。
     * @param permissionVersion 签发时权限版本。
     * @return 签发结果，包含 Token 文本、唯一标识和过期时间。
     */
    public AdminIssuedToken issue(long adminUserId, List<String> roles, long permissionVersion) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        String tokenId = UUID.randomUUID().toString();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(AdminTokenConstants.ISSUER)
                .audience(List.of(AdminTokenConstants.AUDIENCE))
                .subject(String.valueOf(adminUserId))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(tokenId)
                .claim(AdminTokenConstants.CLAIM_TOKEN_TYPE, AdminTokenConstants.TOKEN_TYPE)
                .claim(AdminTokenConstants.CLAIM_ROLES, List.copyOf(roles))
                .claim(AdminTokenConstants.CLAIM_PERMISSION_VERSION, permissionVersion)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        Jwt jwt = encoder.encode(JwtEncoderParameters.from(header, claims));
        return new AdminIssuedToken(
                jwt.getTokenValue(),
                tokenId,
                OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC));
    }

    /**
     * 签发结果值对象，tokenId 与过期时间用于会话登记与响应返回。
     *
     * @param tokenValue JWT 文本。
     * @param tokenId JWT jti。
     * @param expiresTime 过期时间。
     */
    public record AdminIssuedToken(String tokenValue, String tokenId, OffsetDateTime expiresTime) {
    }
}
