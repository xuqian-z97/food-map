package com.foodmap.auth.dto;

/**
 * 注册响应 DTO，只返回当前标准用户身份和可安全展示的账号状态。
 *
 * <p>`accountId` 仅作为 B1 旧身份模型兼容字段保留，新链路返回 null，客户端不得继续依赖。</p>
 */
public record RegisterResponse(
        @Deprecated
        Long accountId,
        Long userId,
        String accountStatus
) {
}
