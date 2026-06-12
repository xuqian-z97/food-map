package com.foodmap.admin.dto;

/**
 * 创建后台管理员请求 DTO，密码只允许进入服务层做哈希处理，不能写入日志或响应。
 *
 * @param username 后台登录账号名。
 * @param password 原始登录密码。
 * @param displayName 后台展示名称。
 * @param mobile 后台管理员手机号，可选。
 * @param email 后台管理员邮箱，可选。
 */
public record CreateAdminUserRequest(
        String username,
        String password,
        String displayName,
        String mobile,
        String email
) {
}
