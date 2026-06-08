package com.foodmap.auth.infrastructure.persistence.jdbc;

import com.foodmap.auth.application.port.AuthAccountRepository;
import com.foodmap.auth.infrastructure.persistence.entity.AuthAccountEntity;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 认证账号 JDBC 仓储实现，负责把 `auth_accounts` 表映射为服务内持久化实体。
 */
@Repository
public class JdbcAuthAccountRepository implements AuthAccountRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<AuthAccountEntity> rowMapper = new AuthAccountRowMapper();

    public JdbcAuthAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 保存账号，先按账号业务主键更新，未命中时插入新记录。
     */
    @Override
    public void save(AuthAccountEntity entity) {
        int updated = jdbcTemplate.update("""
                        update auth_accounts
                        set user_id = ?, account_name = ?, phone = ?, email = ?, account_status = ?,
                            registered_channel = ?, last_login_time = ?, updated_time = ?, is_delete = ?
                        where account_id = ?
                        """,
                entity.getUserId(),
                entity.getAccountName(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getAccountStatus(),
                entity.getRegisteredChannel(),
                entity.getLastLoginTime(),
                entity.getUpdatedTime(),
                entity.getIsDelete(),
                entity.getAccountId());
        if (updated == 0) {
            jdbcTemplate.update("""
                            insert into auth_accounts
                            (created_time, updated_time, is_delete, account_id, user_id, account_name, phone, email,
                             account_status, registered_channel, last_login_time)
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    entity.getCreatedTime(),
                    entity.getUpdatedTime(),
                    entity.getIsDelete(),
                    entity.getAccountId(),
                    entity.getUserId(),
                    entity.getAccountName(),
                    entity.getPhone(),
                    entity.getEmail(),
                    entity.getAccountStatus(),
                    entity.getRegisteredChannel(),
                    entity.getLastLoginTime());
        }
    }

    /**
     * 根据账号业务主键读取账号，自动过滤逻辑删除数据。
     */
    @Override
    public Optional<AuthAccountEntity> findByAccountId(Long accountId) {
        return Optional.ofNullable(DataAccessUtils.singleResult(jdbcTemplate.query("""
                        select * from auth_accounts
                        where account_id = ? and is_delete = 0
                        limit 1
                        """,
                rowMapper,
                accountId)));
    }

    /**
     * 根据账号名、手机号或邮箱读取账号，登录入口统一通过该方法定位账号。
     */
    @Override
    public Optional<AuthAccountEntity> findByLoginIdentifier(String loginIdentifier) {
        String normalized = loginIdentifier.trim().toLowerCase();
        return Optional.ofNullable(DataAccessUtils.singleResult(jdbcTemplate.query("""
                        select * from auth_accounts
                        where is_delete = 0
                          and (lower(account_name) = ? or phone = ? or lower(email) = ?)
                        limit 1
                        """,
                rowMapper,
                normalized,
                loginIdentifier.trim(),
                normalized)));
    }

    private static final class AuthAccountRowMapper implements RowMapper<AuthAccountEntity> {
        @Override
        public AuthAccountEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            AuthAccountEntity entity = new AuthAccountEntity();
            entity.setId(rs.getLong("id"));
            entity.setCreatedTime(rs.getObject("created_time", OffsetDateTime.class));
            entity.setUpdatedTime(rs.getObject("updated_time", OffsetDateTime.class));
            entity.setIsDelete(rs.getShort("is_delete"));
            entity.setAccountId(rs.getLong("account_id"));
            entity.setUserId(rs.getLong("user_id"));
            entity.setAccountName(rs.getString("account_name"));
            entity.setPhone(rs.getString("phone"));
            entity.setEmail(rs.getString("email"));
            entity.setAccountStatus(rs.getString("account_status"));
            entity.setRegisteredChannel(rs.getString("registered_channel"));
            entity.setLastLoginTime(rs.getObject("last_login_time", OffsetDateTime.class));
            return entity;
        }
    }
}
