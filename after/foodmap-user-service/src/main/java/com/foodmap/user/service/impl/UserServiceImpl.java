package com.foodmap.user.service.impl;

import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import com.foodmap.common.security.CurrentUser;
import com.foodmap.common.validation.Check;
import com.foodmap.user.application.port.UserRepository;
import com.foodmap.user.dto.CurrentUserResponse;
import com.foodmap.user.infrastructure.persistence.entity.UserEntity;
import com.foodmap.user.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 用户业务服务实现，负责把持久化实体转换为 API DTO 并承载用户用例编排。
 */
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 查询当前登录用户资料。返回 DTO 而非持久化实体，避免数据库字段直接泄露到前端契约。
     *
     * @param currentUser 网关透传并解析后的当前登录用户上下文。
     * @return 当前登录用户资料响应。
     */
    @Override
    public CurrentUserResponse currentUser(CurrentUser currentUser) {
        Long userId = Check.positive("userId", currentUser.userId());
        UserEntity entity = userRepository.findByUserId(userId)
                .orElseThrow(() -> new FoodMapException(CommonErrorCode.NOT_FOUND, "用户资料不存在"));
        return new CurrentUserResponse(
                entity.getUserId(),
                entity.getAccountId(),
                currentUser.accountName(),
                entity.getNickname(),
                entity.getAvatarMediaId(),
                entity.getUserStatus()
        );
    }
}
