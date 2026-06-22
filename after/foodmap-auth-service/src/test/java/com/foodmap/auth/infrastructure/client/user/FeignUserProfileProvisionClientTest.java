package com.foodmap.auth.infrastructure.client.user;

import com.foodmap.auth.dto.RegisterRequest;
import com.foodmap.common.api.ApiResponse;
import com.foodmap.common.exception.FoodMapException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeignUserProfileProvisionClientTest {

    @Test
    void delegatesProvisionPayloadToUserServiceFeignClient() {
        CapturingUserProvisionFeignClient feignClient = new CapturingUserProvisionFeignClient(ApiResponse.ok(new Object()));
        FeignUserProfileProvisionClient client = new FeignUserProfileProvisionClient(feignClient);

        client.provision(100001L, 200001L, new RegisterRequest(
                "foodie_01",
                "13800138000",
                "foodie@example.com",
                "secret123",
                "小张",
                "IOS"
        ));

        assertThat(feignClient.request.accountId()).isEqualTo(100001L);
        assertThat(feignClient.request.userId()).isEqualTo(200001L);
        assertThat(feignClient.request.nickname()).isEqualTo("小张");
    }

    @Test
    void throwsBadGatewayWhenUserServiceReturnsFailureResponse() {
        CapturingUserProvisionFeignClient feignClient = new CapturingUserProvisionFeignClient(
                ApiResponse.fail(500, "INTERNAL_ERROR", "用户服务异常")
        );
        FeignUserProfileProvisionClient client = new FeignUserProfileProvisionClient(feignClient);

        assertThatThrownBy(() -> client.provision(100001L, 200001L, new RegisterRequest(
                "foodie_01",
                "13800138000",
                "foodie@example.com",
                "secret123",
                "小张",
                "IOS"
        )))
                .isInstanceOf(FoodMapException.class)
                .satisfies(throwable -> assertThat(((FoodMapException) throwable).code()).isEqualTo("BAD_GATEWAY"));
    }

    private static class CapturingUserProvisionFeignClient implements UserProvisionFeignClient {
        private final ApiResponse<Object> response;
        private UserProvisionFeignRequest request;

        private CapturingUserProvisionFeignClient(ApiResponse<Object> response) {
            this.response = response;
        }

        @Override
        public ApiResponse<Object> provision(UserProvisionFeignRequest request) {
            this.request = request;
            return response;
        }
    }
}
