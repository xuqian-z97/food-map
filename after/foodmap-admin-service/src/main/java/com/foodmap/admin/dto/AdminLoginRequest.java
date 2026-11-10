package com.foodmap.admin.dto;

/**
 * 后台登录请求 DTO。
 *
 * @param username 后台登录账号名，必填，最长 64 字符。
 * @param password 后台登录密码，必填，原样校验。
 */
public record AdminLoginRequest(String username, String password) {

    /**
     * 调试输出屏蔽密码，避免请求 DTO 进入日志时泄露原始凭证。
     *
     * @return 屏蔽密码后的文本。
     */
    @Override
    public String toString() {
        return "AdminLoginRequest{username=" + username + ", password=****}";
    }
}
