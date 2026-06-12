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
}
