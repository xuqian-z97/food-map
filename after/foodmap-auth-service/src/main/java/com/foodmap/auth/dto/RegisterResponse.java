package com.foodmap.auth.dto;

/**
 * 注册响应 DTO，只返回跨服务业务主键和可安全展示的账号状态。
 */
public record RegisterResponse(
        Long accountId,
        Long userId,
        String accountStatus
) {
}
