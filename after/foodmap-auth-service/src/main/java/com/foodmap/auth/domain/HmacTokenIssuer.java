package com.foodmap.auth.domain;

import com.foodmap.common.security.HmacTokenCodec;
import com.foodmap.common.security.TokenClaims;
import com.foodmap.common.validation.Check;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * HMAC Token 签发器，用于 MVP 阶段生成可验证结构的 Access Token 和 Refresh Token。
 *
 * <p>本类保留在认证领域层作为应用层依赖入口，底层编解码逻辑委托给 common 中的 HmacTokenCodec，
 * 确保认证服务签发逻辑和网关解析逻辑保持一致。</p>
 */
@Component
public class HmacTokenIssuer {
    private final HmacTokenCodec tokenCodec;

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
        this.tokenCodec = new HmacTokenCodec(Check.notBlank("secret", secret));
    }

    /**
     * 签发 Access Token，载荷包含账号和用户业务主键，供网关和服务端提取身份。
     */
    public String issueAccessToken(Long accountId, Long userId, OffsetDateTime expiresTime) {
        return tokenCodec.issueAccessToken(accountId, userId, expiresTime);
    }

    /**
     * 签发 Refresh Token，载荷包含随机 nonce，降低刷新令牌碰撞和重放排查难度。
     */
    public String issueRefreshToken(Long accountId, Long userId, OffsetDateTime expiresTime) {
        return tokenCodec.issueRefreshToken(accountId, userId, expiresTime);
    }

    /**
     * 对明文 Token 做哈希摘要，用于数据库保存 Refresh Token 时避免落明文。
     */
    public String tokenHash(String token) {
        return tokenCodec.tokenHash(token);
    }

    /**
     * 解析 Access Token，供认证服务的会话查询接口复用网关同款校验逻辑。
     */
    public TokenClaims parseAccessToken(String token) {
        return tokenCodec.parseAccessToken(token);
    }

    /**
     * 解析 Refresh Token，供刷新和退出登录接口校验令牌类型、签名和身份声明。
     */
    public TokenClaims parseRefreshToken(String token) {
        return tokenCodec.parseRefreshToken(token);
    }
}
