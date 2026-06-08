package com.foodmap.auth.domain;

/**
 * Refresh Token 状态，控制刷新令牌是否仍可用于续签 Access Token。
 */
public enum TokenStatus {

    /**
     * 有效令牌，可在未过期时刷新 Access Token。
     */
    ACTIVE,

    /**
     * 已撤销令牌，通常由退出登录、账号安全操作或服务端风控触发。
     */
    REVOKED,

    /**
     * 已过期令牌，不允许继续刷新访问凭证。
     */
    EXPIRED
}
