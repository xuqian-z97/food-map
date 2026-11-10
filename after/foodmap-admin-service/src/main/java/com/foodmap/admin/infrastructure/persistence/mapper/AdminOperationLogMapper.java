package com.foodmap.admin.infrastructure.persistence.mapper;

import com.foodmap.admin.infrastructure.persistence.entity.AdminOperationLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * `admin_operation_logs` 标准 MyBatis Mapper，只承载后台操作记录表的单表模板 SQL。
 */
@Mapper
public interface AdminOperationLogMapper {

    /**
     * 按数据库内部主键查询后台操作记录。
     *
     * @param id 数据库内部主键。
     * @return 后台操作记录实体，未命中返回 null。
     */
    AdminOperationLogEntity selectById(@Param("id") Long id);

    /**
     * 按后台操作业务主键查询后台操作记录。
     *
     * @param operationId 后台操作业务主键。
     * @return 后台操作记录实体，未命中返回 null。
     */
    AdminOperationLogEntity selectByBizId(@Param("operationId") Long operationId);

    /**
     * 按有限条件查询后台操作记录列表。
     *
     * @param condition 查询条件实体。
     * @return 后台操作记录列表。
     */
    List<AdminOperationLogEntity> selectListByCondition(@Param("condition") AdminOperationLogEntity condition);

    /**
     * 按有限条件分页查询后台操作记录列表。
     *
     * @param condition 查询条件实体。
     * @param limit 分页大小。
     * @param offset 分页偏移量。
     * @return 后台操作记录列表。
     */
    List<AdminOperationLogEntity> selectPageByCondition(
            @Param("condition") AdminOperationLogEntity condition,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    /**
     * 单条新增后台操作记录。
     *
     * @param entity 后台操作记录实体。
     * @return 影响行数。
     */
    int insertOne(AdminOperationLogEntity entity);

    /**
     * 批量新增后台操作记录。
     *
     * @param items 后台操作记录实体列表。
     * @return 影响行数。
     */
    int insertBatch(@Param("items") List<AdminOperationLogEntity> items);

    /**
     * 按数据库内部主键更新后台操作记录。
     *
     * @param entity 后台操作记录实体。
     * @return 影响行数。
     */
    int updateById(AdminOperationLogEntity entity);

    /**
     * 按后台操作业务主键更新后台操作记录。
     *
     * @param entity 后台操作记录实体。
     * @return 影响行数。
     */
    int updateByBizId(AdminOperationLogEntity entity);

    /**
     * 按后台操作业务主键批量更新后台操作记录。
     *
     * @param items 后台操作记录实体列表。
     * @return 影响行数。
     */
    int updateBatchByBizId(@Param("items") List<AdminOperationLogEntity> items);

    /**
     * 按数据库内部主键逻辑删除后台操作记录。
     *
     * @param id 数据库内部主键。
     * @param updatedTime 更新时间。
     * @return 影响行数。
     */
    int logicDeleteById(@Param("id") Long id, @Param("updatedTime") OffsetDateTime updatedTime);

    /**
     * 按后台操作业务主键批量逻辑删除后台操作记录。
     *
     * @param operationIds 后台操作业务主键列表。
     * @param updatedTime 更新时间。
     * @return 影响行数。
     */
    int logicDeleteByBizIds(@Param("operationIds") List<Long> operationIds, @Param("updatedTime") OffsetDateTime updatedTime);
}
