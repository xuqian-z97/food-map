package com.foodmap.auth.infrastructure.persistence.jdbc;

import com.foodmap.auth.application.port.AuthCredentialRepository;
import com.foodmap.auth.domain.CredentialType;
import com.foodmap.auth.infrastructure.persistence.entity.AuthCredentialEntity;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 认证凭证 JDBC 仓储实现，负责读写 `auth_credentials` 表中的密码凭证。
 */
@Repository
public class JdbcAuthCredentialRepository implements AuthCredentialRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<AuthCredentialEntity> rowMapper = new AuthCredentialRowMapper();

    public JdbcAuthCredentialRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 保存密码凭证，先按账号业务主键和凭证类型更新，未命中时插入。
     */
    @Override
    public void save(AuthCredentialEntity entity) {
        int updated = jdbcTemplate.update("""
                        update auth_credentials
                        set password_hash = ?, hash_algorithm = ?, updated_time = ?, is_delete = ?
                        where account_id = ? and credential_type = ?
                        """,
                entity.getPasswordHash(),
                entity.getHashAlgorithm(),
                entity.getUpdatedTime(),
                entity.getIsDelete(),
                entity.getAccountId(),
                entity.getCredentialType());
        if (updated == 0) {
            jdbcTemplate.update("""
                            insert into auth_credentials
                            (created_time, updated_time, is_delete, credential_id, account_id, credential_type,
                             password_hash, hash_algorithm)
                            values (?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    entity.getCreatedTime(),
                    entity.getUpdatedTime(),
                    entity.getIsDelete(),
                    entity.getCredentialId(),
                    entity.getAccountId(),
                    entity.getCredentialType(),
                    entity.getPasswordHash(),
                    entity.getHashAlgorithm());
        }
    }

    /**
     * 根据账号业务主键读取密码凭证，自动过滤逻辑删除数据。
     */
    @Override
    public Optional<AuthCredentialEntity> findPasswordByAccountId(Long accountId) {
        return Optional.ofNullable(DataAccessUtils.singleResult(jdbcTemplate.query("""
                        select * from auth_credentials
                        where account_id = ? and credential_type = ? and is_delete = 0
                        limit 1
                        """,
                rowMapper,
                accountId,
                CredentialType.PASSWORD.name())));
    }

    private static final class AuthCredentialRowMapper implements RowMapper<AuthCredentialEntity> {
        @Override
        public AuthCredentialEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            AuthCredentialEntity entity = new AuthCredentialEntity();
            entity.setId(rs.getLong("id"));
            entity.setCreatedTime(rs.getObject("created_time", OffsetDateTime.class));
            entity.setUpdatedTime(rs.getObject("updated_time", OffsetDateTime.class));
            entity.setIsDelete(rs.getShort("is_delete"));
            entity.setCredentialId(rs.getLong("credential_id"));
            entity.setAccountId(rs.getLong("account_id"));
            entity.setCredentialType(rs.getString("credential_type"));
            entity.setPasswordHash(rs.getString("password_hash"));
            entity.setHashAlgorithm(rs.getString("hash_algorithm"));
            return entity;
        }
    }
}
