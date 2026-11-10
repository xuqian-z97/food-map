package com.foodmap.admin.service.impl;

import com.foodmap.admin.application.port.AdminAccessSessionRepository;
import com.foodmap.admin.application.port.AdminAuthorizationSnapshot;
import com.foodmap.admin.domain.AdminStatus;
import com.foodmap.admin.security.AuthenticatedAdmin;
import com.foodmap.admin.security.VerifiedAdminToken;
import com.foodmap.admin.service.AdminAuthorizationService;
import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * 后台认证授权应用服务实现，按会话登记、管理员状态、权限版本和有效权限完成每次请求的授权装配。
 *
 * <p>会话有效但权限为空的结果会被保留，由权限守卫区分 401（认证失败）和 403（授权不足）。</p>
 */
@Service
public class AdminAuthorizationServiceImpl implements AdminAuthorizationService {
    private final AdminAccessSessionRepository accessSessionRepository;

    public AdminAuthorizationServiceImpl(AdminAccessSessionRepository accessSessionRepository) {
        this.accessSessionRepository = accessSessionRepository;
    }

    /**
     * 校验 Token 会话有效性并装配已认证管理员上下文。
     *
     * @param token 验签通过的后台 Access Token。
     * @return 已认证管理员上下文，权限集合来自数据库当前授权。
     */
    @Override
    public AuthenticatedAdmin authenticate(VerifiedAdminToken token) {
        AdminAuthorizationSnapshot snapshot;
        try {
            snapshot = accessSessionRepository.loadAuthorizationSnapshot(token.tokenId())
                    .orElseThrow(AdminAuthorizationServiceImpl::sessionInvalid);
        } catch (DataAccessException ex) {
            throw new FoodMapException(CommonErrorCode.SERVICE_UNAVAILABLE, "管理后台会话数据源暂不可用", ex);
        }
        if (snapshot.revokedTime() != null) {
            throw sessionInvalid();
        }
        if (snapshot.expiresTime() == null || !snapshot.expiresTime().isAfter(OffsetDateTime.now())) {
            throw sessionInvalid();
        }
        if (!Objects.equals(snapshot.adminUserId(), token.adminUserId())) {
            throw sessionInvalid();
        }
        if (!AdminStatus.ACTIVE.name().equals(snapshot.adminStatus())) {
            throw sessionInvalid();
        }
        if (!Objects.equals(snapshot.permissionVersion(), token.permissionVersion())) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "后台权限已变更，请重新登录");
        }
        return new AuthenticatedAdmin(
                snapshot.adminUserId(),
                token.tokenId(),
                snapshot.permissionVersion(),
                Set.copyOf(snapshot.permissionCodes()));
    }

    /**
     * 构造会话失效异常，统一提示避免暴露具体失效原因。
     *
     * @return 401 业务异常。
     */
    private static FoodMapException sessionInvalid() {
        return new FoodMapException(CommonErrorCode.UNAUTHORIZED, "后台登录状态已失效");
    }
}
