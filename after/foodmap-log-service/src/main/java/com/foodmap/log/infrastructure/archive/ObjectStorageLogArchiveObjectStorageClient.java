package com.foodmap.log.infrastructure.archive;

import com.foodmap.common.storage.ObjectStorageClient;
import com.foodmap.common.storage.ObjectStorageCommand;
import com.foodmap.log.application.port.LogArchiveObjectStorageClient;
import com.foodmap.log.application.port.LogArchivePayload;
import com.foodmap.log.infrastructure.persistence.entity.LogArchiveRecordEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

/**
 * 日志归档对象存储适配器，将归档载荷交给 foodmap-common 的统一 ObjectStorageClient 上传。
 */
@Component
@ConditionalOnBean(ObjectStorageClient.class)
@ConditionalOnProperty(prefix = "foodmap.log.archive.storage", name = "enabled", havingValue = "true")
public class ObjectStorageLogArchiveObjectStorageClient implements LogArchiveObjectStorageClient {
    private final ObjectStorageClient objectStorageClient;

    /**
     * 创建日志归档对象存储适配器。
     *
     * @param objectStorageClient common 层统一对象存储客户端。
     */
    public ObjectStorageLogArchiveObjectStorageClient(ObjectStorageClient objectStorageClient) {
        this.objectStorageClient = objectStorageClient;
    }

    /**
     * 上传日志归档载荷到对象存储。
     *
     * @param record 日志归档记录，包含 bucket 和 objectKey。
     * @param payload 日志归档载荷。
     */
    @Override
    public void upload(LogArchiveRecordEntity record, LogArchivePayload payload) {
        ObjectStorageCommand command = ObjectStorageCommand.systemUpload(
                record.getBucketName(),
                record.getObjectKey(),
                payload.contentType(),
                payload.content().length
        );
        objectStorageClient.putObject(command, new ByteArrayInputStream(payload.content()));
    }
}
