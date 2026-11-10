package com.foodmap.admin.security;

import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 后台 Access Token 验签组件，使用后台专用公钥校验 RS256 签名和全部必填声明。
 *
 * <p>验签失败的响应统一为 401 语义的脱敏提示，不暴露具体失败原因，避免为伪造 Token 提供探测信息。</p>
 */
public class AdminTokenVerifier {
    private final NimbusJwtDecoder decoder;

    /**
     * 使用后台公钥创建验签组件。
     *
     * @param publicKey 后台 Token RSA 公钥。
     */
    public AdminTokenVerifier(RSAPublicKey publicKey) {
        NimbusJwtDecoder built = NimbusJwtDecoder.withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        built.setJwtValidator(new DelegatingOAuth2TokenValidator<>(List.of(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(AdminTokenConstants.ISSUER),
                audienceValidator(),
                new JwtClaimValidator<>(AdminTokenConstants.CLAIM_TOKEN_TYPE, AdminTokenConstants.TOKEN_TYPE::equals),
                requiredClaimsValidator()
        )));
        this.decoder = built;
    }

    /**
     * 验签并解析后台 Access Token。
     *
     * @param token Bearer 凭证中的 JWT 文本。
     * @return 验签通过的 Token 事实。
     * @throws FoodMapException 签名、算法、受众、签发方、类型、必填声明或时间窗不合规时抛出 401。
     */
    public VerifiedAdminToken verify(String token) {
        Jwt jwt;
        try {
            jwt = decoder.decode(token);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "无效的后台访问令牌");
        }
        try {
            return new VerifiedAdminToken(
                    Long.parseLong(jwt.getSubject()),
                    ((Number) jwt.getClaim(AdminTokenConstants.CLAIM_PERMISSION_VERSION)).longValue(),
                    jwt.getId(),
                    OffsetDateTime.ofInstant(jwt.getExpiresAt(), ZoneOffset.UTC));
        } catch (RuntimeException ex) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "无效的后台访问令牌");
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
            if (audience == null || !audience.contains(AdminTokenConstants.AUDIENCE)) {
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
            Object permissionVersion = jwt.getClaim(AdminTokenConstants.CLAIM_PERMISSION_VERSION);
            if (!(permissionVersion instanceof Number number) || number.longValue() <= 0) {
                return OAuth2TokenValidatorResult.failure(error);
            }
            if (jwt.getId() == null || jwt.getId().isBlank()) {
                return OAuth2TokenValidatorResult.failure(error);
            }
            if (!(jwt.getClaim(AdminTokenConstants.CLAIM_ROLES) instanceof List<?>)) {
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
