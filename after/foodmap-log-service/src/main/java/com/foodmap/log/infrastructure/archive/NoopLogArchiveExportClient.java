package com.foodmap.log.infrastructure.archive;

import com.foodmap.log.application.port.LogArchiveExportClient;
import com.foodmap.log.application.port.LogArchivePayload;
import com.foodmap.log.infrastructure.persistence.entity.LogArchiveRecordEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 占位日志导出适配器，真实 Elasticsearch 导出适配器接入前用于保持服务可启动。
 */
@Component
@ConditionalOnProperty(prefix = "foodmap.log.archive.elasticsearch", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopLogArchiveExportClient implements LogArchiveExportClient {

    /**
     * 返回空 JSON Lines gzip 占位载荷。
     *
     * @param record 日志归档记录，包含窗口、来源索引和目标对象信息。
     * @return 占位归档载荷。
     */
    @Override
    public LogArchivePayload export(LogArchiveRecordEntity record) {
        return new LogArchivePayload(new byte[0], "application/x-ndjson+gzip");
    }
}
