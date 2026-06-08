package com.foodmap.auth.domain;

import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import com.foodmap.common.validation.Check;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * HMAC Token 签发器，用于 MVP 阶段生成可验证结构的 Access Token 和 Refresh Token。
 *
 * <p>该实现先提供稳定接口和测试闭环，后续接入标准 JWT 库时只替换本类，不影响应用层。</p>
 */
@Component
public class HmacTokenIssuer {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final String secret;

    /**
     * 使用本地默认密钥创建签发器，生产环境后续必须从安全配置注入。
     */
    public HmacTokenIssuer() {
        this("foodmap-local-token-secret-please-change");
    }

    /**
     * 使用指定密钥创建签发器，测试可传入固定值以便排查 Token 结构。
     */
    public HmacTokenIssuer(String secret) {
        this.secret = Check.notBlank("secret", secret);
    }

    /**
     * 签发 Access Token，载荷包含账号和用户业务主键，供网关和服务端提取身份。
     */
    public String issueAccessToken(Long accountId, Long userId, OffsetDateTime expiresTime) {
        return issue("ACCESS", accountId, userId, expiresTime);
    }

    /**
     * 签发 Refresh Token，载荷包含随机 nonce，降低刷新令牌碰撞和重放排查难度。
     */
    public String issueRefreshToken(Long accountId, Long userId, OffsetDateTime expiresTime) {
        return issue("REFRESH", accountId, userId, expiresTime) + "." + UUID.randomUUID();
    }

    /**
     * 对明文 Token 做哈希摘要，用于数据库保存 Refresh Token 时避免落明文。
     */
    public String tokenHash(String token) {
        return sign(Check.notBlank("token", token));
    }

    private String issue(String tokenType, Long accountId, Long userId, OffsetDateTime expiresTime) {
        Check.positive("accountId", accountId);
        Check.positive("userId", userId);
        String header = URL_ENCODER.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = URL_ENCODER.encodeToString(("{\"tokenType\":\"" + tokenType
                + "\",\"accountId\":" + accountId
                + ",\"userId\":" + userId
                + ",\"expiresTime\":\"" + expiresTime + "\"}").getBytes(StandardCharsets.UTF_8));
        String unsignedToken = header + "." + payload;
        return unsignedToken + "." + sign(unsignedToken);
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new FoodMapException(CommonErrorCode.INTERNAL_ERROR, "Token 签名服务不可用");
        }
    }
}
