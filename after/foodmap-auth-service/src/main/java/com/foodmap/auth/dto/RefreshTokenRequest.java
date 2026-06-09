package com.foodmap.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh Token 刷新请求，客户端只提交明文 Refresh Token，服务端按哈希查询和校验状态。
 */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh Token不能为空")
        String refreshToken
) {
}
