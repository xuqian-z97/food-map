package com.foodmap.user.dto;

import com.foodmap.common.validation.Check;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 用户资料开通请求 DTO，由认证服务在注册成功后通过内部接口调用。
 */
public record ProvisionUserRequest(
        @NotNull @Positive Long accountId,
        @NotNull @Positive Long userId,
        @NotBlank @Size(max = 64) String nickname
) {
    /**
     * 归一化内部开通请求，避免服务层重复处理基础主键和昵称校验。
     */
    public ProvisionUserRequest {
        accountId = Check.positive("accountId", accountId);
        userId = Check.positive("userId", userId);
        nickname = Check.notBlank("nickname", nickname);
    }
}
