package com.foodmap.log.dto;

import java.time.OffsetDateTime;

/**
 * 接口访问摘要响应 DTO，只返回已脱敏摘要字段，不暴露原始请求体和敏感信息。
 */
public record ApiAccessLogResponse(
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
