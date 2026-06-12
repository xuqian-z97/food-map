package com.foodmap.log.service;

import com.foodmap.log.application.port.ApiAccessLogRepository;
import com.foodmap.log.infrastructure.messaging.ApiAccessLogEventParser;
import com.foodmap.log.infrastructure.persistence.entity.ApiAccessLogEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 接口访问摘要消费落库应用服务。
 *
 * <p>该服务只处理已脱敏的接口访问摘要，不保存完整请求体、Token、密码或私密正文。</p>
 */
@Service
public class ApiAccessLogIngestionService {
    private final ApiAccessLogEventParser parser;
    private final ApiAccessLogRepository repository;

    /**
     * 创建接口访问摘要消费落库服务。
     *
     * @param parser Kafka 日志事件解析器。
     * @param repository 接口访问摘要仓储端口。
     */
    public ApiAccessLogIngestionService(ApiAccessLogEventParser parser, ApiAccessLogRepository repository) {
        this.parser = parser;
        this.repository = repository;
    }

    /**
     * 解析 Kafka 消息并幂等写入 `api_access_log`。
     *
     * @param payload Kafka JSON 消息体或原始日志对象。
     * @param topic 来源 topic。
     * @param partition 来源分区。
     * @param offset 来源 offset。
     * @return 实际新增行数，重复消息返回 0。
     */
    @Transactional(rollbackFor = Exception.class)
    public int ingest(Object payload, String topic, int partition, long offset) {
        ApiAccessLogEntity entity = parser.parse(payload, topic, partition, offset);
        return repository.saveIgnoreDuplicate(entity);
    }
}
