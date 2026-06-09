package com.foodmap.auth.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * 认证服务业务主键 MyBatis Mapper，负责读取 Flyway 管理的 PostgreSQL sequence。
 */
@Mapper
public interface AuthBusinessIdMapper {

    /**
     * 读取下一个账号业务主键。
     */
    Long nextAccountId();

    /**
     * 读取下一个用户业务主键。
     */
    Long nextUserId();

    /**
     * 读取下一个凭证业务主键。
     */
    Long nextCredentialId();

    /**
     * 读取下一个 Refresh Token 业务主键。
     */
    Long nextRefreshTokenId();

    /**
     * 读取下一个登录日志业务主键。
     */
    Long nextLoginLogId();
}
