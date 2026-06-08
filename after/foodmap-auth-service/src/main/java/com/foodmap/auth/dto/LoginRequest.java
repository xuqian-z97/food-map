package com.foodmap.auth.dto;

import com.foodmap.common.validation.Check;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求 DTO，支持账号名、手机号或邮箱作为统一登录标识。
 */
public record LoginRequest(
        @NotBlank @Size(max = 128) String loginIdentifier,
        @NotBlank @Size(max = 128) String password
) {
    /**
     * 归一化登录标识，避免同一账号因为首尾空格导致无法定位。
     */
    public LoginRequest {
        loginIdentifier = Check.notBlank("loginIdentifier", loginIdentifier);
        password = Check.notBlank("password", password);
    }
}
