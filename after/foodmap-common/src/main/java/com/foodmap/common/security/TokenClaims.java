package com.foodmap.common.security;

import com.foodmap.common.validation.Check;

import java.time.OffsetDateTime;

/**
 * Token 载荷声明，承载网关和认证服务共同理解的用户身份和过期时间。
 *
 * <p>`accountId` 仅用于 B1 旧身份模型兼容，新签发 Token 应只依赖 `userId`。</p>
 */
public record TokenClaims(
        TokenType tokenType,
        @Deprecated
        Long accountId,
        Long userId,
        OffsetDateTime expiresTime
) {
    /**
     * 创建 userId-only Token 声明。
     *
     * @param tokenType Token 类型。
     * @param userId 用户业务主键。
     * @param expiresTime Token 过期时间。
     */
    public TokenClaims(TokenType tokenType, Long userId, OffsetDateTime expiresTime) {
        this(tokenType, null, userId, expiresTime);
    }

    /**
     * 创建 Token 声明并校验关键身份字段，避免无效 Token 进入业务链路。
     */
    public TokenClaims {
        if (tokenType == null) {
            throw new IllegalArgumentException("tokenType must not be null");
        }
        if (accountId != null) {
            Check.positive("accountId", accountId);
        }
        Check.positive("userId", userId);
        if (expiresTime == null) {
            throw new IllegalArgumentException("expiresTime must not be null");
        }
    }

    /**
     * 判断 Token 在指定时间点是否已过期，网关和认证服务都应使用同一逻辑。
     *
     * @param checkedTime 用于判断过期状态的时间点，空值时使用当前时间。
     * @return 如果 Token 已经过期则返回 true。
     */
    public boolean isExpiredAt(OffsetDateTime checkedTime) {
        OffsetDateTime effectiveCheckedTime = checkedTime == null ? OffsetDateTime.now() : checkedTime;
        return !expiresTime.isAfter(effectiveCheckedTime);
    }
}
