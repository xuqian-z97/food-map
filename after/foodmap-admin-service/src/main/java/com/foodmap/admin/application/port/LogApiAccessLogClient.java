package com.foodmap.admin.application.port;

import com.foodmap.admin.dto.AdminApiAccessLogQueryRequest;
import com.foodmap.admin.dto.AdminApiAccessLogResponse;
import com.foodmap.common.api.PageResponse;

/**
 * 日志平台接口访问摘要客户端端口，隔离管理后台与日志服务内部 HTTP 实现。
 */
public interface LogApiAccessLogClient {

    /**
     * 调用日志平台查询接口访问摘要。
     *
     * @param request 后台日志查询请求。
     * @return 分页接口访问摘要。
     */
    PageResponse<AdminApiAccessLogResponse> search(AdminApiAccessLogQueryRequest request);
}
