package com.foodmap.admin.security;

import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import org.springframework.stereotype.Component;

/**
 * 管理后台权限守卫，基于服务端装配的已认证管理员上下文校验接口所需权限。
 *
 * <p>权限集合来自请求认证阶段读取的数据库当前授权，客户端声明的任何权限头都不参与判定。</p>
 */
@Component
public class AdminPermissionGuard {

    /**
     * 校验后台管理员身份存在且具备指定权限码。
     *
     * @param admin 认证拦截器装配的后台管理员上下文，可能为空表示未经认证。
     * @param requiredPermission 当前接口所需权限码。
     */
    public void requirePermission(AuthenticatedAdmin admin, AdminPermissionCode requiredPermission) {
        if (admin == null) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "后台登录状态已失效");
        }
        if (admin.permissions() == null || !admin.permissions().contains(requiredPermission.name())) {
            throw new FoodMapException(CommonErrorCode.FORBIDDEN, "缺少后台操作权限");
        }
    }
}
