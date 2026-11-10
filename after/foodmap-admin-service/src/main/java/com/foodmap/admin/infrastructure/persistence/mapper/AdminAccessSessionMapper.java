package com.foodmap.admin.infrastructure.persistence.mapper;

import com.foodmap.admin.infrastructure.persistence.entity.AdminAccessSessionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * `admin_access_sessions` 标准 MyBatis Mapper，只承载后台 Token 会话登记表的单表模板 SQL。
 */
@Mapper
public interface AdminAccessSessionMapper {

    /**
     * 按数据库内部主键查询后台 Token 会话登记。
     *
     * @param id 数据库内部主键。
     * @return 后台 Token 会话登记实体，未命中返回 null。
     */
    AdminAccessSessionEntity selectById(@Param("id") Long id);

    /**
     * 按后台会话业务主键查询后台 Token 会话登记。
     *
     * @param sessionId 后台会话业务主键。
     * @return 后台 Token 会话登记实体，未命中返回 null。
     */
    AdminAccessSessionEntity selectByBizId(@Param("sessionId") Long sessionId);

    /**
     * 按有限条件查询后台 Token 会话登记列表。
     *
     * @param condition 查询条件实体。
     * @return 后台 Token 会话登记列表。
     */
    List<AdminAccessSessionEntity> selectListByCondition(@Param("condition") AdminAccessSessionEntity condition);

    /**
     * 按有限条件分页查询后台 Token 会话登记列表。
     *
     * @param condition 查询条件实体。
     * @param limit 分页大小。
     * @param offset 分页偏移量。
     * @return 后台 Token 会话登记列表。
     */
    List<AdminAccessSessionEntity> selectPageByCondition(
            @Param("condition") AdminAccessSessionEntity condition,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    /**
     * 单条新增后台 Token 会话登记。
     *
     * @param entity 后台 Token 会话登记实体。
     * @return 影响行数。
     */
    int insertOne(AdminAccessSessionEntity entity);

    /**
     * 批量新增后台 Token 会话登记。
     *
     * @param items 后台 Token 会话登记实体列表。
     * @return 影响行数。
     */
    int insertBatch(@Param("items") List<AdminAccessSessionEntity> items);

    /**
     * 按数据库内部主键更新后台 Token 会话登记。
     *
     * @param entity 后台 Token 会话登记实体。
     * @return 影响行数。
     */
    int updateById(AdminAccessSessionEntity entity);

    /**
     * 按后台会话业务主键更新后台 Token 会话登记。
     *
     * @param entity 后台 Token 会话登记实体。
     * @return 影响行数。
     */
    int updateByBizId(AdminAccessSessionEntity entity);

    /**
     * 按后台会话业务主键批量更新后台 Token 会话登记。
     *
     * @param items 后台 Token 会话登记实体列表。
     * @return 影响行数。
     */
    int updateBatchByBizId(@Param("items") List<AdminAccessSessionEntity> items);

    /**
     * 按数据库内部主键逻辑删除后台 Token 会话登记。
     *
     * @param id 数据库内部主键。
     * @param updatedTime 更新时间。
     * @return 影响行数。
     */
    int logicDeleteById(@Param("id") Long id, @Param("updatedTime") OffsetDateTime updatedTime);

    /**
     * 按后台会话业务主键批量逻辑删除后台 Token 会话登记。
     *
     * @param sessionIds 后台会话业务主键列表。
     * @param updatedTime 更新时间。
     * @return 影响行数。
     */
    int logicDeleteByBizIds(@Param("sessionIds") List<Long> sessionIds, @Param("updatedTime") OffsetDateTime updatedTime);
}
