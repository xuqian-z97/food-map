package com.foodmap.common.mq;

import java.time.Instant;
import java.util.UUID;

/**
 * 领域事件信封，统一承载事件元数据和业务载荷。
 *
 * <p>事件消费者做幂等、追踪和问题排查时，应优先使用 {@code eventId}、{@code eventType}
 * 和 {@code sourceService} 定位消息来源。</p>
 */
public record DomainEventEnvelope<T>(
        String eventId,
        String eventType,
        String eventVersion,
        String sourceService,
        Instant occurredAt,
        T payload
) {
    /**
     * 创建默认 v1 事件信封，适合当前 MVP 阶段的领域事件发布。
     */
    public static <T> DomainEventEnvelope<T> of(String eventType, String sourceService, T payload) {
        return new DomainEventEnvelope<>(
                UUID.randomUUID().toString(),
                requireText("eventType", eventType),
                "v1",
                requireText("sourceService", sourceService),
                Instant.now(),
                payload
        );
    }

    private static String requireText(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
