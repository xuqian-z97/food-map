package com.foodmap.auth.service;

import com.foodmap.auth.application.port.AuthBusinessIdGenerator;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 认证服务单元测试专用业务主键生成器。
 *
 * <p>生产环境必须使用 PostgreSQL sequence；测试环境使用内存计数器可以让应用服务测试保持快速且不依赖数据库。</p>
 */
class TestAuthBusinessIdGenerator implements AuthBusinessIdGenerator {
    private final AtomicLong accountIdSequence = new AtomicLong(100_000L);
    private final AtomicLong userIdSequence = new AtomicLong(200_000L);
    private final AtomicLong credentialIdSequence = new AtomicLong(300_000L);
    private final AtomicLong tokenIdSequence = new AtomicLong(400_000L);
    private final AtomicLong loginLogIdSequence = new AtomicLong(500_000L);

    /**
     * 生成测试账号业务主键。
     */
    @Override
    public Long nextAccountId() {
        return accountIdSequence.incrementAndGet();
    }

    /**
     * 生成测试用户业务主键。
     */
    @Override
    public Long nextUserId() {
        return userIdSequence.incrementAndGet();
    }

    /**
     * 生成测试凭证业务主键。
     */
    @Override
    public Long nextCredentialId() {
        return credentialIdSequence.incrementAndGet();
    }

    /**
     * 生成测试 Refresh Token 业务主键。
     */
    @Override
    public Long nextRefreshTokenId() {
        return tokenIdSequence.incrementAndGet();
    }

    /**
     * 生成测试登录日志业务主键。
     */
    @Override
    public Long nextLoginLogId() {
        return loginLogIdSequence.incrementAndGet();
    }
}
