package com.foodmap.log.service;

import com.foodmap.log.application.port.LogArchiveRecordRepository;
import com.foodmap.log.infrastructure.persistence.entity.LogArchiveRecordEntity;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogArchivePlanningServiceTest {

    @Test
    void shouldCreateDailyArchivePlanForExpiredHotLogWindow() {
        CapturingRepository repository = new CapturingRepository();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-06-12T03:20:00Z"), ZoneOffset.UTC);
        LogArchivePlanningService service = new LogArchivePlanningService(
                repository,
                fixedClock,
                7,
                "foodmap-logs-*",
                "OSS",
                "foodmap-log-archive",
                "logs/full"
        );

        int inserted = service.createDailyArchivePlan();

        assertEquals(1, inserted);
        assertEquals("FULL_LOG_DAILY", repository.saved.getArchiveType());
        assertEquals(OffsetDateTime.parse("2026-06-05T00:00:00Z"), repository.saved.getWindowStartTime());
        assertEquals(OffsetDateTime.parse("2026-06-06T00:00:00Z"), repository.saved.getWindowEndTime());
        assertEquals("foodmap-logs-*", repository.saved.getSourceIndexPattern());
        assertEquals("OSS", repository.saved.getStorageProvider());
        assertEquals("foodmap-log-archive", repository.saved.getBucketName());
        assertEquals("logs/full/year=2026/month=06/day=05/foodmap-logs-2026-06-05.jsonl.gz", repository.saved.getObjectKey());
        assertEquals("PENDING", repository.saved.getArchiveStatus());
        assertEquals(0, repository.saved.getRetryCount());
    }

    private static class CapturingRepository implements LogArchiveRecordRepository {
        private LogArchiveRecordEntity saved;

        @Override
        public int saveIgnoreDuplicate(LogArchiveRecordEntity entity) {
            this.saved = entity;
            return 1;
        }

        @Override
        public Optional<LogArchiveRecordEntity> findNextPending() {
            return Optional.empty();
        }

        @Override
        public int markRunning(Long archiveId, OffsetDateTime startedTime) {
            return 0;
        }

        @Override
        public int markSuccess(Long archiveId, OffsetDateTime completedTime) {
            return 0;
        }

        @Override
        public int markFailed(Long archiveId, String failureReason, OffsetDateTime completedTime) {
            return 0;
        }
    }
}
