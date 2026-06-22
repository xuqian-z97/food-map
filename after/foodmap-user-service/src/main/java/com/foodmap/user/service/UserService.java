package com.foodmap.user.service;

import com.foodmap.common.security.CurrentUser;
import com.foodmap.user.dto.CurrentUserResponse;
import com.foodmap.user.dto.ProvisionUserRequest;

/**
 * 用户业务服务接口，定义 Controller 可调用的用户资料用例。
 */
public interface UserService {

    /**
     * 查询当前登录用户资料。返回 DTO 而非持久化实体，避免数据库字段直接泄露到前端契约。
     */
    CurrentUserResponse currentUser(CurrentUser currentUser);

    /**
     * 开通注册用户资料，创建用户主资料、扩展资料和默认隐私设置。
     *
     * @param request 用户资料开通请求。
     * @return 开通后的当前用户资料响应。
     */
    CurrentUserResponse provisionUser(ProvisionUserRequest request);
}
