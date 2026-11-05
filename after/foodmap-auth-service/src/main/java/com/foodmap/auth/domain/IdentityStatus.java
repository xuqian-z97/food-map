package com.foodmap.auth.domain;

/**
 * 登录身份绑定状态，控制绑定关系是否还能参与登录和身份查找。
 */
public enum IdentityStatus {

    /**
     * 正常状态，登录标识可以用于注册校验、登录定位和安全设置展示。
     */
    NORMAL,

    /**
     * 已禁用，保留绑定关系但拒绝该身份继续登录，通常由风控或后台运营触发。
     */
    DISABLED,

    /**
     * 已解绑，表示用户主动解除该登录标识，历史记录仅用于审计。
     */
    UNBOUND
}
