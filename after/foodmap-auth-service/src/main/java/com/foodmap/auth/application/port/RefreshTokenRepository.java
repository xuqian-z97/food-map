package com.foodmap.auth.application.port;

import com.foodmap.auth.infrastructure.persistence.entity.RefreshTokenEntity;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Refresh Token 仓储端口，负责保存刷新令牌哈希、过期时间和撤销状态。
 */
public interface RefreshTokenRepository {

    /**
     * 保存 Refresh Token 元数据。调用方必须传入 Token 哈希而不是明文 Token。
     */
    void save(RefreshTokenEntity entity);

    /**
     * 根据 Token 哈希查询刷新令牌元数据。调用方不能传入明文 Token。
     */
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    /**
     * 撤销 Refresh Token，退出登录和安全风控都应通过该方法记录撤销时间。
     */
    void revoke(RefreshTokenEntity entity, OffsetDateTime revokedTime);
}
