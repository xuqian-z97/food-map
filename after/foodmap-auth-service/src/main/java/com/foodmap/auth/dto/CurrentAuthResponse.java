package com.foodmap.auth.dto;

import java.time.OffsetDateTime;

/**
 * 当前认证会话响应，供前端确认本地 Access Token 对应的用户身份。
 *
 * <p>`accountId` 仅作为 B1 旧身份模型兼容字段保留，新链路返回 null，客户端不得继续依赖。</p>
 */
public record CurrentAuthResponse(
        @Deprecated
        Long accountId,
        Long userId,
        OffsetDateTime accessExpiresTime
) {
}
