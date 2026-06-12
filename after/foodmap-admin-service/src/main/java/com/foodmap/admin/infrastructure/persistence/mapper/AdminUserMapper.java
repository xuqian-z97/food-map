package com.foodmap.admin.infrastructure.persistence.mapper;

import com.foodmap.admin.infrastructure.persistence.entity.AdminUserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * `admin_users` 标准 MyBatis Mapper，只承载后台管理员表的单表模板 SQL。
 */
@Mapper
public interface AdminUserMapper {

    /**
     * 按数据库内部主键查询后台管理员。
     *
     * @param id 数据库内部主键。
     * @return 后台管理员实体，未命中返回 null。
     */
    AdminUserEntity selectById(@Param("id") Long id);

    /**
     * 按后台管理员业务主键查询后台管理员。
     *
     * @param adminUserId 后台管理员业务主键。
     * @return 后台管理员实体，未命中返回 null。
     */
    AdminUserEntity selectByBizId(@Param("adminUserId") Long adminUserId);

    /**
     * 按登录账号名查询后台管理员。
     *
     * @param username 后台登录账号名。
     * @return 后台管理员实体，未命中返回 null。
     */
    AdminUserEntity selectByUsername(@Param("username") String username);

    /**
     * 按有限条件查询后台管理员列表。
     *
     * @param condition 查询条件实体。
     * @return 后台管理员列表。
     */
    List<AdminUserEntity> selectListByCondition(@Param("condition") AdminUserEntity condition);

    /**
     * 按有限条件分页查询后台管理员列表。
     *
     * @param condition 查询条件实体。
     * @param limit 分页大小。
     * @param offset 分页偏移量。
     * @return 后台管理员列表。
     */
    List<AdminUserEntity> selectPageByCondition(
            @Param("condition") AdminUserEntity condition,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    /**
     * 单条新增后台管理员。
     *
     * @param entity 后台管理员实体。
     * @return 影响行数。
     */
    int insertOne(AdminUserEntity entity);

    /**
     * 批量新增后台管理员。
     *
     * @param items 后台管理员实体列表。
     * @return 影响行数。
     */
    int insertBatch(@Param("items") List<AdminUserEntity> items);

    /**
     * 按数据库内部主键更新后台管理员。
     *
     * @param entity 后台管理员实体。
     * @return 影响行数。
     */
    int updateById(AdminUserEntity entity);

    /**
     * 按后台管理员业务主键更新后台管理员。
     *
     * @param entity 后台管理员实体。
     * @return 影响行数。
     */
    int updateByBizId(AdminUserEntity entity);

    /**
     * 按后台管理员业务主键批量更新后台管理员。
     *
     * @param items 后台管理员实体列表。
     * @return 影响行数。
     */
    int updateBatchByBizId(@Param("items") List<AdminUserEntity> items);

    /**
     * 按数据库内部主键逻辑删除后台管理员。
     *
     * @param id 数据库内部主键。
     * @param updatedTime 更新时间。
     * @return 影响行数。
     */
    int logicDeleteById(@Param("id") Long id, @Param("updatedTime") OffsetDateTime updatedTime);

    /**
     * 按后台管理员业务主键批量逻辑删除后台管理员。
     *
     * @param adminUserIds 后台管理员业务主键列表。
     * @param updatedTime 更新时间。
     * @return 影响行数。
     */
    int logicDeleteByBizIds(@Param("adminUserIds") List<Long> adminUserIds, @Param("updatedTime") OffsetDateTime updatedTime);
}
