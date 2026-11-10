package com.foodmap.admin.controller;

import com.foodmap.admin.dto.AdminApiAccessLogQueryRequest;
import com.foodmap.admin.dto.AdminApiAccessLogResponse;
import com.foodmap.admin.security.AdminAuthInterceptor;
import com.foodmap.admin.security.AdminPermissionCode;
import com.foodmap.admin.security.AdminPermissionGuard;
import com.foodmap.admin.security.AuthenticatedAdmin;
import com.foodmap.admin.service.AdminApiAccessLogQueryService;
import com.foodmap.common.api.ApiResponse;
import com.foodmap.common.api.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * 管理后台日志查询接口，基于服务端装配的管理员上下文执行 RBAC 权限校验。
 */
@RestController
public class AdminLogController {
    private final AdminApiAccessLogQueryService queryService;
    private final AdminPermissionGuard permissionGuard;

    /**
     * 创建管理后台日志查询控制器。
     *
     * @param queryService 管理后台接口访问摘要查询服务。
     * @param permissionGuard 管理后台权限守卫。
     */
    public AdminLogController(
            AdminApiAccessLogQueryService queryService,
            AdminPermissionGuard permissionGuard
    ) {
        this.queryService = queryService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * 分页查询接口访问摘要，需要 LOG_ACCESS_READ 权限，权限来自数据库当前授权。
     *
     * @param request 当前请求，用于读取认证拦截器装配的管理员上下文。
     * @param requestId 请求流水号。
     * @param traceId 链路追踪号。
     * @param serviceName 服务名称。
     * @param logLevel 日志等级。
     * @param httpStatus HTTP 响应状态码。
     * @param occurredFrom 发生时间下界，闭区间。
     * @param occurredTo 发生时间上界，开区间。
     * @param pageIndex 分页页码，从 0 开始。
     * @param pageSize 分页大小。
     * @return 分页接口访问摘要。
     */
    @GetMapping("/api/admin/logs/api-access")
    public ApiResponse<PageResponse<AdminApiAccessLogResponse>> searchApiAccessLogs(
            HttpServletRequest request,
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
        AuthenticatedAdmin admin = (AuthenticatedAdmin) request.getAttribute(AdminAuthInterceptor.ATTR_AUTHENTICATED_ADMIN);
        permissionGuard.requirePermission(admin, AdminPermissionCode.LOG_ACCESS_READ);
        return ApiResponse.ok(queryService.searchApiAccessLogs(new AdminApiAccessLogQueryRequest(
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
