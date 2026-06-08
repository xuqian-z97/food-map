package com.foodmap.auth.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

/**
 * 登录日志持久化实体，对应 `login_logs` 表，用于安全审计和异常登录排查。
 */
public class LoginLogEntity extends BaseEntity {
    private Long loginLogId;
    private Long accountId;
    private String loginType;
    private String loginResult;
    private String ipAddress;
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
