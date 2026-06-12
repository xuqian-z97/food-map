package com.foodmap.log.service;

import com.foodmap.log.application.port.ApiAccessLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * 接口访问摘要保留策略服务，负责计算保留边界并清理过期 `api_access_log` 数据。
 */
@Service
public class ApiAccessLogRetentionService {
    private final ApiAccessLogRepository repository;
    private final Clock clock;
    private final int retentionDays;

    /**
     * 创建接口访问摘要保留策略服务。
     *
     * @param repository 接口访问摘要仓储端口。
     * @param clock 当前时间来源，生产使用 UTC 系统时钟，测试使用固定时钟。
     * @param retentionDays 接口访问摘要保留天数，小于 1 时按 1 天兜底。
     */
    public ApiAccessLogRetentionService(
            ApiAccessLogRepository repository,
            Clock clock,
            @Value("${foodmap.log.api-access.retention-days:15}") int retentionDays
    ) {
        this.repository = repository;
        this.clock = clock;
        this.retentionDays = Math.max(1, retentionDays);
    }

    /**
     * 删除超过保留周期的接口访问摘要。
     *
     * @return 实际删除行数。
     */
    @Transactional(rollbackFor = Exception.class)
    public int deleteExpiredApiAccessLogs() {
        OffsetDateTime cutoffTime = OffsetDateTime.now(clock).minusDays(retentionDays);
        return repository.deleteOccurredBefore(cutoffTime);
    }

    /**
     * 返回当前生效的接口访问摘要保留天数。
     *
     * @return 保留天数。
     */
    public int getRetentionDays() {
        return retentionDays;
    }
}
