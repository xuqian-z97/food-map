package com.foodmap.admin.application.port;

/**
 * 管理后台服务业务主键生成端口，生产实现通过 Flyway 管理的 PostgreSQL sequence 获取主键。
 */
public interface AdminBusinessIdGenerator {

    /**
     * 生成后台管理员业务主键。
     *
     * @return 后台管理员业务主键。
     */
    long nextAdminUserId();

    /**
     * 生成后台角色业务主键。
     *
     * @return 后台角色业务主键。
     */
    long nextAdminRoleId();

    /**
     * 生成后台权限业务主键。
     *
     * @return 后台权限业务主键。
     */
    long nextAdminPermissionId();

    /**
     * 生成后台管理员角色关联业务主键。
     *
     * @return 后台管理员角色关联业务主键。
     */
    long nextAdminUserRoleRelationId();

    /**
     * 生成后台角色权限关联业务主键。
     *
     * @return 后台角色权限关联业务主键。
     */
    long nextAdminRolePermissionRelationId();

    /**
     * 生成后台会话业务主键。
     *
     * @return 后台会话业务主键。
     */
    long nextAdminAccessSessionId();

    /**
     * 生成后台操作业务主键。
     *
     * @return 后台操作业务主键。
     */
    long nextAdminOperationId();
}
