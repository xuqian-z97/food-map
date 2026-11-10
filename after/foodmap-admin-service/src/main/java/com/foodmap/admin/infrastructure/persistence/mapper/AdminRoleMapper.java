package com.foodmap.admin.infrastructure.persistence.mapper;

import com.foodmap.admin.infrastructure.persistence.entity.AdminRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * `admin_roles` 标准 MyBatis Mapper，只承载后台角色表的单表模板 SQL。
 */
@Mapper
public interface AdminRoleMapper {

    /**
     * 按数据库内部主键查询后台角色。
     *
     * @param id 数据库内部主键。
     * @return 后台角色实体，未命中返回 null。
     */
    AdminRoleEntity selectById(@Param("id") Long id);

    /**
     * 按后台角色业务主键查询后台角色。
     *
     * @param roleId 后台角色业务主键。
     * @return 后台角色实体，未命中返回 null。
     */
    AdminRoleEntity selectByBizId(@Param("roleId") Long roleId);

    /**
     * 按有限条件查询后台角色列表。
     *
     * @param condition 查询条件实体。
     * @return 后台角色列表。
     */
    List<AdminRoleEntity> selectListByCondition(@Param("condition") AdminRoleEntity condition);

    /**
     * 按有限条件分页查询后台角色列表。
     *
     * @param condition 查询条件实体。
     * @param limit 分页大小。
     * @param offset 分页偏移量。
     * @return 后台角色列表。
     */
    List<AdminRoleEntity> selectPageByCondition(
            @Param("condition") AdminRoleEntity condition,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    /**
     * 单条新增后台角色。
     *
     * @param entity 后台角色实体。
     * @return 影响行数。
     */
    int insertOne(AdminRoleEntity entity);

    /**
     * 批量新增后台角色。
     *
     * @param items 后台角色实体列表。
     * @return 影响行数。
     */
    int insertBatch(@Param("items") List<AdminRoleEntity> items);

    /**
     * 按数据库内部主键更新后台角色。
     *
     * @param entity 后台角色实体。
     * @return 影响行数。
     */
    int updateById(AdminRoleEntity entity);

    /**
     * 按后台角色业务主键更新后台角色。
     *
     * @param entity 后台角色实体。
     * @return 影响行数。
     */
    int updateByBizId(AdminRoleEntity entity);

    /**
     * 按后台角色业务主键批量更新后台角色。
     *
     * @param items 后台角色实体列表。
     * @return 影响行数。
     */
    int updateBatchByBizId(@Param("items") List<AdminRoleEntity> items);

    /**
     * 按数据库内部主键逻辑删除后台角色。
     *
     * @param id 数据库内部主键。
     * @param updatedTime 更新时间。
     * @return 影响行数。
     */
    int logicDeleteById(@Param("id") Long id, @Param("updatedTime") OffsetDateTime updatedTime);

    /**
     * 按后台角色业务主键批量逻辑删除后台角色。
     *
     * @param roleIds 后台角色业务主键列表。
     * @param updatedTime 更新时间。
     * @return 影响行数。
     */
    int logicDeleteByBizIds(@Param("roleIds") List<Long> roleIds, @Param("updatedTime") OffsetDateTime updatedTime);
}
