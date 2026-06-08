package com.foodmap.auth.dto;

import com.foodmap.common.validation.Check;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求 DTO，只承载 API 入参，不包含数据库内部主键和密码哈希。
 */
public record RegisterRequest(
        @NotBlank @Size(max = 64) String accountName,
        @Size(max = 32) String phone,
        @Email @Size(max = 128) String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Size(max = 64) String nickname,
        @Size(max = 32) String registeredChannel
) {
    /**
     * 归一化注册请求中的必填文本，避免应用层重复处理空白账号名和昵称。
     */
    public RegisterRequest {
        accountName = Check.notBlank("accountName", accountName);
        password = Check.notBlank("password", password);
        nickname = Check.notBlank("nickname", nickname);
        if (phone != null) {
            phone = phone.trim();
        }
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        if (registeredChannel != null) {
            registeredChannel = registeredChannel.trim().toUpperCase();
        }
    }
}
