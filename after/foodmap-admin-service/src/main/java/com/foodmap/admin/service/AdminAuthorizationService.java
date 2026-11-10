package com.foodmap.admin.service;

import com.foodmap.admin.security.AuthenticatedAdmin;
import com.foodmap.admin.security.VerifiedAdminToken;

/**
 * 后台认证授权应用服务，把验签通过的 Token 关联到数据库中的会话与当前授权事实。
 */
public interface AdminAuthorizationService {

    /**
     * 校验 Token 会话有效性并装配已认证管理员上下文。
     *
     * @param token 验签通过的后台 Access Token。
     * @return 已认证管理员上下文，权限集合来自数据库当前授权。
     * @throws com.foodmap.common.exception.FoodMapException 会话未登记、已撤销、已过期、归属不符、
     *         管理员停用或权限版本过期时抛出 401，数据源不可用时抛出 503。
     */
    AuthenticatedAdmin authenticate(VerifiedAdminToken token);
}
