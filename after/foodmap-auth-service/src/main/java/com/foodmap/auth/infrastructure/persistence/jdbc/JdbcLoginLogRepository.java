package com.foodmap.auth.infrastructure.persistence.jdbc;

import com.foodmap.auth.application.port.LoginLogRepository;
import com.foodmap.auth.infrastructure.persistence.entity.LoginLogEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 登录日志 JDBC 仓储实现，负责写入 `login_logs` 安全审计数据。
 */
@Repository
public class JdbcLoginLogRepository implements LoginLogRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcLoginLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 插入登录日志，日志字段不得包含密码、Token 或完整敏感联系方式。
     */
    @Override
    public void save(LoginLogEntity entity) {
        jdbcTemplate.update("""
                        insert into login_logs
                        (created_time, updated_time, is_delete, login_log_id, account_id,
                         login_type, login_result, ip_address, user_agent)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                entity.getCreatedTime(),
                entity.getUpdatedTime(),
                entity.getIsDelete(),
                entity.getLoginLogId(),
                entity.getAccountId(),
                entity.getLoginType(),
                entity.getLoginResult(),
                entity.getIpAddress(),
                entity.getUserAgent());
    }
}
