package com.foodmap.log.infrastructure.archive;

import com.foodmap.common.storage.ObjectStorageClient;
import com.foodmap.common.storage.ObjectStorageCommand;
import com.foodmap.common.storage.ObjectStorageResult;
import com.foodmap.log.application.port.LogArchivePayload;
import com.foodmap.log.infrastructure.persistence.entity.LogArchiveRecordEntity;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ObjectStorageLogArchiveObjectStorageClientTest {

    @Test
    void shouldUploadArchivePayloadThroughCommonObjectStorageClient() {
        CapturingObjectStorageClient storageClient = new CapturingObjectStorageClient();
        ObjectStorageLogArchiveObjectStorageClient client = new ObjectStorageLogArchiveObjectStorageClient(storageClient);
        byte[] content = "archive-content".getBytes();

        client.upload(archiveRecord(), new LogArchivePayload(content, "application/x-ndjson+gzip"));

        assertEquals("foodmap-log-archive", storageClient.command.bucketName());
        assertEquals("logs/full/year=2026/month=06/day=05/foodmap-logs-2026-06-05.jsonl.gz", storageClient.command.objectKey());
        assertEquals("application/x-ndjson+gzip", storageClient.command.contentType());
        assertEquals(content.length, storageClient.command.contentLength());
        assertEquals(null, storageClient.command.ownerUserId());
        assertArrayEquals(content, storageClient.content);
    }

    private static LogArchiveRecordEntity archiveRecord() {
        LogArchiveRecordEntity entity = new LogArchiveRecordEntity();
        entity.setArchiveId(1001L);
        entity.setWindowStartTime(OffsetDateTime.parse("2026-06-05T00:00:00Z"));
        entity.setWindowEndTime(OffsetDateTime.parse("2026-06-06T00:00:00Z"));
        entity.setBucketName("foodmap-log-archive");
        entity.setObjectKey("logs/full/year=2026/month=06/day=05/foodmap-logs-2026-06-05.jsonl.gz");
        return entity;
    }

    private static class CapturingObjectStorageClient implements ObjectStorageClient {
        private ObjectStorageCommand command;
        private byte[] content;

        @Override
        public ObjectStorageResult putObject(ObjectStorageCommand command, InputStream inputStream) {
            try {
                this.command = command;
                this.content = inputStream.readAllBytes();
                return new ObjectStorageResult(command.bucketName(), command.objectKey(), null, command.contentLength());
            } catch (IOException ex) {
                throw new IllegalStateException("failed to read test input", ex);
            }
        }
    }
}
