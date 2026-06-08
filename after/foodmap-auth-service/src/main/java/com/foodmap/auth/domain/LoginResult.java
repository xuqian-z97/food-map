package com.foodmap.auth.domain;

/**
 * 登录结果，用于登录日志、安全审计和异常登录排查。
 */
public enum LoginResult {

    /**
     * 登录成功，表示账号存在、凭证匹配且账号状态允许登录。
     */
    SUCCESS,

    /**
     * 登录失败，排查时结合登录方式、账号状态和错误码定位具体原因。
     */
    FAILED
}
