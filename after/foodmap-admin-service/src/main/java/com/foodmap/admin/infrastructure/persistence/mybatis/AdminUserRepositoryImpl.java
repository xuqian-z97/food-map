package com.foodmap.admin.infrastructure.persistence.mybatis;

import com.foodmap.admin.application.port.AdminUserRepository;
import com.foodmap.admin.infrastructure.persistence.entity.AdminUserEntity;
import com.foodmap.admin.infrastructure.persistence.mapper.AdminUserDefineMapper;
import com.foodmap.admin.infrastructure.persistence.mapper.AdminUserMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 后台管理员仓储实现，负责在 MyBatis 适配层把后台管理员仓储端口转换为 Mapper 调用。
 */
@Repository
public class AdminUserRepositoryImpl implements AdminUserRepository {
    private final AdminUserMapper adminUserMapper;
    private final AdminUserDefineMapper adminUserDefineMapper;

    public AdminUserRepositoryImpl(AdminUserMapper adminUserMapper, AdminUserDefineMapper adminUserDefineMapper) {
        this.adminUserMapper = adminUserMapper;
        this.adminUserDefineMapper = adminUserDefineMapper;
    }

    /**
     * 保存后台管理员实体，先按管理员业务主键更新，未命中时新增。
     *
     * @param entity 待保存的后台管理员持久化实体。
     */
    @Override
    public void save(AdminUserEntity entity) {
        int updated = adminUserMapper.updateByBizId(entity);
        if (updated == 0) {
            adminUserMapper.insertOne(entity);
        }
    }

    /**
     * 按后台管理员业务主键查询管理员。
     *
     * @param adminUserId 后台管理员业务主键。
     * @return 查询到的后台管理员实体，未命中时返回空 Optional。
     */
    @Override
    public Optional<AdminUserEntity> findByAdminUserId(Long adminUserId) {
        return Optional.ofNullable(adminUserMapper.selectByBizId(adminUserId));
    }

    /**
     * 按后台登录账号名查询管理员。
     *
     * @param username 后台登录账号名。
     * @return 查询到的后台管理员实体，未命中时返回空 Optional。
     */
    @Override
    public Optional<AdminUserEntity> findByUsername(String username) {
        return Optional.ofNullable(adminUserMapper.selectByUsername(username));
    }

    /**
     * 按后台管理员业务主键锁定并查询管理员行，供登录短事务内复核账号状态和密码哈希。
     *
     * @param adminUserId 后台管理员业务主键。
     * @return 锁定到的后台管理员实体，未命中时返回空 Optional。
     */
    @Override
    public Optional<AdminUserEntity> lockByAdminUserId(Long adminUserId) {
        return Optional.ofNullable(adminUserDefineMapper.selectByAdminUserIdForUpdate(adminUserId));
    }

    /**
     * 更新最近一次后台登录时间。
     *
     * @param adminUserId 后台管理员业务主键。
     * @param lastLoginTime 最近一次后台登录时间。
     */
    @Override
    public void updateLastLoginTime(Long adminUserId, OffsetDateTime lastLoginTime) {
        adminUserDefineMapper.updateLastLoginTime(adminUserId, lastLoginTime, lastLoginTime);
    }
}
