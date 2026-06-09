package com.foodmap.auth.infrastructure.persistence.mybatis;

import com.foodmap.auth.application.port.AuthAccountRepository;
import com.foodmap.auth.infrastructure.persistence.entity.AuthAccountEntity;
import com.foodmap.auth.infrastructure.persistence.mapper.AuthAccountDefineMapper;
import com.foodmap.auth.infrastructure.persistence.mapper.AuthAccountMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 认证账号仓储实现，负责在 MyBatis 适配层把应用层账号仓储语义转换为 Mapper 调用。
 */
@Repository
public class AuthAccountRepositoryImpl implements AuthAccountRepository {
    private final AuthAccountMapper authAccountMapper;
    private final AuthAccountDefineMapper authAccountDefineMapper;

    public AuthAccountRepositoryImpl(
            AuthAccountMapper authAccountMapper,
            AuthAccountDefineMapper authAccountDefineMapper
    ) {
        this.authAccountMapper = authAccountMapper;
        this.authAccountDefineMapper = authAccountDefineMapper;
    }

    /**
     * 保存账号，先按账号业务主键更新，未命中时新增。
     */
    @Override
    public void save(AuthAccountEntity entity) {
        int updated = authAccountMapper.updateByBizId(entity);
        if (updated == 0) {
            authAccountMapper.insertOne(entity);
        }
    }

    /**
     * 根据账号业务主键查询账号。
     */
    @Override
    public Optional<AuthAccountEntity> findByAccountId(Long accountId) {
        return Optional.ofNullable(authAccountMapper.selectByBizId(accountId));
    }

    /**
     * 根据账号名、手机号或邮箱查询账号。
     */
    @Override
    public Optional<AuthAccountEntity> findByLoginIdentifier(String loginIdentifier) {
        String original = loginIdentifier.trim();
        String normalized = original.toLowerCase();
        return Optional.ofNullable(authAccountDefineMapper.findByLoginIdentifier(normalized, original));
    }
}
