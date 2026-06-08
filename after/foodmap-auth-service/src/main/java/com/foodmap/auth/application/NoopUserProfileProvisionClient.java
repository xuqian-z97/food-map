package com.foodmap.auth.application;

import com.foodmap.auth.dto.RegisterRequest;
import org.springframework.stereotype.Component;

/**
 * 用户资料开通空实现，用于在用户服务联动未落地前保持注册链路可测试。
 */
@Component
public class NoopUserProfileProvisionClient implements UserProfileProvisionClient {

    /**
     * 当前不执行远程调用，仅保留注册到用户服务联动的稳定扩展点。
     */
    @Override
    public void provision(Long accountId, Long userId, RegisterRequest request) {
        // MVP 骨架阶段暂不跨服务写用户资料，后续替换为内部 API 或 MQ 事件。
    }
}
