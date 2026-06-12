package com.foodmap.log.service;

import com.foodmap.common.logging.LogField;
import com.foodmap.common.logging.SafeLog;
import com.foodmap.log.application.port.LogArchiveExportClient;
import com.foodmap.log.application.port.LogArchiveObjectStorageClient;
import com.foodmap.log.application.port.LogArchivePayload;
import com.foodmap.log.application.port.LogArchiveRecordRepository;
import com.foodmap.log.infrastructure.persistence.entity.LogArchiveRecordEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 日志归档执行服务，负责驱动 PENDING 归档记录完成导出、上传和状态流转。
 */
@Service
public class LogArchiveExecutionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogArchiveExecutionService.class);
    private static final int FAILURE_REASON_MAX_LENGTH = 500;

    private final LogArchiveRecordRepository repository;
    private final LogArchiveExportClient exportClient;
    private final LogArchiveObjectStorageClient objectStorageClient;
    private final Clock clock;

    /**
     * 创建日志归档执行服务。
     *
     * @param repository 日志归档记录仓储端口。
     * @param exportClient 全量日志导出端口。
     * @param objectStorageClient 日志归档对象存储端口。
     * @param clock 当前时间来源，生产使用 UTC 系统时钟，测试使用固定时钟。
     */
    public LogArchiveExecutionService(
            LogArchiveRecordRepository repository,
            LogArchiveExportClient exportClient,
            LogArchiveObjectStorageClient objectStorageClient,
            Clock clock
    ) {
        this.repository = repository;
        this.exportClient = exportClient;
        this.objectStorageClient = objectStorageClient;
        this.clock = clock;
    }

    /**
     * 执行下一条待归档记录。
     *
     * @return 成功归档条数，没有待执行或执行失败时返回 0。
     */
    public int executeNextPendingArchive() {
        Optional<LogArchiveRecordEntity> optionalRecord = repository.findNextPending();
        if (optionalRecord.isEmpty()) {
            return 0;
        }
        LogArchiveRecordEntity record = optionalRecord.get();
        OffsetDateTime now = OffsetDateTime.now(clock);
        int claimedRows = repository.markRunning(record.getArchiveId(), now);
        if (claimedRows == 0) {
            return 0;
        }
        try {
            LogArchivePayload payload = exportClient.export(record);
            objectStorageClient.upload(record, payload);
            repository.markSuccess(record.getArchiveId(), OffsetDateTime.now(clock));
            SafeLog.info(
                    LOGGER,
                    "log_archive.execution.completed",
                    LogField.of("archiveId", record.getArchiveId()),
                    LogField.of("objectKey", record.getObjectKey())
            );
            return 1;
        } catch (RuntimeException ex) {
            repository.markFailed(record.getArchiveId(), summarizeFailure(ex), OffsetDateTime.now(clock));
            SafeLog.warn(
                    LOGGER,
                    "log_archive.execution.failed",
                    LogField.of("archiveId", record.getArchiveId()),
                    LogField.of("failureReason", summarizeFailure(ex))
            );
            return 0;
        }
    }

    /**
     * 生成适合落库的失败原因摘要。
     *
     * @param throwable 归档执行异常。
     * @return 截断后的失败原因摘要。
     */
    private String summarizeFailure(Throwable throwable) {
        String message = throwable.getMessage();
        String summary = throwable.getClass().getSimpleName() + ": " + (message == null ? "archive execution failed" : message);
        return summary.length() <= FAILURE_REASON_MAX_LENGTH ? summary : summary.substring(0, FAILURE_REASON_MAX_LENGTH);
    }
}
