package com.foodmap.log.service;

import com.foodmap.log.application.port.LogArchiveExportClient;
import com.foodmap.log.application.port.LogArchiveObjectStorageClient;
import com.foodmap.log.application.port.LogArchiveRecordRepository;
import com.foodmap.log.application.port.LogArchivePayload;
import com.foodmap.log.infrastructure.persistence.entity.LogArchiveRecordEntity;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogArchiveExecutionServiceTest {

    @Test
    void shouldExportUploadAndMarkArchiveSuccess() {
        CapturingRepository repository = new CapturingRepository(pendingRecord());
        CapturingExporter exporter = new CapturingExporter();
        CapturingStorage storage = new CapturingStorage(false);
        Clock fixedClock = Clock.fixed(Instant.parse("2026-06-12T03:40:00Z"), ZoneOffset.UTC);
        LogArchiveExecutionService service = new LogArchiveExecutionService(repository, exporter, storage, fixedClock);

        int executed = service.executeNextPendingArchive();

        assertEquals(1, executed);
        assertEquals(1001L, exporter.record.getArchiveId());
        assertEquals("foodmap-log-archive", storage.record.getBucketName());
        assertEquals("application/x-ndjson+gzip", storage.payload.contentType());
        assertEquals("RUNNING", repository.lastRunningStatus);
        assertEquals("SUCCESS", repository.lastSuccessStatus);
        assertEquals(OffsetDateTime.parse("2026-06-12T03:40:00Z"), repository.startedTime);
        assertEquals(OffsetDateTime.parse("2026-06-12T03:40:00Z"), repository.completedTime);
    }

    @Test
    void shouldMarkArchiveFailedWhenUploadFails() {
        CapturingRepository repository = new CapturingRepository(pendingRecord());
        CapturingExporter exporter = new CapturingExporter();
        CapturingStorage storage = new CapturingStorage(true);
        Clock fixedClock = Clock.fixed(Instant.parse("2026-06-12T03:45:00Z"), ZoneOffset.UTC);
        LogArchiveExecutionService service = new LogArchiveExecutionService(repository, exporter, storage, fixedClock);

        int executed = service.executeNextPendingArchive();

        assertEquals(0, executed);
        assertEquals("RUNNING", repository.lastRunningStatus);
        assertEquals("FAILED", repository.lastFailedStatus);
        assertEquals(1, repository.retryCount);
        assertTrue(repository.failureReason.contains("upload failed"));
    }

    private static LogArchiveRecordEntity pendingRecord() {
        LogArchiveRecordEntity entity = new LogArchiveRecordEntity();
        entity.setArchiveId(1001L);
        entity.setArchiveType("FULL_LOG_DAILY");
        entity.setWindowStartTime(OffsetDateTime.parse("2026-06-05T00:00:00Z"));
        entity.setWindowEndTime(OffsetDateTime.parse("2026-06-06T00:00:00Z"));
        entity.setSourceIndexPattern("foodmap-logs-*");
        entity.setStorageProvider("OSS");
        entity.setBucketName("foodmap-log-archive");
        entity.setObjectKey("logs/full/year=2026/month=06/day=05/foodmap-logs-2026-06-05.jsonl.gz");
        entity.setArchiveStatus("PENDING");
        entity.setRetryCount(0);
        return entity;
    }

    private static class CapturingRepository implements LogArchiveRecordRepository {
        private final LogArchiveRecordEntity pending;
        private String lastRunningStatus;
        private String lastSuccessStatus;
        private String lastFailedStatus;
        private OffsetDateTime startedTime;
        private OffsetDateTime completedTime;
        private String failureReason;
        private int retryCount;

        private CapturingRepository(LogArchiveRecordEntity pending) {
            this.pending = pending;
        }

        @Override
        public int saveIgnoreDuplicate(LogArchiveRecordEntity entity) {
            return 1;
        }

        @Override
        public Optional<LogArchiveRecordEntity> findNextPending() {
            return Optional.ofNullable(pending);
        }

        @Override
        public int markRunning(Long archiveId, OffsetDateTime startedTime) {
            this.lastRunningStatus = "RUNNING";
            this.startedTime = startedTime;
            return 1;
        }

        @Override
        public int markSuccess(Long archiveId, OffsetDateTime completedTime) {
            this.lastSuccessStatus = "SUCCESS";
            this.completedTime = completedTime;
            return 1;
        }

        @Override
        public int markFailed(Long archiveId, String failureReason, OffsetDateTime completedTime) {
            this.lastFailedStatus = "FAILED";
            this.failureReason = failureReason;
            this.completedTime = completedTime;
            this.retryCount = pending.getRetryCount() + 1;
            return 1;
        }
    }

    private static class CapturingExporter implements LogArchiveExportClient {
        private LogArchiveRecordEntity record;

        @Override
        public LogArchivePayload export(LogArchiveRecordEntity record) {
            this.record = record;
            return new LogArchivePayload("{}".getBytes(), "application/x-ndjson+gzip");
        }
    }

    private static class CapturingStorage implements LogArchiveObjectStorageClient {
        private final boolean failUpload;
        private LogArchiveRecordEntity record;
        private LogArchivePayload payload;

        private CapturingStorage(boolean failUpload) {
            this.failUpload = failUpload;
        }

        @Override
        public void upload(LogArchiveRecordEntity record, LogArchivePayload payload) {
            if (failUpload) {
                throw new IllegalStateException("upload failed");
            }
            this.record = record;
            this.payload = payload;
        }
    }
}
