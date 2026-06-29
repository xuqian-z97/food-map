package com.foodmap.user.dto;

/**
 * 当前用户资料响应 DTO，只返回前端需要展示的安全字段。
 *
 * <p>`accountId` 仅作为 B1 旧身份模型兼容字段保留，当前用户查询新链路返回 null，客户端不得继续依赖。</p>
 */
public record CurrentUserResponse(
        Long userId,
        @Deprecated
        Long accountId,
        String accountName,
        String nickname,
        Long avatarMediaId,
        String userStatus
) {
}
