package com.foodmap.auth.infrastructure.persistence.jdbc;

import com.foodmap.auth.application.port.AuthBusinessIdGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 基于 PostgreSQL sequence 的认证服务业务主键生成器。
 *
 * <p>sequence 由 Flyway 管理，并在迁移时根据已有表数据回拨到最大值，解决服务重启后内存 ID 重复的问题。</p>
 */
@Component
public class PostgresAuthBusinessIdGenerator implements AuthBusinessIdGenerator {
    private final JdbcTemplate jdbcTemplate;

    public PostgresAuthBusinessIdGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 生成认证账号业务主键。
     */
    @Override
    public Long nextAccountId() {
        return nextValue("auth_account_id_seq");
    }

    /**
     * 生成用户业务主键。
     */
    @Override
    public Long nextUserId() {
        return nextValue("auth_user_id_seq");
    }

    /**
     * 生成认证凭证业务主键。
     */
    @Override
    public Long nextCredentialId() {
        return nextValue("auth_credential_id_seq");
    }

    /**
     * 生成 Refresh Token 业务主键。
     */
    @Override
    public Long nextRefreshTokenId() {
        return nextValue("auth_refresh_token_id_seq");
    }

    /**
     * 生成登录日志业务主键。
     */
    @Override
    public Long nextLoginLogId() {
        return nextValue("auth_login_log_id_seq");
    }

    /**
     * 读取指定 PostgreSQL sequence 的下一个值，sequence 名称来自代码常量，不接收外部输入。
     */
    private Long nextValue(String sequenceName) {
        return jdbcTemplate.queryForObject("select nextval('" + sequenceName + "')", Long.class);
    }
}
