package com.foodmap.auth.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

/**
 * 登录日志持久化实体，对应 `login_logs` 表，用于安全审计和异常登录排查。
 */
public class LoginLogEntity extends BaseEntity {
    /**
     * 登录日志业务主键。
     */
    private Long loginLogId;

    /**
     * 账号业务主键，登录失败且账号不存在时可为空。
     */
    private Long accountId;

    /**
     * 登录方式，如 PHONE、EMAIL、ACCOUNT_NAME。
     */
    private String loginType;

    /**
     * 登录结果，如 SUCCESS、FAILED。
     */
    private String loginResult;

    /**
     * 登录请求 IP 地址。
     */
    private String ipAddress;

    /**
     * 登录设备或浏览器 User-Agent。
     */
    private String userAgent;

    public Long getLoginLogId() {
        return loginLogId;
    }

    public void setLoginLogId(Long loginLogId) {
        this.loginLogId = loginLogId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getLoginType() {
        return loginType;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }

    public String getLoginResult() {
        return loginResult;
    }

    public void setLoginResult(String loginResult) {
        this.loginResult = loginResult;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
