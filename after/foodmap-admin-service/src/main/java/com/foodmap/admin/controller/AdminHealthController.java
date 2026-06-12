package com.foodmap.admin.controller;

import com.foodmap.common.api.ApiResponse;
import com.foodmap.common.service.ServiceInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台服务内部健康检查接口，用于网关路由和本地启动验证。
 */
@RestController
public class AdminHealthController {

    /**
     * 返回管理后台服务进程级健康状态。
     *
     * @return 管理后台服务健康响应。
     */
    @GetMapping("/internal/admin/health")
    public ApiResponse<ServiceInfo> health() {
        return ApiResponse.ok(ServiceInfo.up("foodmap-admin-service"));
    }
}
