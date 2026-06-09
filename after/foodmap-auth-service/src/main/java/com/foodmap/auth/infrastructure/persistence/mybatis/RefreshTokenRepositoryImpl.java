package com.foodmap.auth.infrastructure.persistence.mybatis;

import com.foodmap.auth.application.port.RefreshTokenRepository;
import com.foodmap.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.foodmap.auth.infrastructure.persistence.mapper.RefreshTokenMapper;
import org.springframework.stereotype.Repository;

/**
 * Refresh Token 仓储实现，负责在 MyBatis 适配层保存 Token 哈希和元数据。
 */
@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {
    private final RefreshTokenMapper refreshTokenMapper;

    public RefreshTokenRepositoryImpl(RefreshTokenMapper refreshTokenMapper) {
        this.refreshTokenMapper = refreshTokenMapper;
    }

    /**
     * 单条保存 Refresh Token 元数据。
     */
    @Override
    public void save(RefreshTokenEntity entity) {
        refreshTokenMapper.insertOne(entity);
    }
}
