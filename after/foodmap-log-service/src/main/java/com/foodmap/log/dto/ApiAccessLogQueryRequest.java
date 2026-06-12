package com.foodmap.log.dto;

import java.time.OffsetDateTime;

/**
 * 接口访问摘要查询请求 DTO，承载后台日志查询入口的筛选条件。
 */
public record ApiAccessLogQueryRequest(
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
