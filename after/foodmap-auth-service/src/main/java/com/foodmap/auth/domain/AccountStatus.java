package com.foodmap.auth.domain;

/**
 * 认证账号状态，控制账号是否允许登录和签发 Token。
 */
public enum AccountStatus {

    /**
     * 正常账号，可完成登录、刷新 Token 和发起后续业务请求。
     */
    NORMAL,

    /**
     * 已禁用账号，通常由风控、用户注销或管理员处理触发，排查登录拒绝时应关注该状态。
     */
    DISABLED,

    /**
     * 已锁定账号，通常由连续登录失败或安全策略触发，排查短时间登录失败时应关注该状态。
     */
    LOCKED
}
