package com.foodmap.log.service;

import com.foodmap.log.application.port.ApiAccessLogRepository;
import com.foodmap.log.application.port.ApiAccessLogQueryCriteria;
import com.foodmap.log.infrastructure.persistence.entity.ApiAccessLogEntity;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiAccessLogRetentionServiceTest {

    @Test
    void shouldDeleteLogsOlderThanRetentionDays() {
        CapturingRepository repository = new CapturingRepository();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-06-12T03:20:00Z"), ZoneOffset.UTC);
        ApiAccessLogRetentionService service = new ApiAccessLogRetentionService(repository, fixedClock, 15);

        int deleted = service.deleteExpiredApiAccessLogs();

        assertEquals(7, deleted);
        assertEquals(OffsetDateTime.parse("2026-05-28T03:20:00Z"), repository.cutoffTime);
    }

    private static class CapturingRepository implements ApiAccessLogRepository {
        private OffsetDateTime cutoffTime;

        @Override
        public int saveIgnoreDuplicate(ApiAccessLogEntity entity) {
            return 1;
        }

        @Override
        public int deleteOccurredBefore(OffsetDateTime cutoffTime) {
            this.cutoffTime = cutoffTime;
            return 7;
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
