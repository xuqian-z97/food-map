package com.foodmap.log.controller;

import com.foodmap.common.api.ApiResponse;
import com.foodmap.common.api.PageResponse;
import com.foodmap.log.dto.ApiAccessLogQueryRequest;
import com.foodmap.log.dto.ApiAccessLogResponse;
import com.foodmap.log.service.ApiAccessLogQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * 日志平台接口访问摘要内部查询接口，供管理后台日志查询代理调用。
 */
@RestController
public class LogApiAccessLogController {
    private final ApiAccessLogQueryService queryService;

    /**
     * 创建接口访问摘要查询控制器。
     *
     * @param queryService 接口访问摘要查询服务。
     */
    public LogApiAccessLogController(ApiAccessLogQueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * 分页查询接口访问摘要，只返回独立日志库中的脱敏摘要字段。
     *
     * @param requestId 请求流水号。
     * @param traceId 链路追踪号。
     * @param serviceName 服务名称。
     * @param logLevel 日志等级。
     * @param httpStatus HTTP 响应状态码。
     * @param occurredFrom 发生时间下界，闭区间。
     * @param occurredTo 发生时间上界，开区间。
     * @param pageIndex 分页页码，从 0 开始。
     * @param pageSize 分页大小，服务端最大限制为 100。
     * @return 分页接口访问摘要。
     */
    @GetMapping("/internal/logs/api-access")
    public ApiResponse<PageResponse<ApiAccessLogResponse>> search(
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String logLevel,
            @RequestParam(required = false) Integer httpStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime occurredFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime occurredTo,
            @RequestParam(required = false) Integer pageIndex,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.ok(queryService.search(new ApiAccessLogQueryRequest(
                requestId,
                traceId,
                serviceName,
                logLevel,
                httpStatus,
                occurredFrom,
                occurredTo,
                pageIndex,
                pageSize
        )));
    }
}
