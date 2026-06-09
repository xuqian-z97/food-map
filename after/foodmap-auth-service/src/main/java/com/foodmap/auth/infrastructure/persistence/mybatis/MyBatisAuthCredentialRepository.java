package com.foodmap.auth.infrastructure.persistence.mybatis;

import com.foodmap.auth.application.port.AuthCredentialRepository;
import com.foodmap.auth.domain.CredentialType;
import com.foodmap.auth.infrastructure.persistence.entity.AuthCredentialEntity;
import com.foodmap.auth.infrastructure.persistence.mapper.AuthCredentialDefineMapper;
import com.foodmap.auth.infrastructure.persistence.mapper.AuthCredentialMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 认证凭证 MyBatis 仓储实现，负责密码凭证的保存和读取。
 */
@Repository
public class MyBatisAuthCredentialRepository implements AuthCredentialRepository {
    private final AuthCredentialMapper authCredentialMapper;
    private final AuthCredentialDefineMapper authCredentialDefineMapper;

    public MyBatisAuthCredentialRepository(
            AuthCredentialMapper authCredentialMapper,
            AuthCredentialDefineMapper authCredentialDefineMapper
    ) {
        this.authCredentialMapper = authCredentialMapper;
        this.authCredentialDefineMapper = authCredentialDefineMapper;
    }

    /**
     * 保存密码凭证，优先按账号和凭证类型更新，未命中时新增。
     */
    @Override
    public void save(AuthCredentialEntity entity) {
        int updated = authCredentialDefineMapper.updateByAccountIdAndCredentialType(entity);
        if (updated == 0) {
            authCredentialMapper.insertOne(entity);
        }
    }

    /**
     * 根据账号业务主键读取 PASSWORD 凭证。
     */
    @Override
    public Optional<AuthCredentialEntity> findPasswordByAccountId(Long accountId) {
        return Optional.ofNullable(authCredentialDefineMapper.findByAccountIdAndCredentialType(
                accountId,
                CredentialType.PASSWORD.name()
        ));
    }
}
