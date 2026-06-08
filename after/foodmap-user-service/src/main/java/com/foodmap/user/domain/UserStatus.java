package com.foodmap.user.domain;

/**
 * 用户资料状态，控制用户资料是否可用于搜索、展示和后续业务访问。
 */
public enum UserStatus {

    /**
     * 正常用户，可被授权查看资料并参与 FoodMap 业务流程。
     */
    NORMAL,

    /**
     * 已禁用用户，通常由安全、注销或管理操作触发，排查资料不可见时应关注该状态。
     */
    DISABLED
}
