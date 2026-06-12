package com.foodmap.admin.dto;

import java.time.OffsetDateTime;

/**
 * 管理后台接口访问摘要响应 DTO，只承载日志平台返回的脱敏摘要字段。
 */
public record AdminApiAccessLogResponse(
        Long accessLogId,
        String requestId,
        String traceId,
        String spanId,
        String serviceName,
        String eventName,
        String logLevel,
        String httpMethod,
        String requestPath,
        Integer httpStatus,
        Long durationMs,
        String clientIpMasked,
        Long accountId,
        Long userId,
        String gatewayRouteId,
        String errorCode,
        OffsetDateTime occurredTime
) {
}
