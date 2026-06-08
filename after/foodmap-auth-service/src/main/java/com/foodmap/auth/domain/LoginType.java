package com.foodmap.auth.domain;

/**
 * 登录标识类型，用于记录用户本次使用哪类标识完成登录。
 */
public enum LoginType {

    /**
     * 账号名登录，适合用户主动设置的 FoodMap 账号名称。
     */
    ACCOUNT_NAME,

    /**
     * 手机号登录，排查时注意日志必须脱敏手机号。
     */
    PHONE,

    /**
     * 邮箱登录，排查时注意日志必须脱敏邮箱。
     */
    EMAIL
}
