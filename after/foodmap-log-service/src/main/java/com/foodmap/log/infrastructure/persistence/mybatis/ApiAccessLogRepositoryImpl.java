package com.foodmap.log.infrastructure.persistence.mybatis;

import com.foodmap.log.application.port.ApiAccessLogRepository;
import com.foodmap.log.application.port.ApiAccessLogQueryCriteria;
import com.foodmap.log.infrastructure.persistence.entity.ApiAccessLogEntity;
import com.foodmap.log.infrastructure.persistence.mapper.ApiAccessLogMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 接口访问摘要仓储实现，通过 MyBatis 写入独立日志 PostgreSQL。
 */
@Repository
public class ApiAccessLogRepositoryImpl implements ApiAccessLogRepository {
    private final ApiAccessLogMapper mapper;

    /**
     * 创建接口访问摘要仓储实现。
     *
     * @param mapper 接口访问摘要 Mapper。
     */
    public ApiAccessLogRepositoryImpl(ApiAccessLogMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 幂等保存接口访问摘要。
     *
     * @param entity 接口访问摘要持久化实体。
     * @return 实际新增行数，重复 Kafka 位点时返回 0。
     */
    @Override
    public int saveIgnoreDuplicate(ApiAccessLogEntity entity) {
        return mapper.insertIgnoreDuplicate(entity);
    }

    /**
     * 删除早于指定发生时间的接口访问摘要。
     *
     * @param cutoffTime 保留边界时间，早于该时间的摘要会被物理删除。
     * @return 实际删除行数。
     */
    @Override
    public int deleteOccurredBefore(OffsetDateTime cutoffTime) {
        return mapper.deleteOccurredBefore(cutoffTime);
    }

    /**
     * 按条件分页查询接口访问摘要。
     *
     * @param criteria 查询条件和分页参数。
     * @return 当前页接口访问摘要实体列表。
     */
    @Override
    public List<ApiAccessLogEntity> search(ApiAccessLogQueryCriteria criteria) {
        return mapper.selectByCriteria(criteria);
    }

    /**
     * 统计符合条件的接口访问摘要数量。
     *
     * @param criteria 查询条件。
     * @return 符合条件的记录数。
     */
    @Override
    public long count(ApiAccessLogQueryCriteria criteria) {
        return mapper.countByCriteria(criteria);
    }
}
