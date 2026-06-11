package com.foodmap.community.controller;

import com.foodmap.common.api.ApiResponse;
import com.foodmap.common.service.ServiceInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 社区服务内部健康检查接口，用于网关路由和本地启动验证。
 */
@RestController
public class CommunityHealthController {
    /**
     * 返回社区服务进程级健康状态。
     *
     * @return 社区服务健康响应。
     */
    @GetMapping("/internal/community/health")
    public ApiResponse<ServiceInfo> health() {
        return ApiResponse.ok(ServiceInfo.up("foodmap-community-service"));
    }
}
