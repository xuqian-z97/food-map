package com.foodmap.user.controller;

import com.foodmap.common.api.ApiResponse;
import com.foodmap.common.security.CurrentUser;
import com.foodmap.common.security.CurrentUserResolver;
import com.foodmap.common.security.FoodMapAuthHeaders;
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
     * 查询当前用户资料，从网关透传的可信身份请求头解析当前用户。
     */
    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me(
            @RequestHeader(FoodMapAuthHeaders.USER_ID) String userId,
            @RequestHeader(FoodMapAuthHeaders.ACCOUNT_ID) String accountId
    ) {
        CurrentUser currentUser = CurrentUserResolver.fromTrustedHeaders(userId, accountId);
        return ApiResponse.ok(userApplicationService.currentUser(currentUser));
    }
}
