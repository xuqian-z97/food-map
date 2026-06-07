package com.foodmap.common.mq;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEventEnvelopeTest {

    @Test
    void shouldCreateEnvelopeWithRequiredEventMetadata() {
        DomainEventEnvelope<Map<String, Long>> envelope = DomainEventEnvelope.of(
                "RecommendationCreatedEvent",
                "foodmap-recommendation-service",
                Map.of("recommendationId", 90001L)
        );

        assertThat(envelope.eventId()).isNotBlank();
        assertThat(envelope.eventType()).isEqualTo("RecommendationCreatedEvent");
        assertThat(envelope.eventVersion()).isEqualTo("v1");
        assertThat(envelope.sourceService()).isEqualTo("foodmap-recommendation-service");
        assertThat(envelope.occurredAt()).isNotNull();
        assertThat(envelope.payload()).containsEntry("recommendationId", 90001L);
    }
}
