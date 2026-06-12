package com.foodmap.admin.dto;

/**
 * 后台管理员响应 DTO，只返回后台页面需要展示的脱敏字段，不暴露密码哈希和数据库内部主键。
 *
 * @param adminUserId 后台管理员业务主键。
 * @param username 后台登录账号名。
 * @param displayName 后台展示名称。
 * @param mobileMasked 脱敏手机号。
 * @param emailMasked 脱敏邮箱。
 * @param adminStatus 管理员状态。
 * @param permissionVersion 权限版本。
 */
public record AdminUserResponse(
        Long adminUserId,
        String username,
        String displayName,
        String mobileMasked,
        String emailMasked,
        String adminStatus,
        Long permissionVersion
) {
}
