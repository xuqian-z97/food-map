package com.foodmap.admin.infrastructure.persistence.mapper;

import com.foodmap.admin.infrastructure.persistence.entity.AdminPermissionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * `admin_permissions` 标准 MyBatis Mapper，只承载后台权限表的单表模板 SQL。
 */
@Mapper
public interface AdminPermissionMapper {

    /**
     * 按数据库内部主键查询后台权限。
     *
     * @param id 数据库内部主键。
     * @return 后台权限实体，未命中返回 null。
     */
    AdminPermissionEntity selectById(@Param("id") Long id);

    /**
     * 按后台权限业务主键查询后台权限。
     *
     * @param permissionId 后台权限业务主键。
     * @return 后台权限实体，未命中返回 null。
     */
    AdminPermissionEntity selectByBizId(@Param("permissionId") Long permissionId);

    /**
     * 按有限条件查询后台权限列表。
     *
     * @param condition 查询条件实体。
     * @return 后台权限列表。
     */
    List<AdminPermissionEntity> selectListByCondition(@Param("condition") AdminPermissionEntity condition);

    /**
     * 按有限条件分页查询后台权限列表。
     *
     * @param condition 查询条件实体。
     * @param limit 分页大小。
     * @param offset 分页偏移量。
     * @return 后台权限列表。
     */
    List<AdminPermissionEntity> selectPageByCondition(
            @Param("condition") AdminPermissionEntity condition,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    /**
     * 单条新增后台权限。
     *
     * @param entity 后台权限实体。
     * @return 影响行数。
     */
    int insertOne(AdminPermissionEntity entity);

    /**
     * 批量新增后台权限。
     *
     * @param items 后台权限实体列表。
     * @return 影响行数。
     */
    int insertBatch(@Param("items") List<AdminPermissionEntity> items);

    /**
     * 按数据库内部主键更新后台权限。
     *
     * @param entity 后台权限实体。
     * @return 影响行数。
     */
    int updateById(AdminPermissionEntity entity);

    /**
     * 按后台权限业务主键更新后台权限。
     *
     * @param entity 后台权限实体。
     * @return 影响行数。
     */
    int updateByBizId(AdminPermissionEntity entity);

    /**
     * 按后台权限业务主键批量更新后台权限。
     *
     * @param items 后台权限实体列表。
     * @return 影响行数。
     */
    int updateBatchByBizId(@Param("items") List<AdminPermissionEntity> items);

    /**
     * 按数据库内部主键逻辑删除后台权限。
     *
     * @param id 数据库内部主键。
     * @param updatedTime 更新时间。
     * @return 影响行数。
     */
    int logicDeleteById(@Param("id") Long id, @Param("updatedTime") OffsetDateTime updatedTime);

    /**
     * 按后台权限业务主键批量逻辑删除后台权限。
     *
     * @param permissionIds 后台权限业务主键列表。
     * @param updatedTime 更新时间。
     * @return 影响行数。
     */
    int logicDeleteByBizIds(@Param("permissionIds") List<Long> permissionIds, @Param("updatedTime") OffsetDateTime updatedTime);
}
