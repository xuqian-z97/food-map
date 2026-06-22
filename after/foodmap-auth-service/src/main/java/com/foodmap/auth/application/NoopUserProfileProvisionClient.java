package com.foodmap.auth.application;

import com.foodmap.auth.dto.RegisterRequest;

/**
 * 用户资料开通空实现，用于单元测试或临时替身场景保持注册链路可测试。
 */
public class NoopUserProfileProvisionClient implements UserProfileProvisionClient {

    /**
     * 当前不执行远程调用，仅保留注册到用户服务联动的稳定扩展点。
     *
     * @param accountId 认证账号业务主键。
     * @param userId 用户业务主键。
     * @param request 注册请求原始 DTO，用于后续创建用户资料。
     */
    @Override
    public void provision(Long accountId, Long userId, RegisterRequest request) {
        // 测试替身不执行远程调用。
    }
}
