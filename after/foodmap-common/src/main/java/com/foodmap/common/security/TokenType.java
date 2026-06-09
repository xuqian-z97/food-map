package com.foodmap.common.security;

/**
 * FoodMap Token 类型，用于区分短期访问令牌和长期刷新令牌。
 */
public enum TokenType {

    /**
     * Access Token，用于访问受保护 API，过期时间较短，由网关解析并透传用户身份。
     */
    ACCESS,

    /**
     * Refresh Token，用于刷新 Access Token 和退出登录撤销，必须以哈希形式保存到数据库。
     */
    REFRESH
}
