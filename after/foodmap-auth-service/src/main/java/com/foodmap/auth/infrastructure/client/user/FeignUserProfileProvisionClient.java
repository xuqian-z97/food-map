package com.foodmap.auth.infrastructure.client.user;

import com.foodmap.auth.application.UserProfileProvisionClient;
import com.foodmap.auth.dto.RegisterRequest;
import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import feign.FeignException;
import feign.RetryableException;
import org.springframework.stereotype.Component;

/**
 * 基于 OpenFeign 的用户资料开通客户端适配器。
 *
 * <p>认证服务业务层只依赖 {@link UserProfileProvisionClient} 端口，本类负责把端口调用转换为
 * 面向 `foodmap-user-service` 的内部 Feign 调用。</p>
 */
@Component
public class FeignUserProfileProvisionClient implements UserProfileProvisionClient {
    private final UserProvisionFeignClient feignClient;

    /**
     * 创建用户资料开通 Feign 适配器。
     *
     * @param feignClient 用户服务内部 Feign 客户端。
     */
    public FeignUserProfileProvisionClient(UserProvisionFeignClient feignClient) {
        this.feignClient = feignClient;
    }

    /**
     * 调用用户服务内部接口开通注册用户资料。
     *
     * @param accountId 认证账号业务主键。
     * @param userId 用户业务主键。
     * @param request 注册请求原始 DTO，用于传递初始昵称。
     */
    @Override
    public void provision(Long accountId, Long userId, RegisterRequest request) {
        try {
            var response = feignClient.provision(new UserProvisionFeignRequest(accountId, userId, request.nickname()));
            if (response == null || !response.success()) {
                throw new FoodMapException(CommonErrorCode.BAD_GATEWAY, "用户服务资料开通失败");
            }
        } catch (RetryableException ex) {
            throw new FoodMapException(CommonErrorCode.GATEWAY_TIMEOUT, "用户服务资料开通超时", ex);
        } catch (FeignException ex) {
            throw new FoodMapException(CommonErrorCode.BAD_GATEWAY, "用户服务资料开通失败", ex);
        }
    }
}
