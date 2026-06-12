package com.foodmap.admin.security;

import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 管理后台权限守卫，校验网关透传的后台管理员身份和权限码。
 */
@Component
public class AdminPermissionGuard {

    /**
     * 校验后台管理员身份存在且具备指定权限码。
     *
     * @param adminUserIdHeader 后台管理员业务主键请求头。
     * @param permissionsHeader 后台管理员权限码列表请求头。
     * @param requiredPermission 当前接口所需权限码。
     */
    public void requirePermission(
            String adminUserIdHeader,
            String permissionsHeader,
            AdminPermissionCode requiredPermission
    ) {
        if (adminUserIdHeader == null || adminUserIdHeader.isBlank()) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "后台登录状态已失效");
        }
        if (!hasPermission(permissionsHeader, requiredPermission)) {
            throw new FoodMapException(CommonErrorCode.FORBIDDEN, "缺少后台操作权限");
        }
    }

    /**
     * 判断权限码列表是否包含目标权限。
     *
     * @param permissionsHeader 后台管理员权限码列表请求头。
     * @param requiredPermission 当前接口所需权限码。
     * @return 包含目标权限时返回 true。
     */
    private boolean hasPermission(String permissionsHeader, AdminPermissionCode requiredPermission) {
        if (permissionsHeader == null || permissionsHeader.isBlank()) {
            return false;
        }
        return Arrays.stream(permissionsHeader.split(","))
                .map(String::trim)
                .anyMatch(requiredPermission.name()::equals);
    }
}
