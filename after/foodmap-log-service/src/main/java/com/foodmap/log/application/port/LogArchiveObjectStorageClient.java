package com.foodmap.log.application.port;

import com.foodmap.log.infrastructure.persistence.entity.LogArchiveRecordEntity;

/**
 * 日志归档对象存储端口，隔离归档执行服务与 OSS、MinIO 等具体 SDK。
 */
public interface LogArchiveObjectStorageClient {

    /**
     * 上传日志归档载荷到对象存储。
     *
     * @param record 日志归档记录，包含 bucket 和 objectKey。
     * @param payload 日志归档载荷。
     */
    void upload(LogArchiveRecordEntity record, LogArchivePayload payload);
}
