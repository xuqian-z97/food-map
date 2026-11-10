package com.foodmap.admin.security;

import java.time.OffsetDateTime;

/**
 * 验签通过的后台 Access Token，只承载 JWT 层事实，不包含数据库授权状态。
 *
 * @param adminUserId 后台管理员业务主键，来自 JWT sub 声明。
 * @param permissionVersion 签发时权限版本，来自 JWT permissionVersion 声明。
 * @param tokenId JWT jti，用于关联会话登记。
 * @param expiresTime Token 过期时间，来自 JWT exp 声明。
 */
public record VerifiedAdminToken(
        long adminUserId,
        long permissionVersion,
        String tokenId,
        OffsetDateTime expiresTime
) {
}
