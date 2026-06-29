package com.foodmap.auth.controller;

import com.foodmap.auth.dto.CurrentAuthResponse;
import com.foodmap.auth.dto.LoginRequest;
import com.foodmap.auth.dto.LoginResponse;
import com.foodmap.auth.dto.LogoutRequest;
import com.foodmap.auth.dto.RefreshTokenRequest;
import com.foodmap.auth.dto.RegisterRequest;
import com.foodmap.auth.dto.RegisterResponse;
import com.foodmap.auth.service.AuthService;
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
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 注册 FoodMap 登录身份，返回标准用户业务主键。
     *
     * @param request 注册请求，包含账号名、联系方式、昵称和密码。
     * @return 注册后的用户业务主键和账号状态，旧 accountId 兼容字段返回 null。
     */
    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    /**
     * 使用账号名、手机号或邮箱登录，返回 Access Token 和 Refresh Token。
     *
     * @param request 登录请求，登录标识可以是账号名、手机号或邮箱。
     * @return 登录成功后的 Token 和过期时间。
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    /**
     * 使用 Refresh Token 刷新 Access Token。
     *
     * @param request Refresh Token 刷新请求。
     * @return 新签发的 Access Token 和原 Refresh Token 信息。
     */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    /**
     * 退出登录并撤销 Refresh Token。
     *
     * @param request 退出登录请求，包含待撤销的 Refresh Token。
     * @return 空响应体，用于表示退出登录处理完成。
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.ok(null);
    }

    /**
     * 查询当前 Access Token 对应的认证会话。
     *
     * @param authorization HTTP Authorization 请求头。
     * @return 当前认证会话中的用户业务主键，旧 accountId 兼容字段返回 null。
     */
    @GetMapping("/me")
    public ApiResponse<CurrentAuthResponse> current(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(authService.current(extractBearerToken(authorization)));
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "缺少Access Token");
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}
