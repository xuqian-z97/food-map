package com.foodmap.common.security;

import com.foodmap.common.validation.Check;

import java.time.OffsetDateTime;

/**
 * Token 载荷声明，承载网关和认证服务共同理解的账号身份、用户身份和过期时间。
 */
public record TokenClaims(
        TokenType tokenType,
        Long accountId,
        Long userId,
        OffsetDateTime expiresTime
) {
    /**
     * 创建 Token 声明并校验关键身份字段，避免无效 Token 进入业务链路。
     */
    public TokenClaims {
        if (tokenType == null) {
            throw new IllegalArgumentException("tokenType must not be null");
        }
        Check.positive("accountId", accountId);
        Check.positive("userId", userId);
        if (expiresTime == null) {
            throw new IllegalArgumentException("expiresTime must not be null");
        }
    }

    /**
     * 判断 Token 在指定时间点是否已过期，网关和认证服务都应使用同一逻辑。
     */
    public boolean isExpiredAt(OffsetDateTime checkedTime) {
        OffsetDateTime effectiveCheckedTime = checkedTime == null ? OffsetDateTime.now() : checkedTime;
        return !expiresTime.isAfter(effectiveCheckedTime);
    }
}
