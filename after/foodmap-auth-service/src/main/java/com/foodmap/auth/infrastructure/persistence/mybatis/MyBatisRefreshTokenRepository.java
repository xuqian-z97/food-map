package com.foodmap.auth.infrastructure.persistence.mybatis;

import com.foodmap.auth.application.port.RefreshTokenRepository;
import com.foodmap.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.foodmap.auth.infrastructure.persistence.mapper.RefreshTokenMapper;
import org.springframework.stereotype.Repository;

/**
 * Refresh Token MyBatis 仓储实现，只保存 Token 哈希和元数据。
 */
@Repository
public class MyBatisRefreshTokenRepository implements RefreshTokenRepository {
    private final RefreshTokenMapper refreshTokenMapper;

    public MyBatisRefreshTokenRepository(RefreshTokenMapper refreshTokenMapper) {
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
