package com.foodmap.auth.controller;

import com.foodmap.auth.application.AuthApplicationService;
import com.foodmap.auth.dto.CurrentAuthResponse;
import com.foodmap.auth.dto.LoginRequest;
import com.foodmap.auth.dto.LoginResponse;
import com.foodmap.auth.dto.LogoutRequest;
import com.foodmap.auth.dto.RefreshTokenRequest;
import com.foodmap.auth.dto.RegisterRequest;
import com.foodmap.auth.dto.RegisterResponse;
import com.foodmap.common.api.ApiResponse;
import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证 API 控制器，只负责 HTTP 入参校验和 DTO 响应封装。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    /**
     * 注册 FoodMap 账号，返回账号和用户业务主键。
     */
    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authApplicationService.register(request));
    }

    /**
     * 使用账号名、手机号或邮箱登录，返回 Access Token 和 Refresh Token。
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authApplicationService.login(request));
    }

    /**
     * 使用 Refresh Token 刷新 Access Token。
     */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authApplicationService.refresh(request));
    }

    /**
     * 退出登录并撤销 Refresh Token。
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authApplicationService.logout(request);
        return ApiResponse.ok(null);
    }

    /**
     * 查询当前 Access Token 对应的认证会话。
     */
    @GetMapping("/me")
    public ApiResponse<CurrentAuthResponse> current(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(authApplicationService.current(extractBearerToken(authorization)));
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "缺少Access Token");
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}
