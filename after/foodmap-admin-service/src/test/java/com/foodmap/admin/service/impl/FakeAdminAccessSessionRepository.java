package com.foodmap.admin.service.impl;

import com.foodmap.admin.application.port.AdminAccessSessionRepository;
import com.foodmap.admin.application.port.AdminAuthorizationSnapshot;
import com.foodmap.admin.infrastructure.persistence.entity.AdminAccessSessionEntity;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 后台会话仓储内存替身，用于登录、登出和认证授权用例的可重复测试。
 */
class FakeAdminAccessSessionRepository implements AdminAccessSessionRepository {
    private final Map<String, AdminAccessSessionEntity> sessionsByTokenId = new LinkedHashMap<>();
    private final Map<String, AdminAuthorizationSnapshot> snapshots = new HashMap<>();
    private String lastRegisteredTokenId;

    /**
     * 预置会话实体。
     *
     * @param entity 会话实体。
     */
    void putSession(AdminAccessSessionEntity entity) {
        sessionsByTokenId.put(entity.getTokenId(), entity);
    }

    /**
     * 预置认证授权快照。
     *
     * @param tokenId Token 唯一标识。
     * @param snapshot 快照。
     */
    void putSnapshot(String tokenId, AdminAuthorizationSnapshot snapshot) {
        snapshots.put(tokenId, snapshot);
    }

    /**
     * 查询会话实体。
     *
     * @param tokenId Token 唯一标识。
     * @return 会话实体。
     */
    Optional<AdminAccessSessionEntity> session(String tokenId) {
        return Optional.ofNullable(sessionsByTokenId.get(tokenId));
    }

    /**
     * 读取最近一次登记的 Token 唯一标识，供登录用例关联响应与会话。
     *
     * @return Token 唯一标识，未登记时为 null。
     */
    String lastRegisteredTokenId() {
        return lastRegisteredTokenId;
    }

    @Override
    public void register(AdminAccessSessionEntity entity) {
        lastRegisteredTokenId = entity.getTokenId();
        sessionsByTokenId.put(entity.getTokenId(), entity);
    }

    @Override
    public boolean revokeActive(String tokenId, Long adminUserId, OffsetDateTime revokedTime) {
        AdminAccessSessionEntity entity = sessionsByTokenId.get(tokenId);
        if (entity == null || !entity.getAdminUserId().equals(adminUserId) || entity.getRevokedTime() != null) {
            return false;
        }
        entity.setRevokedTime(revokedTime);
        return true;
    }

    @Override
    public Optional<AdminAuthorizationSnapshot> loadAuthorizationSnapshot(String tokenId) {
        return Optional.ofNullable(snapshots.get(tokenId));
    }

    /**
     * 便捷构造有效快照。
     *
     * @param adminUserId 后台管理员业务主键。
     * @param permissionVersion 权限版本。
     * @param permissionCodes 权限编码集合。
     * @return 未来 15 分钟内有效的快照。
     */
    static AdminAuthorizationSnapshot activeSnapshot(Long adminUserId, long permissionVersion, Set<String> permissionCodes) {
        return new AdminAuthorizationSnapshot(adminUserId, "ACTIVE", permissionVersion,
                OffsetDateTime.now().plusMinutes(15), null, permissionCodes);
    }
}
