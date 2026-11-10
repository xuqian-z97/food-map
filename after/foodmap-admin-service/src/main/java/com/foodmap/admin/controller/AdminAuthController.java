package com.foodmap.admin.controller;

import com.foodmap.admin.dto.AdminLoginRequest;
import com.foodmap.admin.dto.AdminLoginResponse;
import com.foodmap.admin.security.AdminAuthInterceptor;
import com.foodmap.admin.security.AuthenticatedAdmin;
import com.foodmap.admin.service.AdminAuthService;
import com.foodmap.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台认证接口，提供后台管理员登录和登出入口。
 *
 * <p>登录响应携带 `Cache-Control: no-store`，Token 只通过 Authorization 传输，不进入日志或响应体采集。</p>
 */
@RestController
public class AdminAuthController {
    private final AdminAuthService adminAuthService;

    /**
     * 创建管理后台认证控制器。
     *
     * @param adminAuthService 后台认证应用服务。
     */
    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    /**
     * 后台管理员登录，成功返回后台 Access Token。
     *
     * @param request 后台登录请求，账号名和密码均必填。
     * @return 统一响应，data 含 accessToken、tokenType 和 expiresTime，禁止任何缓存存储。
     */
    @PostMapping(value = "/api/admin/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<AdminLoginResponse>> login(@RequestBody AdminLoginRequest request) {
        AdminLoginResponse response = adminAuthService.login(request);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(response));
    }

    /**
     * 后台管理员登出，撤销当前 Token 会话；重复登出幂等成功。
     *
     * @param request 当前请求，用于读取认证拦截器装配的管理员上下文。
     * @return 统一响应，data 为 null，登出后原 Token 再访问受保护接口返回 401。
     */
    @PostMapping(value = "/api/admin/auth/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Void> logout(HttpServletRequest request) {
        AuthenticatedAdmin admin = (AuthenticatedAdmin) request.getAttribute(AdminAuthInterceptor.ATTR_AUTHENTICATED_ADMIN);
        adminAuthService.logout(admin);
        return ApiResponse.ok(null);
    }
}
