package com.foodmap.admin.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * 管理后台服务业务主键 MyBatis Mapper，负责读取 Flyway 管理的 PostgreSQL sequence。
 */
@Mapper
public interface AdminBusinessIdMapper {

    /**
     * 读取下一个后台管理员业务主键。
     *
     * @return 后台管理员业务主键。
     */
    Long nextAdminUserId();

    /**
     * 读取下一个后台角色业务主键。
     *
     * @return 后台角色业务主键。
     */
    Long nextAdminRoleId();

    /**
     * 读取下一个后台权限业务主键。
     *
     * @return 后台权限业务主键。
     */
    Long nextAdminPermissionId();

    /**
     * 读取下一个后台管理员角色关联业务主键。
     *
     * @return 后台管理员角色关联业务主键。
     */
    Long nextAdminUserRoleRelationId();

    /**
     * 读取下一个后台角色权限关联业务主键。
     *
     * @return 后台角色权限关联业务主键。
     */
    Long nextAdminRolePermissionRelationId();

    /**
     * 读取下一个后台会话业务主键。
     *
     * @return 后台会话业务主键。
     */
    Long nextAdminAccessSessionId();

    /**
     * 读取下一个后台操作业务主键。
     *
     * @return 后台操作业务主键。
     */
    Long nextAdminOperationId();
}
