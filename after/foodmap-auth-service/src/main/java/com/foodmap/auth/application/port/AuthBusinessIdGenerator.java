package com.foodmap.auth.application.port;

/**
 * 认证服务业务主键生成端口。
 *
 * <p>账号、用户、凭证、刷新令牌和登录日志都需要稳定递增的 bigint 业务主键。实现方必须保证服务重启后
 * 不会回到固定起点，避免与数据库已有数据冲突。</p>
 */
public interface AuthBusinessIdGenerator {

    /**
     * 生成认证账号业务主键，用于 auth_accounts.account_id。
     */
    Long nextAccountId();

    /**
     * 生成用户业务主键，用于 auth_accounts.user_id 并同步给用户服务。
     */
    Long nextUserId();

    /**
     * 生成认证凭证业务主键，用于 auth_credentials.credential_id。
     */
    Long nextCredentialId();

    /**
     * 生成 Refresh Token 业务主键，用于 refresh_tokens.token_id。
     */
    Long nextRefreshTokenId();

    /**
     * 生成登录日志业务主键，用于 login_logs.login_log_id。
     */
    Long nextLoginLogId();
}
