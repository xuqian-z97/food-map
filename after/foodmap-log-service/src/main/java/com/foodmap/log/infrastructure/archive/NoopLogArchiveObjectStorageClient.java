package com.foodmap.log.infrastructure.archive;

import com.foodmap.log.application.port.LogArchiveObjectStorageClient;
import com.foodmap.log.application.port.LogArchivePayload;
import com.foodmap.log.infrastructure.persistence.entity.LogArchiveRecordEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 占位日志归档对象存储适配器，真实 OSS 适配器接入前用于保持服务可启动。
 */
@Component
@ConditionalOnMissingBean(LogArchiveObjectStorageClient.class)
public class NoopLogArchiveObjectStorageClient implements LogArchiveObjectStorageClient {

    /**
     * 当前占位实现不执行网络上传，后续由 OSS 适配器替换。
     *
     * @param record 日志归档记录，包含 bucket 和 objectKey。
     * @param payload 日志归档载荷。
     */
    @Override
    public void upload(LogArchiveRecordEntity record, LogArchivePayload payload) {
        // No-op by design until the real OSS adapter lands.
    }
}
