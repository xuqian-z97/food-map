package com.foodmap.common.security;

/**
 * 当前登录用户上下文，供写接口和权限校验从服务端可信上下文读取身份。
 *
 * <p>这里使用用户 BIGINT 业务主键，不使用数据库自增主键，也不接受客户端传入的用户身份作为事实来源。
 * `accountId` 仅用于 B1 旧身份模型兼容，新业务不得依赖。</p>
 */
public record CurrentUser(
        Long userId,
        @Deprecated
        Long accountId,
        String accountName
) {
}
