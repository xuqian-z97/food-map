package com.foodmap.admin.service;

import com.foodmap.admin.dto.AdminLoginRequest;
import com.foodmap.admin.dto.AdminLoginResponse;
import com.foodmap.admin.security.AuthenticatedAdmin;

/**
 * 后台认证应用服务，负责后台管理员登录和登出。
 */
public interface AdminAuthService {

    /**
     * 后台管理员登录，签发后台 Access Token 并登记会话。
     *
     * @param request 后台登录请求。
     * @return 后台登录响应，含 Token 文本和过期时间。
     * @throws com.foodmap.common.exception.FoodMapException 凭据错误、账号停用或锁定返回 401，
     *         触发限流返回 429，数据源不可用返回 503。
     */
    AdminLoginResponse login(AdminLoginRequest request);

    /**
     * 后台管理员登出，撤销当前 Token 会话，重复登出幂等成功。
     *
     * @param admin 认证拦截器装配的后台管理员上下文。
     * @throws com.foodmap.common.exception.FoodMapException 会话数据源不可用返回 503。
     */
    void logout(AuthenticatedAdmin admin);
}
