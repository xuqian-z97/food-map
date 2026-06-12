package com.foodmap.log.service;

import com.foodmap.common.api.PageResponse;
import com.foodmap.log.application.port.ApiAccessLogQueryCriteria;
import com.foodmap.log.application.port.ApiAccessLogRepository;
import com.foodmap.log.dto.ApiAccessLogQueryRequest;
import com.foodmap.log.dto.ApiAccessLogResponse;
import com.foodmap.log.infrastructure.persistence.entity.ApiAccessLogEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 接口访问摘要查询服务，负责查询参数归一化、分页限制和响应 DTO 转换。
 */
@Service
public class ApiAccessLogQueryService {
    private static final int DEFAULT_PAGE_INDEX = 0;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ApiAccessLogRepository repository;

    /**
     * 创建接口访问摘要查询服务。
     *
     * @param repository 接口访问摘要仓储端口。
     */
    public ApiAccessLogQueryService(ApiAccessLogRepository repository) {
        this.repository = repository;
    }

    /**
     * 按条件分页查询接口访问摘要。
     *
     * @param request 查询请求 DTO。
     * @return 分页接口访问摘要响应。
     */
    public PageResponse<ApiAccessLogResponse> search(ApiAccessLogQueryRequest request) {
        ApiAccessLogQueryCriteria criteria = toCriteria(request);
        long total = repository.count(criteria);
        List<ApiAccessLogResponse> items = repository.search(criteria)
                .stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.of(items, total, criteria.pageIndex(), criteria.pageSize());
    }

    /**
     * 将查询请求归一化为仓储条件，限制分页参数避免后台误触发大范围扫描。
     *
     * @param request 查询请求 DTO。
     * @return 仓储查询条件。
     */
    private ApiAccessLogQueryCriteria toCriteria(ApiAccessLogQueryRequest request) {
        int pageIndex = normalizePageIndex(request.pageIndex());
        int pageSize = normalizePageSize(request.pageSize());
        return new ApiAccessLogQueryCriteria(
                trimToNull(request.requestId()),
                trimToNull(request.traceId()),
                trimToNull(request.serviceName()),
                trimToNull(request.logLevel()),
                request.httpStatus(),
                request.occurredFrom(),
                request.occurredTo(),
                pageIndex,
                pageSize,
                pageIndex * pageSize
        );
    }

    /**
     * 将持久化实体转换为响应 DTO，避免 Controller 暴露数据库实体。
     *
     * @param entity 接口访问摘要持久化实体。
     * @return 接口访问摘要响应 DTO。
     */
    private ApiAccessLogResponse toResponse(ApiAccessLogEntity entity) {
        return new ApiAccessLogResponse(
                entity.getAccessLogId(),
                entity.getRequestId(),
                entity.getTraceId(),
                entity.getSpanId(),
                entity.getServiceName(),
                entity.getEventName(),
                entity.getLogLevel(),
                entity.getHttpMethod(),
                entity.getRequestPath(),
                entity.getHttpStatus(),
                entity.getDurationMs(),
                entity.getClientIpMasked(),
                entity.getAccountId(),
                entity.getUserId(),
                entity.getGatewayRouteId(),
                entity.getErrorCode(),
                entity.getOccurredTime()
        );
    }

    /**
     * 归一化页码，非法页码回退到第一页。
     *
     * @param pageIndex 原始页码。
     * @return 从 0 开始的页码。
     */
    private int normalizePageIndex(Integer pageIndex) {
        if (pageIndex == null || pageIndex < 0) {
            return DEFAULT_PAGE_INDEX;
        }
        return pageIndex;
    }

    /**
     * 归一化页大小，非法或过大页大小回退或截断到安全范围。
     *
     * @param pageSize 原始页大小。
     * @return 安全页大小。
     */
    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 将空白文本转换为空值，避免 MyBatis 动态 SQL 使用无效条件。
     *
     * @param value 原始文本。
     * @return trim 后文本或空值。
     */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
