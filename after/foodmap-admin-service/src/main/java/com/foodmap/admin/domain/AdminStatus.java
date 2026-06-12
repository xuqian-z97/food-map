package com.foodmap.admin.domain;

/**
 * 后台管理员状态，用于后台登录鉴权和高风险操作权限判断。
 */
public enum AdminStatus {
    /**
     * 正常状态，允许后台登录和执行已授权操作。
     */
    ACTIVE,
    /**
     * 已禁用状态，不允许登录后台，排查时应查看禁用操作审计。
     */
    DISABLED,
    /**
     * 已锁定状态，通常由连续登录失败或安全策略触发。
     */
    LOCKED
}
