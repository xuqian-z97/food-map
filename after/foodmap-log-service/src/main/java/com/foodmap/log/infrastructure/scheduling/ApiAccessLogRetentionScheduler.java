package com.foodmap.log.infrastructure.scheduling;

import com.foodmap.common.logging.LogField;
import com.foodmap.common.logging.SafeLog;
import com.foodmap.log.service.ApiAccessLogRetentionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 接口访问摘要保留清理调度器，默认关闭，开启后按 cron 清理超过保留周期的数据。
 */
@Component
@ConditionalOnProperty(prefix = "foodmap.log.api-access.retention.cleanup", name = "enabled", havingValue = "true")
public class ApiAccessLogRetentionScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiAccessLogRetentionScheduler.class);

    private final ApiAccessLogRetentionService retentionService;

    /**
     * 创建接口访问摘要保留清理调度器。
     *
     * @param retentionService 接口访问摘要保留策略服务。
     */
    public ApiAccessLogRetentionScheduler(ApiAccessLogRetentionService retentionService) {
        this.retentionService = retentionService;
    }

    /**
     * 执行一次接口访问摘要保留清理任务。
     */
    @Scheduled(cron = "${foodmap.log.api-access.retention.cleanup.cron:0 20 3 * * *}", zone = "${foodmap.log.api-access.retention.cleanup.zone:Asia/Shanghai}")
    public void runCleanup() {
        int deletedRows = retentionService.deleteExpiredApiAccessLogs();
        SafeLog.info(
                LOGGER,
                "api_access_log.retention.cleanup.completed",
                LogField.of("retentionDays", retentionService.getRetentionDays()),
                LogField.of("deletedRows", deletedRows)
        );
    }
}
