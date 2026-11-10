package com.foodmap.admin.application.port;

import com.foodmap.admin.infrastructure.persistence.entity.AdminUserEntity;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 后台管理员仓储端口，隔离 application 层和 MyBatis 等具体持久化技术。
 */
public interface AdminUserRepository {

    /**
     * 保存后台管理员实体，按管理员业务主键更新，未命中时新增。
     *
     * @param entity 待保存的后台管理员持久化实体。
     */
    void save(AdminUserEntity entity);

    /**
     * 按后台管理员业务主键查询管理员。
     *
     * @param adminUserId 后台管理员业务主键。
     * @return 查询到的后台管理员实体，未命中时返回空 Optional。
     */
    Optional<AdminUserEntity> findByAdminUserId(Long adminUserId);

    /**
     * 按后台登录账号名查询管理员。
     *
     * @param username 后台登录账号名。
     * @return 查询到的后台管理员实体，未命中时返回空 Optional。
     */
    Optional<AdminUserEntity> findByUsername(String username);

    /**
     * 按后台管理员业务主键锁定并查询管理员行，供登录短事务内复核账号状态和密码哈希。
     *
     * @param adminUserId 后台管理员业务主键。
     * @return 锁定到的后台管理员实体，未命中时返回空 Optional。
     */
    Optional<AdminUserEntity> lockByAdminUserId(Long adminUserId);

    /**
     * 更新最近一次后台登录时间。
     *
     * @param adminUserId 后台管理员业务主键。
     * @param lastLoginTime 最近一次后台登录时间。
     */
    void updateLastLoginTime(Long adminUserId, OffsetDateTime lastLoginTime);
}
