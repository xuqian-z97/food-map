package com.foodmap.user.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

import java.time.LocalDate;

/**
 * 用户资料持久化实体，对应 `user_profiles` 表，承载城市、简介等展示扩展字段。
 */
public class UserProfileEntity extends BaseEntity {
    /**
     * 用户资料业务主键。
     */
    private Long profileId;

    /**
     * 用户业务主键，关联 users.user_id。
     */
    private Long userId;

    /**
     * 用户所在城市编码。
     */
    private String cityCode;

    /**
     * 用户所在城市名称。
     */
    private String cityName;

    /**
     * 用户个人简介。
     */
    private String bio;

    /**
     * 性别，可选字段。
     */
    private String gender;

    /**
     * 生日，可选字段。
     */
    private LocalDate birthday;

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }
}
