package com.foodmap.auth.infrastructure.client.user;

import com.foodmap.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 用户服务内部资料开通 Feign 客户端，通过 Nacos 服务名定位 `foodmap-user-service`。
 */
@FeignClient(name = "foodmap-user-service", url = "${foodmap.auth.user-service.url:}")
public interface UserProvisionFeignClient {

    /**
     * 调用用户服务内部开档接口。
     *
     * @param request 用户资料开通请求。
     * @return 用户服务统一响应。
     */
    @PostMapping(value = "/internal/users/provision", consumes = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<Object> provision(@RequestBody UserProvisionFeignRequest request);
}
