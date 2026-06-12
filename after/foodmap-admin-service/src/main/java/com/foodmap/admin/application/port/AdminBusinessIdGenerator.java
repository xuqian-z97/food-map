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
}
