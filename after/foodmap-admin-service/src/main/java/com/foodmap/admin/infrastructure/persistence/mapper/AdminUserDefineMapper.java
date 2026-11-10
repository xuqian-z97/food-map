package com.foodmap.admin.infrastructure.persistence.mapper;

import com.foodmap.admin.infrastructure.persistence.entity.AdminUserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;

/**
 * `admin_users` 自定义 MyBatis Mapper，承载后台登录所需的行级锁定和登录时间更新 SQL。
 */
@Mapper
public interface AdminUserDefineMapper {

    /**
     * 按后台管理员业务主键锁定并查询管理员行，供登录短事务内复核账号状态和密码哈希。
     *
     * @param adminUserId 后台管理员业务主键。
     * @return 后台管理员实体，未命中返回 null。
     */
    AdminUserEntity selectByAdminUserIdForUpdate(@Param("adminUserId") Long adminUserId);

    /**
     * 更新最近一次后台登录时间，避免登录链路全字段覆盖管理员实体。
     *
     * @param adminUserId 后台管理员业务主键。
     * @param lastLoginTime 最近一次后台登录时间。
     * @param updatedTime 更新时间。
     * @return 影响行数。
     */
    int updateLastLoginTime(
            @Param("adminUserId") Long adminUserId,
            @Param("lastLoginTime") OffsetDateTime lastLoginTime,
            @Param("updatedTime") OffsetDateTime updatedTime
    );
}
