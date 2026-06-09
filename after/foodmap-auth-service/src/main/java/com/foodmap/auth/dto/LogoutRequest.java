package com.foodmap.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 退出登录请求，服务端根据 Refresh Token 哈希撤销刷新能力。
 */
public record LogoutRequest(
        @NotBlank(message = "Refresh Token不能为空")
        String refreshToken
) {
}
