package com.foodmap.admin.application.port;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * 后台认证授权快照，是后台受保护请求一次读取的会话与管理员授权投影，只承载事实字段不做判定。
 *
 * @param adminUserId 后台管理员业务主键。
 * @param adminStatus 管理员状态，如 ACTIVE、DISABLED、LOCKED。
 * @param permissionVersion 当前权限版本，角色或权限变更后递增。
 * @param expiresTime 会话过期时间，与 Token 过期时间一致。
 * @param revokedTime 会话撤销时间，未撤销时为 null。
 * @param permissionCodes 当前有效权限编码集合，会话有效但无权限时为空集合。
 */
public record AdminAuthorizationSnapshot(
        Long adminUserId,
        String adminStatus,
        Long permissionVersion,
        OffsetDateTime expiresTime,
        OffsetDateTime revokedTime,
        Set<String> permissionCodes
) {
}
