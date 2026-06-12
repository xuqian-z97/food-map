package com.foodmap.admin.infrastructure.persistence.memory;

import com.foodmap.admin.application.port.AdminUserRepository;
import com.foodmap.admin.infrastructure.persistence.entity.AdminUserEntity;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 后台管理员内存仓储，仅用于单元测试和本地替身，不作为生产 profile 默认持久化实现。
 */
public class InMemoryAdminUserRepository implements AdminUserRepository {
    private final Map<Long, AdminUserEntity> usersByAdminUserId = new ConcurrentHashMap<>();
    private final Map<String, AdminUserEntity> usersByUsername = new ConcurrentHashMap<>();

    /**
     * 保存后台管理员实体。
     *
     * @param entity 待保存的后台管理员持久化实体。
     */
    @Override
    public void save(AdminUserEntity entity) {
        usersByAdminUserId.put(entity.getAdminUserId(), entity);
        usersByUsername.put(entity.getUsername(), entity);
    }

    /**
     * 按后台管理员业务主键查询管理员。
     *
     * @param adminUserId 后台管理员业务主键。
     * @return 查询到的后台管理员实体，未命中时返回空 Optional。
     */
    @Override
    public Optional<AdminUserEntity> findByAdminUserId(Long adminUserId) {
        return Optional.ofNullable(usersByAdminUserId.get(adminUserId));
    }

    /**
     * 按后台登录账号名查询管理员。
     *
     * @param username 后台登录账号名。
     * @return 查询到的后台管理员实体，未命中时返回空 Optional。
     */
    @Override
    public Optional<AdminUserEntity> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username));
    }
}
