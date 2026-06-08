package com.foodmap.user.dto;

/**
 * 当前用户资料响应 DTO，只返回前端需要展示的安全字段。
 */
public record CurrentUserResponse(
        Long userId,
        Long accountId,
        String accountName,
        String nickname,
        Long avatarMediaId,
        String userStatus
) {
}
