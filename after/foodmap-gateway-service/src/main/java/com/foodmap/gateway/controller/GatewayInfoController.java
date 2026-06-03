package com.foodmap.gateway.controller;

import com.foodmap.common.api.ApiResponse;
import com.foodmap.common.service.ServiceInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayInfoController {
    @GetMapping("/internal/gateway/health")
    public ApiResponse<ServiceInfo> health() {
        return ApiResponse.ok(ServiceInfo.up("foodmap-gateway-service"));
    }
}
