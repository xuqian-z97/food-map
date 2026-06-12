package com.foodmap.log.service;

import com.foodmap.log.application.port.ApiAccessLogRepository;
import com.foodmap.log.application.port.ApiAccessLogQueryCriteria;
import com.foodmap.log.infrastructure.messaging.ApiAccessLogEventParser;
import com.foodmap.log.infrastructure.persistence.entity.ApiAccessLogEntity;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiAccessLogIngestionServiceTest {

    @Test
    void shouldSaveParsedApiAccessLog() {
        CapturingRepository repository = new CapturingRepository();
        ApiAccessLogIngestionService service = new ApiAccessLogIngestionService(new ApiAccessLogEventParser(), repository);

        int inserted = service.ingest(Map.of(
                "requestId", "req-ingest",
                "traceId", "trace-ingest",
                "serviceName", "foodmap-gateway-service",
                "method", "GET",
                "path", "/internal/auth/health",
                "status", 200,
                "durationMs", 8
        ), "foodmap.logs.api-access", 1, 99L);

        assertEquals(1, inserted);
        assertEquals("req-ingest", repository.saved.getRequestId());
        assertEquals("foodmap.logs.api-access", repository.saved.getSourceTopic());
        assertEquals(1, repository.saved.getSourcePartition());
        assertEquals(99L, repository.saved.getSourceOffset());
    }

    private static class CapturingRepository implements ApiAccessLogRepository {
        private ApiAccessLogEntity saved;

        @Override
        public int saveIgnoreDuplicate(ApiAccessLogEntity entity) {
            this.saved = entity;
            return 1;
        }

        @Override
        public int deleteOccurredBefore(OffsetDateTime cutoffTime) {
            return 0;
        }

        @Override
        public List<ApiAccessLogEntity> search(ApiAccessLogQueryCriteria criteria) {
            return List.of();
        }

        @Override
        public long count(ApiAccessLogQueryCriteria criteria) {
            return 0L;
        }
    }
}
