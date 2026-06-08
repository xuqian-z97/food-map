package com.foodmap.auth.infrastructure.persistence.memory;

import com.foodmap.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Refresh Token 内存仓储，用于 B1 阶段验证刷新令牌只保存哈希的持久化边界。
 */
@Repository
public class InMemoryRefreshTokenRepository {
    private final Map<Long, RefreshTokenEntity> tokensByTokenId = new ConcurrentHashMap<>();

    /**
     * 保存 Refresh Token 元数据。调用方必须传入 Token 哈希而不是明文 Token。
     */
    public void save(RefreshTokenEntity entity) {
        tokensByTokenId.put(entity.getTokenId(), entity);
    }
}
