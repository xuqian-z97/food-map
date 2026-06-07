package com.foodmap.common.security;

/**
 * 当前登录用户上下文，供写接口和权限校验从服务端可信上下文读取身份。
 *
 * <p>这里使用 BIGINT 业务主键，不使用数据库自增主键，也不接受客户端传入的用户身份作为事实来源。</p>
 */
public record CurrentUser(
        Long userId,
        Long accountId,
        String accountName
) {
}
