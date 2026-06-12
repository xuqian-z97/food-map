package com.foodmap.admin.service.impl;

import com.foodmap.admin.application.port.AdminBusinessIdGenerator;
import com.foodmap.admin.application.port.AdminUserRepository;
import com.foodmap.admin.domain.AdminStatus;
import com.foodmap.admin.dto.AdminUserResponse;
import com.foodmap.admin.dto.CreateAdminUserRequest;
import com.foodmap.admin.infrastructure.persistence.entity.AdminUserEntity;
import com.foodmap.admin.service.AdminPasswordHashService;
import com.foodmap.admin.service.AdminUserService;
import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import com.foodmap.common.logging.LogMasker;
import com.foodmap.common.validation.Check;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 后台管理员应用服务实现，负责后台管理员账号创建和 DTO 转换。
 */
@Service
public class AdminUserServiceImpl implements AdminUserService {
    private static final long INITIAL_PERMISSION_VERSION = 1L;
    private static final short NOT_DELETED = 0;

    private final AdminBusinessIdGenerator businessIdGenerator;
    private final AdminUserRepository adminUserRepository;
    private final AdminPasswordHashService passwordHashService;

    public AdminUserServiceImpl(AdminBusinessIdGenerator businessIdGenerator,
                                AdminUserRepository adminUserRepository,
                                AdminPasswordHashService passwordHashService) {
        this.businessIdGenerator = businessIdGenerator;
        this.adminUserRepository = adminUserRepository;
        this.passwordHashService = passwordHashService;
    }

    /**
     * 创建后台管理员账号，初始化 ACTIVE 状态、权限版本和密码哈希。
     *
     * @param request 创建后台管理员请求。
     * @return 后台管理员响应 DTO。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserResponse createAdminUser(CreateAdminUserRequest request) {
        String username = Check.notBlank("username", request.username());
        String password = Check.notBlank("password", request.password());
        String displayName = Check.notBlank("displayName", request.displayName());
        if (adminUserRepository.findByUsername(username).isPresent()) {
            throw new FoodMapException(CommonErrorCode.CONFLICT, "后台管理员账号名已存在");
        }

        OffsetDateTime now = OffsetDateTime.now();
        AdminUserEntity entity = new AdminUserEntity();
        entity.setCreatedTime(now);
        entity.setUpdatedTime(now);
        entity.setIsDelete(NOT_DELETED);
        entity.setAdminUserId(businessIdGenerator.nextAdminUserId());
        entity.setUsername(username);
        entity.setPasswordHash(passwordHashService.hash(password));
        entity.setDisplayName(displayName);
        entity.setMobileMasked(maskMobile(request.mobile()));
        entity.setEmailMasked(maskEmail(request.email()));
        entity.setAdminStatus(AdminStatus.ACTIVE.name());
        entity.setPermissionVersion(INITIAL_PERMISSION_VERSION);
        adminUserRepository.save(entity);
        return toResponse(entity);
    }

    /**
     * 将后台管理员持久化实体转换为响应 DTO，避免泄露数据库内部主键和密码哈希。
     *
     * @param entity 后台管理员持久化实体。
     * @return 后台管理员响应 DTO。
     */
    private AdminUserResponse toResponse(AdminUserEntity entity) {
        return new AdminUserResponse(
                entity.getAdminUserId(),
                entity.getUsername(),
                entity.getDisplayName(),
                entity.getMobileMasked(),
                entity.getEmailMasked(),
                entity.getAdminStatus(),
                entity.getPermissionVersion()
        );
    }

    /**
     * 对手机号执行字段级脱敏，空值保持为空避免把可选字段误展示为敏感占位。
     *
     * @param mobile 原始手机号。
     * @return 脱敏手机号。
     */
    private String maskMobile(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            return null;
        }
        return LogMasker.maskPhone(mobile.trim());
    }

    /**
     * 对邮箱执行字段级脱敏，空值保持为空避免把可选字段误展示为敏感占位。
     *
     * @param email 原始邮箱。
     * @return 脱敏邮箱。
     */
    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return LogMasker.maskEmail(email.trim());
    }
}
