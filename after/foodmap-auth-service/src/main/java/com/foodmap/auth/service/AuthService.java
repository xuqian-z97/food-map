package com.foodmap.auth.service;

import com.foodmap.auth.dto.CurrentAuthResponse;
import com.foodmap.auth.dto.LoginRequest;
import com.foodmap.auth.dto.LoginResponse;
import com.foodmap.auth.dto.LogoutRequest;
import com.foodmap.auth.dto.RefreshTokenRequest;
import com.foodmap.auth.dto.RegisterRequest;
import com.foodmap.auth.dto.RegisterResponse;

/**
 * 认证业务服务接口，定义 Controller 可调用的注册、登录、刷新和退出登录用例。
 *
 * <p>Controller 只依赖该接口，不直接依赖实现类；实现类负责事务边界、业务编排和仓储端口调用。</p>
 */
public interface AuthService {

    /**
     * 注册账号并保存密码哈希，返回账号和用户业务主键。
     */
    RegisterResponse register(RegisterRequest request);

    /**
     * 使用账号名、手机号或邮箱登录，成功后签发 Access Token 和 Refresh Token。
     */
    LoginResponse login(LoginRequest request);

    /**
     * 使用有效 Refresh Token 刷新 Access Token。
     */
    LoginResponse refresh(RefreshTokenRequest request);

    /**
     * 退出登录并撤销 Refresh Token。
     */
    void logout(LogoutRequest request);

    /**
     * 解析当前 Access Token，返回账号和用户业务主键。
     */
    CurrentAuthResponse current(String accessToken);
}
