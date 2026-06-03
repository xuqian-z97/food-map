package com.foodmap.relation.controller;

import com.foodmap.common.api.ApiResponse;
import com.foodmap.common.service.ServiceInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RelationHealthController {
    @GetMapping("/internal/relations/health")
    public ApiResponse<ServiceInfo> health() {
        return ApiResponse.ok(ServiceInfo.up("foodmap-relation-service"));
    }
}
