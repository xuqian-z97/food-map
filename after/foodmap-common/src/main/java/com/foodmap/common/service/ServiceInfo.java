package com.foodmap.common.service;

import java.time.Instant;

public record ServiceInfo(
        String serviceName,
        String status,
        Instant checkedAt
) {
    public static ServiceInfo up(String serviceName) {
        return new ServiceInfo(serviceName, "UP", Instant.now());
    }
}
