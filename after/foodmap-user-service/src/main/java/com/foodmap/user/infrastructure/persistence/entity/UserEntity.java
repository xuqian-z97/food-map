package com.foodmap.user.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

/**
 * 用户主表持久化实体，对应 `users` 表，不能作为 API 响应直接返回。
 */
public class UserEntity extends BaseEntity {
    private Long userId;
    private Long accountId;
    private String nickname;
    private Long avatarMediaId;
    private String userStatus;
    private Short searchable;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Long getAvatarMediaId() {
        return avatarMediaId;
    }

    public void setAvatarMediaId(Long avatarMediaId) {
        this.avatarMediaId = avatarMediaId;
    }

    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }

    public Short getSearchable() {
        return searchable;
    }

    public void setSearchable(Short searchable) {
        this.searchable = searchable;
    }
}
