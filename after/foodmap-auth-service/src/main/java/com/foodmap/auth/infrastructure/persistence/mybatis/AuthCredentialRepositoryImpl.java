package com.foodmap.auth.infrastructure.persistence.mybatis;

import com.foodmap.auth.application.port.AuthCredentialRepository;
import com.foodmap.auth.domain.CredentialType;
import com.foodmap.auth.infrastructure.persistence.entity.AuthCredentialEntity;
import com.foodmap.auth.infrastructure.persistence.mapper.AuthCredentialDefineMapper;
import com.foodmap.auth.infrastructure.persistence.mapper.AuthCredentialMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 认证凭证仓储实现，负责在 MyBatis 适配层完成密码凭证的保存和读取。
 */
@Repository
public class AuthCredentialRepositoryImpl implements AuthCredentialRepository {
    private final AuthCredentialMapper authCredentialMapper;
    private final AuthCredentialDefineMapper authCredentialDefineMapper;

    public AuthCredentialRepositoryImpl(
            AuthCredentialMapper authCredentialMapper,
            AuthCredentialDefineMapper authCredentialDefineMapper
    ) {
        this.authCredentialMapper = authCredentialMapper;
        this.authCredentialDefineMapper = authCredentialDefineMapper;
    }

    /**
     * 保存密码凭证，优先按账号和凭证类型更新，未命中时新增。
     *
     * @param entity 待保存的认证凭证持久化实体。
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
     *
     * @param accountId 账号业务主键。
     * @return 查询到的密码凭证实体，未命中时返回空 Optional。
     */
    @Override
    public Optional<AuthCredentialEntity> findPasswordByAccountId(Long accountId) {
        return Optional.ofNullable(authCredentialDefineMapper.findByAccountIdAndCredentialType(
                accountId,
                CredentialType.PASSWORD.name()
        ));
    }
}
