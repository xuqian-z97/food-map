package com.foodmap.auth.infrastructure.persistence.mybatis;

import com.foodmap.auth.application.port.AuthBusinessIdGenerator;
import com.foodmap.auth.infrastructure.persistence.mapper.AuthBusinessIdMapper;
import org.springframework.stereotype.Component;

/**
 * 认证服务业务主键生成器，负责通过 MyBatis 适配层读取 PostgreSQL sequence。
 */
@Component
public class AuthBusinessIdGeneratorImpl implements AuthBusinessIdGenerator {
    private final AuthBusinessIdMapper authBusinessIdMapper;

    public AuthBusinessIdGeneratorImpl(AuthBusinessIdMapper authBusinessIdMapper) {
        this.authBusinessIdMapper = authBusinessIdMapper;
    }

    /**
     * 生成认证账号业务主键。
     */
    @Override
    public Long nextAccountId() {
        return authBusinessIdMapper.nextAccountId();
    }

    /**
     * 生成用户业务主键。
     */
    @Override
    public Long nextUserId() {
        return authBusinessIdMapper.nextUserId();
    }

    /**
     * 生成认证凭证业务主键。
     */
    @Override
    public Long nextCredentialId() {
        return authBusinessIdMapper.nextCredentialId();
    }

    /**
     * 生成 Refresh Token 业务主键。
     */
    @Override
    public Long nextRefreshTokenId() {
        return authBusinessIdMapper.nextRefreshTokenId();
    }

    /**
     * 生成登录日志业务主键。
     */
    @Override
    public Long nextLoginLogId() {
        return authBusinessIdMapper.nextLoginLogId();
    }
}
