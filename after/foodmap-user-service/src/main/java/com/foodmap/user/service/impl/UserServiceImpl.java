package com.foodmap.user.service.impl;

import com.foodmap.common.domain.VisibilityType;
import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import com.foodmap.common.security.CurrentUser;
import com.foodmap.common.validation.Check;
import com.foodmap.user.application.port.UserBusinessIdGenerator;
import com.foodmap.user.application.port.UserRepository;
import com.foodmap.user.domain.UserStatus;
import com.foodmap.user.dto.CurrentUserResponse;
import com.foodmap.user.dto.ProvisionUserRequest;
import com.foodmap.user.infrastructure.persistence.entity.UserEntity;
import com.foodmap.user.infrastructure.persistence.entity.UserProfileEntity;
import com.foodmap.user.infrastructure.persistence.entity.UserSettingEntity;
import com.foodmap.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 用户业务服务实现，负责把持久化实体转换为 API DTO 并承载用户用例编排。
 */
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserBusinessIdGenerator businessIdGenerator;

    public UserServiceImpl(UserRepository userRepository, UserBusinessIdGenerator businessIdGenerator) {
        this.userRepository = userRepository;
        this.businessIdGenerator = businessIdGenerator;
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
                null,
                currentUser.accountName(),
                entity.getNickname(),
                entity.getAvatarMediaId(),
                entity.getUserStatus()
        );
    }

    /**
     * 开通注册用户资料，创建用户主表、用户资料表和用户设置表默认记录。
     *
     * @param request 用户资料开通请求。
     * @return 开通后的当前用户资料响应。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CurrentUserResponse provisionUser(ProvisionUserRequest request) {
        if (userRepository.findByUserId(request.userId()).isPresent()) {
            throw new FoodMapException(CommonErrorCode.CONFLICT, "用户资料已存在");
        }
        OffsetDateTime now = OffsetDateTime.now();
        UserEntity user = buildUser(request, now);
        UserProfileEntity profile = buildProfile(request, now);
        UserSettingEntity setting = buildSetting(request, now);
        userRepository.provision(user, profile, setting);
        return toResponse(user, null);
    }

    /**
     * 构建用户主表实体，默认启用搜索和正常状态。
     *
     * @param request 用户资料开通请求。
     * @param now 当前时间。
     * @return 用户主表实体。
     */
    private UserEntity buildUser(ProvisionUserRequest request, OffsetDateTime now) {
        UserEntity entity = new UserEntity();
        entity.setUserId(request.userId());
        entity.setAccountId(request.accountId());
        entity.setNickname(request.nickname());
        entity.setUserStatus(UserStatus.NORMAL.name());
        entity.setSearchable((short) 1);
        entity.setCreatedTime(now);
        entity.setUpdatedTime(now);
        entity.setIsDelete((short) 0);
        return entity;
    }

    /**
     * 构建用户资料实体，资料业务主键由用户服务 sequence 生成。
     *
     * @param request 用户资料开通请求。
     * @param now 当前时间。
     * @return 用户资料实体。
     */
    private UserProfileEntity buildProfile(ProvisionUserRequest request, OffsetDateTime now) {
        UserProfileEntity entity = new UserProfileEntity();
        entity.setProfileId(businessIdGenerator.nextProfileId());
        entity.setUserId(request.userId());
        entity.setCreatedTime(now);
        entity.setUpdatedTime(now);
        entity.setIsDelete((short) 0);
        return entity;
    }

    /**
     * 构建用户设置实体，默认推荐可见范围为仅自己可见。
     *
     * @param request 用户资料开通请求。
     * @param now 当前时间。
     * @return 用户设置实体。
     */
    private UserSettingEntity buildSetting(ProvisionUserRequest request, OffsetDateTime now) {
        UserSettingEntity entity = new UserSettingEntity();
        entity.setSettingId(businessIdGenerator.nextSettingId());
        entity.setUserId(request.userId());
        entity.setDefaultVisibilityType(VisibilityType.PRIVATE.name());
        entity.setAllowFriendRequest((short) 1);
        entity.setAllowSearchByPhone((short) 0);
        entity.setAllowSearchByEmail((short) 0);
        entity.setCreatedTime(now);
        entity.setUpdatedTime(now);
        entity.setIsDelete((short) 0);
        return entity;
    }

    /**
     * 将用户持久化实体转换为当前用户响应 DTO。
     *
     * @param entity 用户主表实体。
     * @param accountName 当前账号名，内部开通接口可为空。
     * @return 当前用户响应。
     */
    private CurrentUserResponse toResponse(UserEntity entity, String accountName) {
        return new CurrentUserResponse(
                entity.getUserId(),
                entity.getAccountId(),
                accountName,
                entity.getNickname(),
                entity.getAvatarMediaId(),
                entity.getUserStatus()
        );
    }
}
