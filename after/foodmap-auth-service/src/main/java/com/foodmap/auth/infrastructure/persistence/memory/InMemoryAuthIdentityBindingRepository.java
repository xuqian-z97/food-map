package com.foodmap.auth.infrastructure.persistence.memory;

import com.foodmap.auth.application.port.AuthIdentityBindingRepository;
import com.foodmap.auth.infrastructure.persistence.entity.AuthIdentityBindingEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录身份绑定内存仓储，用于单元测试验证“登录标识 → userId”的绑定写入语义。
 *
 * <p>生产环境必须使用 MyBatis 仓储实现，内存实现只作为测试替身，不参与 Spring 运行时装配。</p>
 */
public class InMemoryAuthIdentityBindingRepository implements AuthIdentityBindingRepository {
    private final Map<Long, AuthIdentityBindingEntity> bindingsByIdentityId = new ConcurrentHashMap<>();

    /**
     * 保存身份绑定实体，调用方负责生成 identityId。
     *
     * @param entity 待保存的身份绑定实体。
     */
    @Override
    public void save(AuthIdentityBindingEntity entity) {
        bindingsByIdentityId.put(entity.getIdentityId(), entity);
    }

    /**
     * 按身份提供方、身份类型和标准化哈希查找身份绑定。
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
        return bindingsByIdentityId.values().stream()
                .filter(binding -> provider.equals(binding.getProvider()))
                .filter(binding -> identityType.equals(binding.getIdentityType()))
                .filter(binding -> identityHash.equals(binding.getIdentityHash()))
                .findFirst();
    }

    /**
     * 按用户业务主键查找全部身份绑定，测试可直接用于核验注册写入条数。
     *
     * @param userId 用户业务主键。
     * @return 该用户的身份绑定列表，没有绑定时返回空列表。
     */
    @Override
    public List<AuthIdentityBindingEntity> findByUserId(Long userId) {
        return bindingsByIdentityId.values().stream()
                .filter(binding -> userId.equals(binding.getUserId()))
                .toList();
    }
}
