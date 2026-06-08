package com.foodmap.auth.application.port;

import com.foodmap.auth.infrastructure.persistence.entity.RefreshTokenEntity;

/**
 * Refresh Token 仓储端口，负责保存刷新令牌哈希、过期时间和撤销状态。
 */
public interface RefreshTokenRepository {

    /**
     * 保存 Refresh Token 元数据。调用方必须传入 Token 哈希而不是明文 Token。
     */
    void save(RefreshTokenEntity entity);
}
