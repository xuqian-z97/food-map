package com.foodmap.auth.infrastructure.persistence.mybatis;

import com.foodmap.auth.application.port.AuthIdentityBindingRepository;
import com.foodmap.auth.infrastructure.persistence.entity.AuthIdentityBindingEntity;
import com.foodmap.auth.infrastructure.persistence.mapper.AuthIdentityBindingMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 登录身份绑定仓储实现，负责在 MyBatis 适配层把应用层绑定语义转换为单表 Mapper 调用。
 */
@Repository
public class AuthIdentityBindingRepositoryImpl implements AuthIdentityBindingRepository {
    private final AuthIdentityBindingMapper authIdentityBindingMapper;

    public AuthIdentityBindingRepositoryImpl(AuthIdentityBindingMapper authIdentityBindingMapper) {
        this.authIdentityBindingMapper = authIdentityBindingMapper;
    }

    /**
     * 单条新增登录身份绑定。
     *
     * @param entity 待保存的身份绑定实体。
     */
    @Override
    public void save(AuthIdentityBindingEntity entity) {
        authIdentityBindingMapper.insertOne(entity);
    }

    /**
     * 按身份提供方、身份类型和标准化哈希查询有效绑定。
     *
     * @param provider 身份提供方，如 LOCAL。
     * @param identityType 身份类型，如 ACCOUNT_NAME、PHONE、EMAIL。
     * @param identityHash 标准化登录标识的 SHA-256 小写十六进制哈希。
     * @return 查询到的身份绑定，未命中时返回空 Optional。
     */
    @Override
    public Optional<AuthIdentityBindingEntity> findByProviderAndIdentityTypeAndIdentityHash(
            String provider,
            String identityType,
            String identityHash
    ) {
        AuthIdentityBindingEntity condition = new AuthIdentityBindingEntity();
        condition.setProvider(provider);
        condition.setIdentityType(identityType);
        condition.setIdentityHash(identityHash);
        List<AuthIdentityBindingEntity> items = authIdentityBindingMapper.selectPageByCondition(condition, 1, 0);
        if (items.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(items.get(0));
    }

    /**
     * 按用户业务主键查询该用户全部有效身份绑定。
     *
     * @param userId 用户业务主键。
     * @return 该用户的身份绑定列表，没有绑定时返回空列表。
     */
    @Override
    public List<AuthIdentityBindingEntity> findByUserId(Long userId) {
        AuthIdentityBindingEntity condition = new AuthIdentityBindingEntity();
        condition.setUserId(userId);
        return authIdentityBindingMapper.selectListByCondition(condition);
    }
}
