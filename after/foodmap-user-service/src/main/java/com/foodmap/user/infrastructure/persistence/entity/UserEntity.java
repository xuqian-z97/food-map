package com.foodmap.user.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

/**
 * 用户主表持久化实体，对应 `users` 表，不能作为 API 响应直接返回。
 */
public class UserEntity extends BaseEntity {
    /**
     * 用户业务主键，用于跨服务引用用户。
     */
    private Long userId;

    /**
     * 认证账号业务主键，来源于认证服务。
     */
    private Long accountId;

    /**
     * 用户昵称。
     */
    private String nickname;

    /**
     * 头像媒体业务主键，关联媒体服务 media_id。
     */
    private Long avatarMediaId;

    /**
     * 用户状态，如 NORMAL、DISABLED。
     */
    private String userStatus;

    /**
     * 是否允许被搜索，1 表示允许，0 表示不允许。
     */
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
