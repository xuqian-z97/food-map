package com.foodmap.log.infrastructure.persistence.mybatis;

import com.foodmap.log.application.port.LogArchiveRecordRepository;
import com.foodmap.log.infrastructure.persistence.entity.LogArchiveRecordEntity;
import com.foodmap.log.infrastructure.persistence.mapper.LogArchiveRecordMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 日志归档记录仓储实现，通过 MyBatis 写入独立日志 PostgreSQL。
 */
@Repository
public class LogArchiveRecordRepositoryImpl implements LogArchiveRecordRepository {
    private final LogArchiveRecordMapper mapper;

    /**
     * 创建日志归档记录仓储实现。
     *
     * @param mapper 日志归档记录 Mapper。
     */
    public LogArchiveRecordRepositoryImpl(LogArchiveRecordMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 幂等保存日志归档记录。
     *
     * @param entity 日志归档记录持久化实体。
     * @return 实际新增行数，已存在同一归档窗口时返回 0。
     */
    @Override
    public int saveIgnoreDuplicate(LogArchiveRecordEntity entity) {
        return mapper.insertIgnoreDuplicate(entity);
    }

    /**
     * 查询下一条待执行的日志归档记录。
     *
     * @return 待执行归档记录，不存在时为空。
     */
    @Override
    public Optional<LogArchiveRecordEntity> findNextPending() {
        return mapper.selectNextPending();
    }

    /**
     * 将日志归档记录标记为执行中。
     *
     * @param archiveId 日志归档记录业务主键。
     * @param startedTime 归档开始时间。
     * @return 实际更新行数。
     */
    @Override
    public int markRunning(Long archiveId, OffsetDateTime startedTime) {
        return mapper.markRunning(archiveId, startedTime);
    }

    /**
     * 将日志归档记录标记为成功。
     *
     * @param archiveId 日志归档记录业务主键。
     * @param completedTime 归档完成时间。
     * @return 实际更新行数。
     */
    @Override
    public int markSuccess(Long archiveId, OffsetDateTime completedTime) {
        return mapper.markSuccess(archiveId, completedTime);
    }

    /**
     * 将日志归档记录标记为失败。
     *
     * @param archiveId 日志归档记录业务主键。
     * @param failureReason 归档失败原因摘要。
     * @param completedTime 归档完成时间。
     * @return 实际更新行数。
     */
    @Override
    public int markFailed(Long archiveId, String failureReason, OffsetDateTime completedTime) {
        return mapper.markFailed(archiveId, failureReason, completedTime);
    }
}
