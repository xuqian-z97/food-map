package com.foodmap.auth.infrastructure.persistence.jdbc;

import com.foodmap.auth.application.port.RefreshTokenRepository;
import com.foodmap.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Refresh Token JDBC 仓储实现，负责把刷新令牌哈希写入 `refresh_tokens` 表。
 */
@Repository
public class JdbcRefreshTokenRepository implements RefreshTokenRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcRefreshTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 插入 Refresh Token 元数据。该方法只保存哈希，不保存明文 Token。
     */
    @Override
    public void save(RefreshTokenEntity entity) {
        jdbcTemplate.update("""
                        insert into refresh_tokens
                        (created_time, updated_time, is_delete, token_id, account_id, token_hash,
                         expires_time, revoked_time, token_status)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                entity.getCreatedTime(),
                entity.getUpdatedTime(),
                entity.getIsDelete(),
                entity.getTokenId(),
                entity.getAccountId(),
                entity.getTokenHash(),
                entity.getExpiresTime(),
                entity.getRevokedTime(),
                entity.getTokenStatus());
    }
}
