package com.foodmap.auth.application;

import com.foodmap.auth.dto.RegisterRequest;

/**
 * 用户资料开通客户端，认证服务注册成功后通过该边界通知用户服务创建资料。
 *
 * <p>MVP 当前使用空实现保留边界，后续替换为内部 HTTP 客户端或领域事件时不改注册用例。</p>
 */
public interface UserProfileProvisionClient {

    /**
     * 为新注册账号开通用户资料。实现方必须使用用户业务主键而不是数据库自增主键。
     */
    void provision(Long accountId, Long userId, RegisterRequest request);
}
