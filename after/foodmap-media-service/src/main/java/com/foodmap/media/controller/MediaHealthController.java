package com.foodmap.media.controller;

import com.foodmap.common.api.ApiResponse;
import com.foodmap.common.service.ServiceInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MediaHealthController {
    @GetMapping("/internal/media/health")
    public ApiResponse<ServiceInfo> health() {
        return ApiResponse.ok(ServiceInfo.up("foodmap-media-service"));
    }
}
