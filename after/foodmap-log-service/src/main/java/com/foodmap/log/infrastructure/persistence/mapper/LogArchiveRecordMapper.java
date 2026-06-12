package com.foodmap.log.infrastructure.persistence.mapper;

import com.foodmap.log.infrastructure.persistence.entity.LogArchiveRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 日志归档记录 MyBatis Mapper，只包含 `log_archive_records` 单表写入 SQL。
 */
@Mapper
public interface LogArchiveRecordMapper {

    /**
     * 幂等新增日志归档记录。
     *
     * @param entity 日志归档记录实体。
     * @return 实际新增行数，已存在同一归档窗口时返回 0。
     */
    int insertIgnoreDuplicate(LogArchiveRecordEntity entity);

    /**
     * 查询下一条待执行的日志归档记录。
     *
     * @return 待执行归档记录，不存在时为空。
     */
    Optional<LogArchiveRecordEntity> selectNextPending();

    /**
     * 将日志归档记录标记为执行中。
     *
     * @param archiveId 日志归档记录业务主键。
     * @param startedTime 归档开始时间。
     * @return 实际更新行数。
     */
    int markRunning(@Param("archiveId") Long archiveId, @Param("startedTime") OffsetDateTime startedTime);

    /**
     * 将日志归档记录标记为成功。
     *
     * @param archiveId 日志归档记录业务主键。
     * @param completedTime 归档完成时间。
     * @return 实际更新行数。
     */
    int markSuccess(@Param("archiveId") Long archiveId, @Param("completedTime") OffsetDateTime completedTime);

    /**
     * 将日志归档记录标记为失败。
     *
     * @param archiveId 日志归档记录业务主键。
     * @param failureReason 归档失败原因摘要。
     * @param completedTime 归档完成时间。
     * @return 实际更新行数。
     */
    int markFailed(
            @Param("archiveId") Long archiveId,
            @Param("failureReason") String failureReason,
            @Param("completedTime") OffsetDateTime completedTime
    );
}
