package com.foodmap.admin.service;

import com.foodmap.admin.application.port.LogApiAccessLogClient;
import com.foodmap.admin.dto.AdminApiAccessLogQueryRequest;
import com.foodmap.admin.dto.AdminApiAccessLogResponse;
import com.foodmap.common.api.PageResponse;
import org.springframework.stereotype.Service;

/**
 * 管理后台接口访问摘要查询服务，负责通过日志平台客户端获取脱敏日志摘要。
 */
@Service
public class AdminApiAccessLogQueryService {
    private final LogApiAccessLogClient client;

    /**
     * 创建管理后台日志查询服务。
     *
     * @param client 日志平台接口访问摘要客户端。
     */
    public AdminApiAccessLogQueryService(LogApiAccessLogClient client) {
        this.client = client;
    }

    /**
     * 查询接口访问摘要，管理后台不直接访问日志库。
     *
     * @param request 查询请求。
     * @return 分页接口访问摘要。
     */
    public PageResponse<AdminApiAccessLogResponse> searchApiAccessLogs(AdminApiAccessLogQueryRequest request) {
        return client.search(request);
    }
}
