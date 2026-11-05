package com.foodmap.auth.application.port;

import com.foodmap.auth.infrastructure.persistence.entity.AuthIdentityBindingEntity;

import java.util.List;
import java.util.Optional;

/**
 * 登录身份绑定仓储端口，应用层通过该接口维护“登录标识 → userId”的绑定关系。
 *
 * <p>登录标识查询必须使用 provider、identityType 和 identityHash 三元组，禁止把明文登录标识
 * 放进查询条件。双写阶段写入前必须先调用查询方法判断是否已存在，避免触发唯一索引。</p>
 */
public interface AuthIdentityBindingRepository {

    /**
     * 单条新增登录身份绑定。调用方负责生成 identityId 并保证同一哈希未被写入。
     *
     * @param entity 待保存的身份绑定实体。
     */
    void save(AuthIdentityBindingEntity entity);

    /**
     * 按身份提供方、身份类型和标准化哈希查询有效绑定。
     *
     * @param provider 身份提供方，如 LOCAL。
     * @param identityType 身份类型，如 ACCOUNT_NAME、PHONE、EMAIL。
     * @param identityHash 标准化登录标识的 SHA-256 小写十六进制哈希。
     * @return 查询到的身份绑定，未命中时返回空 Optional。
     */
    Optional<AuthIdentityBindingEntity> findByProviderAndIdentityTypeAndIdentityHash(
            String provider,
            String identityType,
            String identityHash
    );

    /**
     * 按用户业务主键查询该用户全部有效身份绑定，用于安全设置展示和双写核验。
     *
     * @param userId 用户业务主键。
     * @return 该用户的身份绑定列表，没有绑定时返回空列表。
     */
    List<AuthIdentityBindingEntity> findByUserId(Long userId);
}
