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
     * 注册登录身份并保存密码哈希，返回标准用户业务主键。
     *
     * @param request 注册请求。
     * @return 注册后的用户业务主键和账号状态，旧 accountId 兼容字段返回 null。
     */
    RegisterResponse register(RegisterRequest request);

    /**
     * 使用账号名、手机号或邮箱登录，成功后签发 Access Token 和 Refresh Token。
     *
     * @param request 登录请求。
     * @return 登录成功后的 Token、过期时间和用户业务主键，旧 accountId 兼容字段返回 null。
     */
    LoginResponse login(LoginRequest request);

    /**
     * 使用有效 Refresh Token 刷新 Access Token。
     *
     * @param request 刷新请求。
     * @return 刷新后的 Access Token、Refresh Token 信息和用户业务主键，旧 accountId 兼容字段返回 null。
     */
    LoginResponse refresh(RefreshTokenRequest request);

    /**
     * 退出登录并撤销 Refresh Token。
     *
     * @param request 退出登录请求。
     */
    void logout(LogoutRequest request);

    /**
     * 解析当前 Access Token，返回用户业务主键。
     *
     * @param accessToken 当前 Access Token。
     * @return 当前认证会话中的用户业务主键，旧 accountId 兼容字段返回 null。
     */
    CurrentAuthResponse current(String accessToken);
}
