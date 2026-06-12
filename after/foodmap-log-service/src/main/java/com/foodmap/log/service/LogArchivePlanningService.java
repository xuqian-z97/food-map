package com.foodmap.log.service;

import com.foodmap.log.application.port.LogArchiveRecordRepository;
import com.foodmap.log.infrastructure.persistence.entity.LogArchiveRecordEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * 日志归档计划服务，负责为超过 Elasticsearch 热保留周期的全量日志生成 OSS 归档计划。
 */
@Service
public class LogArchivePlanningService {
    private static final String ARCHIVE_TYPE_FULL_LOG_DAILY = "FULL_LOG_DAILY";
    private static final String ARCHIVE_STATUS_PENDING = "PENDING";

    private final LogArchiveRecordRepository repository;
    private final Clock clock;
    private final int hotRetentionDays;
    private final String sourceIndexPattern;
    private final String storageProvider;
    private final String bucketName;
    private final String objectPrefix;

    /**
     * 创建日志归档计划服务。
     *
     * @param repository 日志归档记录仓储端口。
     * @param clock 当前时间来源，生产使用 UTC 系统时钟，测试使用固定时钟。
     * @param hotRetentionDays Elasticsearch 热日志保留天数，小于 1 时按 1 天兜底。
     * @param sourceIndexPattern 来源 Elasticsearch 索引模式。
     * @param storageProvider 归档存储提供方。
     * @param bucketName 归档对象存储桶名称。
     * @param objectPrefix 归档对象存储 Key 前缀。
     */
    public LogArchivePlanningService(
            LogArchiveRecordRepository repository,
            Clock clock,
            @Value("${foodmap.log.archive.hot-retention-days:7}") int hotRetentionDays,
            @Value("${foodmap.log.archive.source-index-pattern:foodmap-logs-*}") String sourceIndexPattern,
            @Value("${foodmap.log.archive.storage-provider:OSS}") String storageProvider,
            @Value("${foodmap.log.archive.bucket-name:foodmap-log-archive}") String bucketName,
            @Value("${foodmap.log.archive.object-prefix:logs/full}") String objectPrefix
    ) {
        this.repository = repository;
        this.clock = clock;
        this.hotRetentionDays = Math.max(1, hotRetentionDays);
        this.sourceIndexPattern = normalizeDefault(sourceIndexPattern, "foodmap-logs-*");
        this.storageProvider = normalizeDefault(storageProvider, "OSS");
        this.bucketName = normalizeDefault(bucketName, "foodmap-log-archive");
        this.objectPrefix = trimSlashes(normalizeDefault(objectPrefix, "logs/full"));
    }

    /**
     * 为刚超过热保留周期的自然日生成归档计划。
     *
     * @return 实际新增行数，归档窗口已存在时返回 0。
     */
    @Transactional(rollbackFor = Exception.class)
    public int createDailyArchivePlan() {
        LocalDate archiveDate = OffsetDateTime.now(clock).minusDays(hotRetentionDays).toLocalDate();
        OffsetDateTime windowStart = archiveDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime windowEnd = windowStart.plusDays(1);
        LogArchiveRecordEntity entity = new LogArchiveRecordEntity();
        entity.setArchiveType(ARCHIVE_TYPE_FULL_LOG_DAILY);
        entity.setWindowStartTime(windowStart);
        entity.setWindowEndTime(windowEnd);
        entity.setSourceIndexPattern(sourceIndexPattern);
        entity.setStorageProvider(storageProvider);
        entity.setBucketName(bucketName);
        entity.setObjectKey(buildDailyObjectKey(archiveDate));
        entity.setArchiveStatus(ARCHIVE_STATUS_PENDING);
        entity.setRetryCount(0);
        return repository.saveIgnoreDuplicate(entity);
    }

    /**
     * 返回当前生效的 Elasticsearch 热日志保留天数。
     *
     * @return 热日志保留天数。
     */
    public int getHotRetentionDays() {
        return hotRetentionDays;
    }

    /**
     * 构建每日归档对象 Key。
     *
     * @param archiveDate 需要归档的 UTC 自然日。
     * @return 稳定的对象存储 Key。
     */
    private String buildDailyObjectKey(LocalDate archiveDate) {
        String dateText = archiveDate.toString();
        return objectPrefix
                + "/year=" + archiveDate.getYear()
                + "/month=" + twoDigits(archiveDate.getMonthValue())
                + "/day=" + twoDigits(archiveDate.getDayOfMonth())
                + "/foodmap-logs-" + dateText + ".jsonl.gz";
    }

    /**
     * 将数字格式化为两位字符串。
     *
     * @param value 月份或日期数字。
     * @return 两位字符串。
     */
    private String twoDigits(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    /**
     * 规范化配置值，空值时使用默认值。
     *
     * @param value 配置值。
     * @param defaultValue 默认值。
     * @return 规范化后的配置值。
     */
    private String normalizeDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    /**
     * 去除对象 Key 前缀首尾斜杠。
     *
     * @param value 对象 Key 前缀。
     * @return 去除首尾斜杠后的前缀。
     */
    private String trimSlashes(String value) {
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result.isBlank() ? "logs/full" : result;
    }
}
