package com.foodmap.common.mq;

import java.time.Instant;
import java.util.UUID;

public record DomainEventEnvelope<T>(
        String eventId,
        String eventType,
        String eventVersion,
        String sourceService,
        Instant occurredAt,
        T payload
) {
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
