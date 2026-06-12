package com.foodmap.log.application.port;

import com.foodmap.log.infrastructure.persistence.entity.LogArchiveRecordEntity;

/**
 * 全量日志导出端口，隔离归档执行服务与 Elasticsearch 查询和压缩实现。
 */
public interface LogArchiveExportClient {

    /**
     * 导出指定归档窗口的全量日志。
     *
     * @param record 日志归档记录，包含窗口、来源索引和目标对象信息。
     * @return 可上传到对象存储的归档载荷。
     */
    LogArchivePayload export(LogArchiveRecordEntity record);
}
