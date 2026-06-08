package com.foodmap.user.controller;

import com.foodmap.common.api.ApiResponse;
import com.foodmap.common.security.CurrentUser;
import com.foodmap.user.application.UserApplicationService;
import com.foodmap.user.dto.CurrentUserResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户 API 控制器，负责读取可信身份头并返回用户 DTO。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserApplicationService userApplicationService;

    public UserController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    /**
     * 查询当前用户资料。MVP 阶段先从网关透传请求头读取身份，后续替换为统一认证上下文解析器。
     */
    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me(
            @RequestHeader("X-FoodMap-User-Id") Long userId,
            @RequestHeader("X-FoodMap-Account-Id") Long accountId,
            @RequestHeader(value = "X-FoodMap-Account-Name", required = false) String accountName
    ) {
        return ApiResponse.ok(userApplicationService.currentUser(new CurrentUser(userId, accountId, accountName)));
    }
}
