package com.foodmap.admin.service;

import com.foodmap.admin.dto.AdminUserResponse;
import com.foodmap.admin.dto.CreateAdminUserRequest;

/**
 * 后台管理员应用服务接口，Controller 只能依赖该接口而不是具体实现。
 */
public interface AdminUserService {

    /**
     * 创建后台管理员账号，负责密码哈希、手机号邮箱脱敏和默认权限版本初始化。
     *
     * @param request 创建后台管理员请求。
     * @return 后台管理员响应 DTO。
     */
    AdminUserResponse createAdminUser(CreateAdminUserRequest request);
}
