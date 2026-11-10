package com.foodmap.admin.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * `admin_user_roles` 自定义 MyBatis Mapper，承载有效角色编码查询和幂等授权 SQL。
 */
@Mapper
public interface AdminUserRoleDefineMapper {

    /**
     * 查询管理员当前有效角色编码列表，只统计有效角色且未逻辑删除的授权关系。
     *
     * @param adminUserId 后台管理员业务主键。
     * @return 有效角色编码列表，无角色时返回空列表。
     */
    List<String> selectActiveRoleCodesByAdminUserId(@Param("adminUserId") Long adminUserId);

    /**
     * 幂等授予管理员角色，命中有效唯一约束时跳过写入，供受控初始化入口重复执行。
     *
     * @param relationId 管理员角色关联业务主键。
     * @param adminUserId 后台管理员业务主键。
     * @param roleId 后台角色业务主键。
     * @param grantedTime 授权时间。
     * @return 实际新增行数，已存在有效授权时返回 0。
     */
    int insertIgnoreDuplicate(
            @Param("relationId") Long relationId,
            @Param("adminUserId") Long adminUserId,
            @Param("roleId") Long roleId,
            @Param("grantedTime") OffsetDateTime grantedTime
    );
}
