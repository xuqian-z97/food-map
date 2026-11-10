package com.foodmap.admin.security;

import java.util.Set;

/**
 * 已认证的后台管理员请求上下文，只由后台认证链路构造并放入请求属性，不接受请求 JSON 反序列化。
 *
 * @param adminUserId 后台管理员业务主键，来自验签后的 Token 会话事实。
 * @param tokenId 后台 Access Token 的 JWT jti，用于登出和审计关联。
 * @param permissionVersion 请求认证时数据库中的当前权限版本。
 * @param permissions 请求认证时数据库中的有效权限编码集合，权限为空代表已认证但无权限。
 */
public record AuthenticatedAdmin(
        Long adminUserId,
        String tokenId,
        long permissionVersion,
        Set<String> permissions
) {
}
