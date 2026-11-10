package com.foodmap.gateway.security;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.interfaces.RSAPublicKey;
import java.util.List;

/**
 * 网关侧后台 Access Token 验签组件，与后台服务验签入口相互独立但使用同一 JWT 契约。
 *
 * <p>网关只持有后台公钥，只做签名与声明校验；会话有效性和权限仍由后台服务按数据库授权判定。</p>
 */
public class GatewayAdminTokenVerifier {
    /**
     * 后台 Token 签发方，取值必须与后台服务签发一致。
     */
    public static final String ISSUER = "foodmap-admin-service";
    /**
     * 后台 Token 受众，取值必须与后台服务签发一致。
     */
    public static final String AUDIENCE = "foodmap-admin-api";
    /**
     * 后台 Token 类型声明值，取值必须与后台服务签发一致。
     */
    public static final String TOKEN_TYPE = "foodmap-admin+jwt";
    /**
     * Token 类型声明名。
     */
    public static final String CLAIM_TOKEN_TYPE = "typ";
    /**
     * 签发时权限版本声明名。
     */
    public static final String CLAIM_PERMISSION_VERSION = "permissionVersion";
    /**
     * 签发时角色编码列表声明名。
     */
    public static final String CLAIM_ROLES = "roles";

    private final NimbusJwtDecoder decoder;

    /**
     * 使用后台公钥创建网关侧验签组件。
     *
     * @param publicKey 后台 Token RSA 公钥。
     */
    public GatewayAdminTokenVerifier(RSAPublicKey publicKey) {
        NimbusJwtDecoder built = NimbusJwtDecoder.withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        built.setJwtValidator(new DelegatingOAuth2TokenValidator<>(List.of(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(ISSUER),
                audienceValidator(),
                new JwtClaimValidator<>(CLAIM_TOKEN_TYPE, TOKEN_TYPE::equals),
                requiredClaimsValidator()
        )));
        this.decoder = built;
    }

    /**
     * 验签后台 Access Token，任何不合规情况都返回 false，具体原因由统一 401 响应脱敏。
     *
     * @param token Bearer 凭证中的 JWT 文本。
     * @return 验签通过返回 true。
     */
    public boolean verify(String token) {
        try {
            decoder.decode(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * 构建受众校验器，要求 aud 声明包含后台专用受众。
     *
     * @return 后台受众校验器。
     */
    private static OAuth2TokenValidator<Jwt> audienceValidator() {
        return jwt -> {
            List<String> audience = jwt.getAudience();
            if (audience == null || !audience.contains(AUDIENCE)) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "后台 Token 受众不匹配", null));
            }
            return OAuth2TokenValidatorResult.success();
        };
    }

    /**
     * 构建必填声明校验器，覆盖 sub、permissionVersion、jti 和 roles 声明形态。
     *
     * @return 必填声明校验器。
     */
    private static OAuth2TokenValidator<Jwt> requiredClaimsValidator() {
        return jwt -> {
            OAuth2Error error = new OAuth2Error("invalid_token", "后台 Token 必填声明不合规", null);
            String subject = jwt.getSubject();
            if (subject == null || parsePositiveLong(subject) <= 0) {
                return OAuth2TokenValidatorResult.failure(error);
            }
            Object permissionVersion = jwt.getClaim(CLAIM_PERMISSION_VERSION);
            if (!(permissionVersion instanceof Number number) || number.longValue() <= 0) {
                return OAuth2TokenValidatorResult.failure(error);
            }
            if (jwt.getId() == null || jwt.getId().isBlank()) {
                return OAuth2TokenValidatorResult.failure(error);
            }
            if (!(jwt.getClaim(CLAIM_ROLES) instanceof List<?>)) {
                return OAuth2TokenValidatorResult.failure(error);
            }
            return OAuth2TokenValidatorResult.success();
        };
    }

    /**
     * 解析十进制正整数文本，解析失败时返回 -1 以便按声明不合规拒绝。
     *
     * @param text 待解析文本。
     * @return 解析出的正整数，解析失败时返回 -1。
     */
    private static long parsePositiveLong(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
}
