package com.foodmap.admin.dto;

import java.time.OffsetDateTime;

/**
 * 管理后台接口访问摘要查询请求 DTO，承载后台日志查询筛选条件。
 */
public record AdminApiAccessLogQueryRequest(
        String requestId,
        String traceId,
        String serviceName,
        String logLevel,
        Integer httpStatus,
        OffsetDateTime occurredFrom,
        OffsetDateTime occurredTo,
        Integer pageIndex,
        Integer pageSize
) {
}
