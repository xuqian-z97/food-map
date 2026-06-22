package com.foodmap.user.controller;

import com.foodmap.common.api.ApiResponse;
import com.foodmap.user.dto.CurrentUserResponse;
import com.foodmap.user.dto.ProvisionUserRequest;
import com.foodmap.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户服务内部开通接口，只供认证服务等受信内部调用方使用。
 */
@RestController
public class InternalUserProvisionController {
    private final UserService userService;

    /**
     * 创建内部用户开通 Controller。
     *
     * @param userService 用户业务服务。
     */
    public InternalUserProvisionController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 开通注册用户资料，创建用户主资料、扩展资料和默认设置。
     *
     * @param request 用户资料开通请求。
     * @return 开通后的用户资料摘要。
     */
    @PostMapping("/internal/users/provision")
    public ApiResponse<CurrentUserResponse> provision(@Valid @RequestBody ProvisionUserRequest request) {
        return ApiResponse.ok(userService.provisionUser(request));
    }
}
