package com.foodmap.log.infrastructure.persistence.mapper;

import com.foodmap.log.application.port.ApiAccessLogQueryCriteria;
import com.foodmap.log.infrastructure.persistence.entity.ApiAccessLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 接口访问摘要日志 MyBatis Mapper，只包含 `api_access_log` 单表写入 SQL。
 */
@Mapper
public interface ApiAccessLogMapper {

    /**
     * 幂等新增接口访问摘要。
     *
     * @param entity 接口访问摘要日志实体。
     * @return 实际新增行数，重复 Kafka 位点时返回 0。
     */
    int insertIgnoreDuplicate(ApiAccessLogEntity entity);

    /**
     * 物理删除超过保留周期的接口访问摘要。
     *
     * @param cutoffTime 保留边界时间，早于该时间的摘要会被删除。
     * @return 实际删除行数。
     */
    int deleteOccurredBefore(@Param("cutoffTime") OffsetDateTime cutoffTime);

    /**
     * 按条件分页查询接口访问摘要。
     *
     * @param criteria 查询条件和分页参数。
     * @return 当前页接口访问摘要实体列表。
     */
    List<ApiAccessLogEntity> selectByCriteria(@Param("criteria") ApiAccessLogQueryCriteria criteria);

    /**
     * 统计符合条件的接口访问摘要数量。
     *
     * @param criteria 查询条件。
     * @return 符合条件的记录数。
     */
    long countByCriteria(@Param("criteria") ApiAccessLogQueryCriteria criteria);
}
