package com.foodmap.recommendation.controller;

import com.foodmap.common.api.ApiResponse;
import com.foodmap.common.service.ServiceInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 推荐服务内部健康检查接口，用于网关路由和本地启动验证。
 */
@RestController
public class RecommendationHealthController {
    /**
     * 返回推荐服务进程级健康状态。
     *
     * @return 推荐服务健康响应。
     */
    @GetMapping("/internal/recommendations/health")
    public ApiResponse<ServiceInfo> health() {
        return ApiResponse.ok(ServiceInfo.up("foodmap-recommendation-service"));
    }
}
