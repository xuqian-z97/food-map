package com.foodmap.auth.infrastructure.persistence.mybatis;

import com.foodmap.auth.application.port.RefreshTokenRepository;
import com.foodmap.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.foodmap.auth.infrastructure.persistence.mapper.RefreshTokenMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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

    /**
     * 根据 Token 哈希查询未逻辑删除的 Refresh Token 记录。
     */
    @Override
    public Optional<RefreshTokenEntity> findByTokenHash(String tokenHash) {
        RefreshTokenEntity condition = new RefreshTokenEntity();
        condition.setTokenHash(tokenHash);
        List<RefreshTokenEntity> items = refreshTokenMapper.selectPageByCondition(condition, 1, 0);
        if (items.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(items.get(0));
    }

    /**
     * 撤销 Refresh Token 并按业务主键更新状态，便于排查退出登录时间。
     */
    @Override
    public void revoke(RefreshTokenEntity entity, OffsetDateTime revokedTime) {
        entity.setTokenStatus(com.foodmap.auth.domain.TokenStatus.REVOKED.name());
        entity.setRevokedTime(revokedTime);
        entity.setUpdatedTime(revokedTime);
        refreshTokenMapper.updateByBizId(entity);
    }
}
