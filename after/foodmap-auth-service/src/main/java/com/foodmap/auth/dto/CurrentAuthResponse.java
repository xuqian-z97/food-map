package com.foodmap.auth.dto;

import java.time.OffsetDateTime;

/**
 * 当前认证会话响应，供前端确认本地 Access Token 对应的账号和用户身份。
 */
public record CurrentAuthResponse(
        Long accountId,
        Long userId,
        OffsetDateTime accessExpiresTime
) {
}
