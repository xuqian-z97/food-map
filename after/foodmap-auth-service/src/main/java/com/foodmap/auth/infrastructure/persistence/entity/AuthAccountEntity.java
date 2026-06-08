package com.foodmap.auth.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

import java.time.OffsetDateTime;

/**
 * 认证账号持久化实体，对应 `auth_accounts` 表，不能作为 API 响应直接返回。
 */
public class AuthAccountEntity extends BaseEntity {
    private Long accountId;
    private Long userId;
    private String accountName;
    private String phone;
    private String email;
    private String accountStatus;
    private String registeredChannel;
    private OffsetDateTime lastLoginTime;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getRegisteredChannel() {
        return registeredChannel;
    }

    public void setRegisteredChannel(String registeredChannel) {
        this.registeredChannel = registeredChannel;
    }

    public OffsetDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(OffsetDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }
}
