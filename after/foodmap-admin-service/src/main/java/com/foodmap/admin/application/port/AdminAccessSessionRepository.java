package com.foodmap.admin.application.port;

import com.foodmap.admin.infrastructure.persistence.entity.AdminAccessSessionEntity;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 后台 Token 会话登记仓储端口，隔离应用层与会话事实的具体持久化技术。
 */
public interface AdminAccessSessionRepository {

    /**
     * 登记新的后台 Token 会话。
     *
     * @param entity 后台 Token 会话登记实体。
     */
    void register(AdminAccessSessionEntity entity);

    /**
     * 按 Token 唯一标识和管理员业务主键撤销未撤销会话。
     *
     * @param tokenId 后台 Access Token 的 JWT jti 文本形式。
     * @param adminUserId 后台管理员业务主键。
     * @param revokedTime 会话撤销时间。
     * @return 首次撤销返回 true，会话不存在、归属不符或已撤销返回 false。
     */
    boolean revokeActive(String tokenId, Long adminUserId, OffsetDateTime revokedTime);

    /**
     * 加载后台认证授权快照：从会话行读取过期与撤销状态，联合管理员状态与当前权限版本。
     *
     * @param tokenId 后台 Access Token 的 JWT jti 文本形式。
     * @return 认证授权快照，会话或管理员未命中返回空 Optional。
     */
    Optional<AdminAuthorizationSnapshot> loadAuthorizationSnapshot(String tokenId);
}
