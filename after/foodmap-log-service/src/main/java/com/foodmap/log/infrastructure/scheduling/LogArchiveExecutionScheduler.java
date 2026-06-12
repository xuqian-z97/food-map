package com.foodmap.log.infrastructure.scheduling;

import com.foodmap.common.logging.LogField;
import com.foodmap.common.logging.SafeLog;
import com.foodmap.log.service.LogArchiveExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 日志归档执行调度器，默认关闭，开启后执行一条 PENDING 归档记录。
 */
@Component
@ConditionalOnProperty(prefix = "foodmap.log.archive.execution", name = "enabled", havingValue = "true")
public class LogArchiveExecutionScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogArchiveExecutionScheduler.class);

    private final LogArchiveExecutionService executionService;

    /**
     * 创建日志归档执行调度器。
     *
     * @param executionService 日志归档执行服务。
     */
    public LogArchiveExecutionScheduler(LogArchiveExecutionService executionService) {
        this.executionService = executionService;
    }

    /**
     * 执行一次日志归档任务。
     */
    @Scheduled(cron = "${foodmap.log.archive.execution.cron:0 */10 * * * *}", zone = "${foodmap.log.archive.execution.zone:Asia/Shanghai}")
    public void runExecution() {
        int executedRows = executionService.executeNextPendingArchive();
        SafeLog.info(
                LOGGER,
                "log_archive.execution.tick.completed",
                LogField.of("executedRows", executedRows)
        );
    }
}
