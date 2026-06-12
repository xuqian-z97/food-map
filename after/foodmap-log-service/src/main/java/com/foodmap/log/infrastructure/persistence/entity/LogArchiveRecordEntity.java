package com.foodmap.log.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

import java.time.OffsetDateTime;

/**
 * 日志归档记录持久化实体，对应 `foodmap_log_db.log_archive_records`。
 */
public class LogArchiveRecordEntity extends BaseEntity {
    /**
     * 日志归档记录业务主键。
     */
    private Long archiveId;
    /**
     * 归档类型，如 FULL_LOG_DAILY。
     */
    private String archiveType;
    /**
     * 归档日志窗口开始时间，闭区间。
     */
    private OffsetDateTime windowStartTime;
    /**
     * 归档日志窗口结束时间，开区间。
     */
    private OffsetDateTime windowEndTime;
    /**
     * 来源 Elasticsearch 索引模式。
     */
    private String sourceIndexPattern;
    /**
     * 归档存储提供方，如 OSS。
     */
    private String storageProvider;
    /**
     * 归档对象存储桶名称。
     */
    private String bucketName;
    /**
     * 归档对象存储 Key。
     */
    private String objectKey;
    /**
     * 归档状态，如 PENDING、RUNNING、SUCCESS、FAILED。
     */
    private String archiveStatus;
    /**
     * 归档重试次数。
     */
    private Integer retryCount;
    /**
     * 归档失败原因摘要。
     */
    private String failureReason;
    /**
     * 归档开始时间。
     */
    private OffsetDateTime startedTime;
    /**
     * 归档完成时间。
     */
    private OffsetDateTime completedTime;

    public Long getArchiveId() {
        return archiveId;
    }

    public void setArchiveId(Long archiveId) {
        this.archiveId = archiveId;
    }

    public String getArchiveType() {
        return archiveType;
    }

    public void setArchiveType(String archiveType) {
        this.archiveType = archiveType;
    }

    public OffsetDateTime getWindowStartTime() {
        return windowStartTime;
    }

    public void setWindowStartTime(OffsetDateTime windowStartTime) {
        this.windowStartTime = windowStartTime;
    }

    public OffsetDateTime getWindowEndTime() {
        return windowEndTime;
    }

    public void setWindowEndTime(OffsetDateTime windowEndTime) {
        this.windowEndTime = windowEndTime;
    }

    public String getSourceIndexPattern() {
        return sourceIndexPattern;
    }

    public void setSourceIndexPattern(String sourceIndexPattern) {
        this.sourceIndexPattern = sourceIndexPattern;
    }

    public String getStorageProvider() {
        return storageProvider;
    }

    public void setStorageProvider(String storageProvider) {
        this.storageProvider = storageProvider;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getArchiveStatus() {
        return archiveStatus;
    }

    public void setArchiveStatus(String archiveStatus) {
        this.archiveStatus = archiveStatus;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public OffsetDateTime getStartedTime() {
        return startedTime;
    }

    public void setStartedTime(OffsetDateTime startedTime) {
        this.startedTime = startedTime;
    }

    public OffsetDateTime getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(OffsetDateTime completedTime) {
        this.completedTime = completedTime;
    }
}
