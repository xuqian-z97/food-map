package com.foodmap.auth.domain;

/**
 * 注册来源渠道，用于后续统计用户来源和排查不同入口的注册问题。
 */
public enum RegisteredChannel {

    /**
     * iOS App 注册，是 FoodMap MVP 的主要注册来源。
     */
    IOS,

    /**
     * Web 注册，预留给后续管理后台或网页入口。
     */
    WEB,

    /**
     * 管理后台创建账号，预留给后续运营或测试账号场景。
     */
    ADMIN
}
