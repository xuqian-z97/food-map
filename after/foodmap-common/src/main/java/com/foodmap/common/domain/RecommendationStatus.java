package com.foodmap.common.domain;

/**
 * 推荐内容状态，用于推荐服务和社区统计服务判断内容是否可展示、可统计。
 *
 * <p>排查公开统计异常时，应同时检查推荐的 {@link RecommendationStatus} 和可见范围。</p>
 */
public enum RecommendationStatus {
    /**
     * 正常状态，表示内容可按可见范围展示；只有该状态的 PUBLIC 推荐允许进入社区统计。
     */
    NORMAL,
    /**
     * 待审核状态，预留给公开内容审核流程；该状态不应进入公开社区统计。
     */
    PENDING_REVIEW,
    /**
     * 已隐藏状态，通常由审核、举报或管理操作触发；该状态不应对普通用户展示。
     */
    HIDDEN,
    /**
     * 已删除状态，对应业务逻辑删除；查询业务数据时必须排除该状态。
     */
    DELETED
}
