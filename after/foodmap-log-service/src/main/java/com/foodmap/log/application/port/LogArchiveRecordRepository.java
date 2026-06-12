package com.foodmap.log.application.port;

import com.foodmap.log.infrastructure.persistence.entity.LogArchiveRecordEntity;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 日志归档记录仓储端口，隔离归档规划服务与 MyBatis 持久化实现。
 */
public interface LogArchiveRecordRepository {

    /**
     * 幂等保存日志归档记录。
     *
     * @param entity 日志归档记录持久化实体。
     * @return 实际新增行数，已存在同一归档窗口时返回 0。
     */
    int saveIgnoreDuplicate(LogArchiveRecordEntity entity);

    /**
     * 查询下一条待执行的日志归档记录。
     *
     * @return 待执行归档记录，不存在时为空。
     */
    Optional<LogArchiveRecordEntity> findNextPending();

    /**
     * 将日志归档记录标记为执行中。
     *
     * @param archiveId 日志归档记录业务主键。
     * @param startedTime 归档开始时间。
     * @return 实际更新行数。
     */
    int markRunning(Long archiveId, OffsetDateTime startedTime);

    /**
     * 将日志归档记录标记为成功。
     *
     * @param archiveId 日志归档记录业务主键。
     * @param completedTime 归档完成时间。
     * @return 实际更新行数。
     */
    int markSuccess(Long archiveId, OffsetDateTime completedTime);

    /**
     * 将日志归档记录标记为失败，并记录失败摘要。
     *
     * @param archiveId 日志归档记录业务主键。
     * @param failureReason 归档失败原因摘要。
     * @param completedTime 归档完成时间。
     * @return 实际更新行数。
     */
    int markFailed(Long archiveId, String failureReason, OffsetDateTime completedTime);
}
