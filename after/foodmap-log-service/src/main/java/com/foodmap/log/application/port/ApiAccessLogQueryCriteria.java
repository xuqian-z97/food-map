package com.foodmap.log.application.port;

import java.time.OffsetDateTime;

/**
 * 接口访问摘要查询条件，封装日志库筛选项、分页参数和 SQL offset。
 */
public record ApiAccessLogQueryCriteria(
        String requestId,
        String traceId,
        String serviceName,
        String logLevel,
        Integer httpStatus,
        OffsetDateTime occurredFrom,
        OffsetDateTime occurredTo,
        int pageIndex,
        int pageSize,
        int offset
) {
}
