package com.foodmap.log.application.port;

import com.foodmap.log.infrastructure.persistence.entity.ApiAccessLogEntity;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 接口访问摘要仓储端口，隔离应用服务与 MyBatis 等持久化实现。
 */
public interface ApiAccessLogRepository {

    /**
     * 幂等保存接口访问摘要。
     *
     * @param entity 接口访问摘要持久化实体。
     * @return 实际新增行数，重复 Kafka 位点时返回 0。
     */
    int saveIgnoreDuplicate(ApiAccessLogEntity entity);

    /**
     * 删除早于指定发生时间的接口访问摘要。
     *
     * @param cutoffTime 保留边界时间，早于该时间的摘要会被物理删除。
     * @return 实际删除行数。
     */
    int deleteOccurredBefore(OffsetDateTime cutoffTime);

    /**
     * 按条件分页查询接口访问摘要。
     *
     * @param criteria 查询条件和分页参数。
     * @return 当前页接口访问摘要实体列表。
     */
    List<ApiAccessLogEntity> search(ApiAccessLogQueryCriteria criteria);

    /**
     * 统计符合条件的接口访问摘要数量。
     *
     * @param criteria 查询条件。
     * @return 符合条件的记录数。
     */
    long count(ApiAccessLogQueryCriteria criteria);
}
