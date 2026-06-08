package com.foodmap.auth.controller;

import com.foodmap.auth.application.AuthApplicationService;
import com.foodmap.auth.dto.LoginRequest;
import com.foodmap.auth.dto.LoginResponse;
import com.foodmap.auth.dto.RegisterRequest;
import com.foodmap.auth.dto.RegisterResponse;
import com.foodmap.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
}
