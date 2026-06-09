package com.foodmap.auth.infrastructure.persistence.memory;

import com.foodmap.auth.application.port.RefreshTokenRepository;
import com.foodmap.auth.domain.TokenStatus;
import com.foodmap.auth.infrastructure.persistence.entity.RefreshTokenEntity;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Refresh Token 内存仓储，用于 B1 阶段验证刷新令牌只保存哈希的持久化边界。
 */
public class InMemoryRefreshTokenRepository implements RefreshTokenRepository {
    private final Map<Long, RefreshTokenEntity> tokensByTokenId = new ConcurrentHashMap<>();

    /**
     * 保存 Refresh Token 元数据。调用方必须传入 Token 哈希而不是明文 Token。
     */
    @Override
    public void save(RefreshTokenEntity entity) {
        tokensByTokenId.put(entity.getTokenId(), entity);
    }

    /**
     * 按 Token 哈希查找内存中的刷新令牌，便于测试刷新和退出登录流程。
     */
    @Override
    public Optional<RefreshTokenEntity> findByTokenHash(String tokenHash) {
        return tokensByTokenId.values().stream()
                .filter(token -> tokenHash.equals(token.getTokenHash()))
                .findFirst();
    }

    /**
     * 撤销内存刷新令牌，测试中可直接观察实体状态变化。
     */
    @Override
    public void revoke(RefreshTokenEntity entity, OffsetDateTime revokedTime) {
        entity.setTokenStatus(TokenStatus.REVOKED.name());
        entity.setRevokedTime(revokedTime);
        entity.setUpdatedTime(revokedTime);
    }
}
