package com.foodmap.user.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

/**
 * 用户设置持久化实体，对应 `user_settings` 表，承载隐私和默认可见范围偏好。
 */
public class UserSettingEntity extends BaseEntity {
    /**
     * 用户设置业务主键。
     */
    private Long settingId;

    /**
     * 用户业务主键，关联 users.user_id。
     */
    private Long userId;

    /**
     * 默认推荐可见范围，如 PRIVATE、FRIENDS、PUBLIC。
     */
    private String defaultVisibilityType;

    /**
     * 是否允许收到好友申请，1 表示允许，0 表示不允许。
     */
    private Short allowFriendRequest;

    /**
     * 是否允许通过手机号搜索到本人。
     */
    private Short allowSearchByPhone;

    /**
     * 是否允许通过邮箱搜索到本人。
     */
    private Short allowSearchByEmail;

    public Long getSettingId() {
        return settingId;
    }

    public void setSettingId(Long settingId) {
        this.settingId = settingId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDefaultVisibilityType() {
        return defaultVisibilityType;
    }

    public void setDefaultVisibilityType(String defaultVisibilityType) {
        this.defaultVisibilityType = defaultVisibilityType;
    }

    public Short getAllowFriendRequest() {
        return allowFriendRequest;
    }

    public void setAllowFriendRequest(Short allowFriendRequest) {
        this.allowFriendRequest = allowFriendRequest;
    }

    public Short getAllowSearchByPhone() {
        return allowSearchByPhone;
    }

    public void setAllowSearchByPhone(Short allowSearchByPhone) {
        this.allowSearchByPhone = allowSearchByPhone;
    }

    public Short getAllowSearchByEmail() {
        return allowSearchByEmail;
    }

    public void setAllowSearchByEmail(Short allowSearchByEmail) {
        this.allowSearchByEmail = allowSearchByEmail;
    }
}
