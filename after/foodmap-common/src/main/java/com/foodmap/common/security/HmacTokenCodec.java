package com.foodmap.common.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import com.foodmap.common.validation.Check;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * HMAC Token 编解码器，统一认证服务签发逻辑和网关解析逻辑。
 *
 * <p>MVP 阶段使用轻量 HMAC 结构保证本地调试闭环。后续接入标准 JWT 库时，应保持本类对外方法稳定，
 * 让认证服务和网关仍共享同一套 Token 语义。</p>
 */
public class HmacTokenCodec {
    /**
     * HMAC 签名算法，排查签名不一致时需要确认认证服务和网关配置一致。
     */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * URL 安全 Base64 编码器，用于生成无填充 Token 片段。
     */
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    /**
     * URL 安全 Base64 解码器，用于读取 Token payload。
     */
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final String secret;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 使用指定密钥创建编解码器，认证服务和网关必须配置同一个密钥。
     *
     * @param secret HMAC 签名密钥。
     */
    public HmacTokenCodec(String secret) {
        this.secret = Check.notBlank("secret", secret);
    }

    /**
     * 签发 Access Token，新身份模型只把用户业务主键写入载荷。
     *
     * @param userId 用户业务主键。
     * @param expiresTime Access Token 过期时间。
     * @return 可返回给客户端使用的 Access Token。
     */
    public String issueAccessToken(Long userId, OffsetDateTime expiresTime) {
        return issue(TokenType.ACCESS, userId, expiresTime);
    }

    /**
     * 签发 Refresh Token，新身份模型只把用户业务主键写入载荷，并追加 nonce 降低碰撞和重放排查难度。
     *
     * @param userId 用户业务主键。
     * @param expiresTime Refresh Token 过期时间。
     * @return 可返回给客户端保存的 Refresh Token。
     */
    public String issueRefreshToken(Long userId, OffsetDateTime expiresTime) {
        return issue(TokenType.REFRESH, userId, expiresTime) + "." + UUID.randomUUID();
    }

    /**
     * 签发包含旧账号业务主键的 Access Token，仅用于 B1 旧身份模型兼容期。
     *
     * @param accountId 旧账号业务主键。
     * @param userId 用户业务主键。
     * @param expiresTime Access Token 过期时间。
     * @return 可返回给客户端使用的 Access Token。
     * @deprecated 新签发链路应使用 {@link #issueAccessToken(Long, OffsetDateTime)}，避免继续扩大 accountId 依赖。
     */
    @Deprecated
    public String issueAccessToken(Long accountId, Long userId, OffsetDateTime expiresTime) {
        return issueLegacy(TokenType.ACCESS, accountId, userId, expiresTime);
    }

    /**
     * 签发包含旧账号业务主键的 Refresh Token，仅用于 B1 旧身份模型兼容期。
     *
     * @param accountId 旧账号业务主键。
     * @param userId 用户业务主键。
     * @param expiresTime Refresh Token 过期时间。
     * @return 可返回给客户端保存的 Refresh Token。
     * @deprecated 新签发链路应使用 {@link #issueRefreshToken(Long, OffsetDateTime)}，避免继续扩大 accountId 依赖。
     */
    @Deprecated
    public String issueRefreshToken(Long accountId, Long userId, OffsetDateTime expiresTime) {
        return issueLegacy(TokenType.REFRESH, accountId, userId, expiresTime) + "." + UUID.randomUUID();
    }

    /**
     * 对明文 Token 做哈希摘要，用于数据库保存 Refresh Token 时避免落明文。
     *
     * @param token 待摘要的明文 Token。
     * @return Token 哈希摘要。
     */
    public String tokenHash(String token) {
        return sign(Check.notBlank("token", token));
    }

    /**
     * 解析并校验 Access Token，签名、类型或结构错误时抛出未认证异常。
     *
     * @param token 客户端提交的 Access Token。
     * @return 解析出的 Token 声明。
     */
    public TokenClaims parseAccessToken(String token) {
        TokenClaims claims = parse(token);
        if (claims.tokenType() != TokenType.ACCESS) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "Access Token 类型错误");
        }
        return claims;
    }

    /**
     * 解析并校验 Refresh Token，签名、类型或结构错误时抛出未认证异常。
     *
     * @param token 客户端提交的 Refresh Token。
     * @return 解析出的 Token 声明。
     */
    public TokenClaims parseRefreshToken(String token) {
        TokenClaims claims = parse(token);
        if (claims.tokenType() != TokenType.REFRESH) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "Refresh Token 类型错误");
        }
        return claims;
    }

    private String issue(TokenType tokenType, Long userId, OffsetDateTime expiresTime) {
        TokenClaims claims = new TokenClaims(tokenType, userId, expiresTime);
        String header = URL_ENCODER.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = URL_ENCODER.encodeToString(("{\"tokenType\":\"" + claims.tokenType().name()
                + "\",\"userId\":" + claims.userId()
                + ",\"expiresTime\":\"" + claims.expiresTime() + "\"}").getBytes(StandardCharsets.UTF_8));
        String unsignedToken = header + "." + payload;
        return unsignedToken + "." + sign(unsignedToken);
    }

    private String issueLegacy(TokenType tokenType, Long accountId, Long userId, OffsetDateTime expiresTime) {
        TokenClaims claims = new TokenClaims(tokenType, accountId, userId, expiresTime);
        String header = URL_ENCODER.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = URL_ENCODER.encodeToString(("{\"tokenType\":\"" + claims.tokenType().name()
                + "\",\"accountId\":" + claims.accountId()
                + ",\"userId\":" + claims.userId()
                + ",\"expiresTime\":\"" + claims.expiresTime() + "\"}").getBytes(StandardCharsets.UTF_8));
        String unsignedToken = header + "." + payload;
        return unsignedToken + "." + sign(unsignedToken);
    }

    private TokenClaims parse(String token) {
        String effectiveToken = Check.notBlank("token", token);
        String[] parts = effectiveToken.split("\\.");
        if (parts.length < 3) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "Token 结构错误");
        }
        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "Token 签名无效");
        }
        try {
            String payload = new String(URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(payload);
            JsonNode accountIdNode = root.get("accountId");
            Long accountId = accountIdNode == null || accountIdNode.isNull() ? null : accountIdNode.asLong();
            return new TokenClaims(
                    TokenType.valueOf(root.get("tokenType").asText()),
                    accountId,
                    root.get("userId").asLong(),
                    OffsetDateTime.parse(root.get("expiresTime").asText())
            );
        } catch (FoodMapException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "Token 载荷无效");
        }
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

    private boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        int result = leftBytes.length ^ rightBytes.length;
        int maxLength = Math.max(leftBytes.length, rightBytes.length);
        for (int index = 0; index < maxLength; index++) {
            byte leftValue = index < leftBytes.length ? leftBytes[index] : 0;
            byte rightValue = index < rightBytes.length ? rightBytes[index] : 0;
            result |= leftValue ^ rightValue;
        }
        return result == 0;
    }
}
