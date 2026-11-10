package com.foodmap.admin.dto;

import java.time.OffsetDateTime;

/**
 * 后台登录响应 DTO，accessToken 不进入访问日志、响应体采集或测试证据。
 *
 * @param accessToken 后台 Access Token 文本。
 * @param tokenType Token 类型，固定 Bearer。
 * @param expiresTime Token 过期时间，带时区 ISO 8601。
 */
public record AdminLoginResponse(String accessToken, String tokenType, OffsetDateTime expiresTime) {

    /**
     * 调试输出屏蔽 accessToken，避免响应 DTO 进入日志时泄露凭证。
     *
     * @return 屏蔽 Token 后的文本。
     */
    @Override
    public String toString() {
        return "AdminLoginResponse{accessToken=****, tokenType=" + tokenType + ", expiresTime=" + expiresTime + "}";
    }
}
