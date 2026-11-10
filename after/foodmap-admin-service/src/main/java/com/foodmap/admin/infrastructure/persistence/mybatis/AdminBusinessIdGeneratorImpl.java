package com.foodmap.admin.infrastructure.persistence.mybatis;

import com.foodmap.admin.application.port.AdminBusinessIdGenerator;
import com.foodmap.admin.infrastructure.persistence.mapper.AdminBusinessIdMapper;
import org.springframework.stereotype.Component;

/**
 * 管理后台服务业务主键生成器，负责通过 MyBatis 适配层读取 PostgreSQL sequence。
 */
@Component
public class AdminBusinessIdGeneratorImpl implements AdminBusinessIdGenerator {
    private final AdminBusinessIdMapper adminBusinessIdMapper;

    public AdminBusinessIdGeneratorImpl(AdminBusinessIdMapper adminBusinessIdMapper) {
        this.adminBusinessIdMapper = adminBusinessIdMapper;
    }

    /**
     * 生成后台管理员业务主键。
     *
     * @return 后台管理员业务主键。
     */
    @Override
    public long nextAdminUserId() {
        return adminBusinessIdMapper.nextAdminUserId();
    }

    /**
     * 生成后台角色业务主键。
     *
     * @return 后台角色业务主键。
     */
    @Override
    public long nextAdminRoleId() {
        return adminBusinessIdMapper.nextAdminRoleId();
    }

    /**
     * 生成后台权限业务主键。
     *
     * @return 后台权限业务主键。
     */
    @Override
    public long nextAdminPermissionId() {
        return adminBusinessIdMapper.nextAdminPermissionId();
    }

    /**
     * 生成后台管理员角色关联业务主键。
     *
     * @return 后台管理员角色关联业务主键。
     */
    @Override
    public long nextAdminUserRoleRelationId() {
        return adminBusinessIdMapper.nextAdminUserRoleRelationId();
    }

    /**
     * 生成后台角色权限关联业务主键。
     *
     * @return 后台角色权限关联业务主键。
     */
    @Override
    public long nextAdminRolePermissionRelationId() {
        return adminBusinessIdMapper.nextAdminRolePermissionRelationId();
    }

    /**
     * 生成后台会话业务主键。
     *
     * @return 后台会话业务主键。
     */
    @Override
    public long nextAdminAccessSessionId() {
        return adminBusinessIdMapper.nextAdminAccessSessionId();
    }

    /**
     * 生成后台操作业务主键。
     *
     * @return 后台操作业务主键。
     */
    @Override
    public long nextAdminOperationId() {
        return adminBusinessIdMapper.nextAdminOperationId();
    }
}
