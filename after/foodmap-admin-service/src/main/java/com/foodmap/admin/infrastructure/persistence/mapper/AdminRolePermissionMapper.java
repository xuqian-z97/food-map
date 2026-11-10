package com.foodmap.admin.infrastructure.persistence.mapper;

import com.foodmap.admin.infrastructure.persistence.entity.AdminRolePermissionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * `admin_role_permissions` 标准 MyBatis Mapper，只承载后台角色权限关联表的单表模板 SQL。
 */
@Mapper
public interface AdminRolePermissionMapper {

    /**
     * 按数据库内部主键查询后台角色权限关联。
     *
     * @param id 数据库内部主键。
     * @return 后台角色权限关联实体，未命中返回 null。
     */
    AdminRolePermissionEntity selectById(@Param("id") Long id);

    /**
     * 按角色权限关联业务主键查询后台角色权限关联。
     *
     * @param relationId 角色权限关联业务主键。
     * @return 后台角色权限关联实体，未命中返回 null。
     */
    AdminRolePermissionEntity selectByBizId(@Param("relationId") Long relationId);

    /**
     * 按有限条件查询后台角色权限关联列表。
     *
     * @param condition 查询条件实体。
     * @return 后台角色权限关联列表。
     */
    List<AdminRolePermissionEntity> selectListByCondition(@Param("condition") AdminRolePermissionEntity condition);

    /**
     * 按有限条件分页查询后台角色权限关联列表。
     *
     * @param condition 查询条件实体。
     * @param limit 分页大小。
     * @param offset 分页偏移量。
     * @return 后台角色权限关联列表。
     */
    List<AdminRolePermissionEntity> selectPageByCondition(
            @Param("condition") AdminRolePermissionEntity condition,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    /**
     * 单条新增后台角色权限关联。
     *
     * @param entity 后台角色权限关联实体。
     * @return 影响行数。
     */
    int insertOne(AdminRolePermissionEntity entity);

    /**
     * 批量新增后台角色权限关联。
     *
     * @param items 后台角色权限关联实体列表。
     * @return 影响行数。
     */
    int insertBatch(@Param("items") List<AdminRolePermissionEntity> items);

    /**
     * 按数据库内部主键更新后台角色权限关联。
     *
     * @param entity 后台角色权限关联实体。
     * @return 影响行数。
     */
    int updateById(AdminRolePermissionEntity entity);

    /**
     * 按角色权限关联业务主键更新后台角色权限关联。
     *
     * @param entity 后台角色权限关联实体。
     * @return 影响行数。
     */
    int updateByBizId(AdminRolePermissionEntity entity);

    /**
     * 按角色权限关联业务主键批量更新后台角色权限关联。
     *
     * @param items 后台角色权限关联实体列表。
     * @return 影响行数。
     */
    int updateBatchByBizId(@Param("items") List<AdminRolePermissionEntity> items);

    /**
     * 按数据库内部主键逻辑删除后台角色权限关联。
     *
     * @param id 数据库内部主键。
     * @param updatedTime 更新时间。
     * @return 影响行数。
     */
    int logicDeleteById(@Param("id") Long id, @Param("updatedTime") OffsetDateTime updatedTime);

    /**
     * 按角色权限关联业务主键批量逻辑删除后台角色权限关联。
     *
     * @param relationIds 角色权限关联业务主键列表。
     * @param updatedTime 更新时间。
     * @return 影响行数。
     */
    int logicDeleteByBizIds(@Param("relationIds") List<Long> relationIds, @Param("updatedTime") OffsetDateTime updatedTime);
}
