package com.foodmap.log.infrastructure.scheduling;

import com.foodmap.common.logging.LogField;
import com.foodmap.common.logging.SafeLog;
import com.foodmap.log.service.LogArchivePlanningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 日志归档计划调度器，默认关闭，开启后为超过热保留周期的全量日志生成归档计划。
 */
@Component
@ConditionalOnProperty(prefix = "foodmap.log.archive.planning", name = "enabled", havingValue = "true")
public class LogArchivePlanningScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogArchivePlanningScheduler.class);

    private final LogArchivePlanningService planningService;

    /**
     * 创建日志归档计划调度器。
     *
     * @param planningService 日志归档计划服务。
     */
    public LogArchivePlanningScheduler(LogArchivePlanningService planningService) {
        this.planningService = planningService;
    }

    /**
     * 执行一次日志归档计划生成任务。
     */
    @Scheduled(cron = "${foodmap.log.archive.planning.cron:0 40 3 * * *}", zone = "${foodmap.log.archive.planning.zone:Asia/Shanghai}")
    public void runPlanning() {
        int insertedRows = planningService.createDailyArchivePlan();
        SafeLog.info(
                LOGGER,
                "log_archive.planning.completed",
                LogField.of("hotRetentionDays", planningService.getHotRetentionDays()),
                LogField.of("insertedRows", insertedRows)
        );
    }
}
