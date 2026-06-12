package com.foodmap.log.infrastructure.messaging;

import com.foodmap.log.service.ApiAccessLogIngestionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 接口访问摘要 Kafka 消费器，消费 `foodmap.logs.api-access` 并写入独立日志 PostgreSQL。
 */
@Component
@ConditionalOnProperty(prefix = "foodmap.log.api-access.consumer", name = "enabled", havingValue = "true")
public class ApiAccessLogKafkaConsumer {
    private final ApiAccessLogIngestionService ingestionService;

    /**
     * 创建接口访问摘要 Kafka 消费器。
     *
     * @param ingestionService 接口访问摘要消费落库服务。
     */
    public ApiAccessLogKafkaConsumer(ApiAccessLogIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /**
     * 处理 Kafka 中的接口访问摘要事件。
     *
     * @param record Kafka 消费记录，value 为 JSON 反序列化后的字段映射。
     */
    @KafkaListener(topics = "${foodmap.log.api-access.topic:foodmap.logs.api-access}", groupId = "${foodmap.log.api-access.group-id:foodmap-log-service-api-access}")
    @SuppressWarnings("rawtypes")
    public void onMessage(ConsumerRecord record) {
        ingestionService.ingest(record.value(), record.topic(), record.partition(), record.offset());
    }
}
