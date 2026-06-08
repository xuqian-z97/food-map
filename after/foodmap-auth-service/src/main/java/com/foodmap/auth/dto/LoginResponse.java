package com.foodmap.auth.dto;

import java.time.OffsetDateTime;

/**
 * 登录响应 DTO，向客户端返回 Token 和跨服务业务主键，不暴露数据库实体或 Token 哈希。
 */
public record LoginResponse(
        Long accountId,
        Long userId,
        String accessToken,
        String refreshToken,
        OffsetDateTime accessTokenExpiresTime,
        OffsetDateTime refreshTokenExpiresTime
) {
}
