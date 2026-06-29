package com.foodmap.auth.dto;

import java.time.OffsetDateTime;

/**
 * 登录响应 DTO，向客户端返回 Token 和当前用户业务主键，不暴露数据库实体或 Token 哈希。
 *
 * <p>`accountId` 仅作为 B1 旧身份模型兼容字段保留，新链路返回 null，客户端不得继续依赖。</p>
 */
public record LoginResponse(
        @Deprecated
        Long accountId,
        Long userId,
        String accessToken,
        String refreshToken,
        OffsetDateTime accessTokenExpiresTime,
        OffsetDateTime refreshTokenExpiresTime
) {
}
