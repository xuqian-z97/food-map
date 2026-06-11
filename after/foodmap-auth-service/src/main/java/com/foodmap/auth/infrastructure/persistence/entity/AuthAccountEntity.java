package com.foodmap.auth.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

import java.time.OffsetDateTime;

/**
 * 认证账号持久化实体，对应 `auth_accounts` 表，不能作为 API 响应直接返回。
 */
public class AuthAccountEntity extends BaseEntity {
    /**
     * 账号业务主键，用于认证服务对外引用账号。
     */
    private Long accountId;

    /**
     * 用户业务主键，关联用户服务的用户身份。
     */
    private Long userId;

    /**
     * 账号名，可用于账号名登录。
     */
    private String accountName;

    /**
     * 手机号，可用于手机号登录。
     */
    private String phone;

    /**
     * 邮箱，可用于邮箱登录。
     */
    private String email;

    /**
     * 账号状态，如 NORMAL、DISABLED、LOCKED。
     */
    private String accountStatus;

    /**
     * 注册来源，如 IOS、WEB、ADMIN。
     */
    private String registeredChannel;

    /**
     * 最近一次登录成功时间。
     */
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
