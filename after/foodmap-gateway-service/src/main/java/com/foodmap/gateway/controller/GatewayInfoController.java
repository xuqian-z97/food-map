package com.foodmap.gateway.controller;

import com.foodmap.common.api.ApiResponse;
import com.foodmap.common.service.ServiceInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 网关服务内部健康检查接口，用于本地启动和服务联通性验证。
 */
@RestController
public class GatewayInfoController {
    /**
     * 返回网关服务进程级健康状态。
     *
     * @return 网关服务健康响应。
     */
    @GetMapping("/internal/gateway/health")
    public ApiResponse<ServiceInfo> health() {
        return ApiResponse.ok(ServiceInfo.up("foodmap-gateway-service"));
    }
}
