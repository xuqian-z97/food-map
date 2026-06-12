package com.foodmap.common.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObjectStorageCommandTest {

    @Test
    void shouldCreateUploadCommandWithRequiredMetadata() {
        ObjectStorageCommand command = ObjectStorageCommand.upload(
                "foodmap-dev",
                "recommendation/10001/image.jpg",
                "image/jpeg",
                1024L,
                10001L
        );

        assertThat(command.bucketName()).isEqualTo("foodmap-dev");
        assertThat(command.objectKey()).isEqualTo("recommendation/10001/image.jpg");
        assertThat(command.contentType()).isEqualTo("image/jpeg");
        assertThat(command.contentLength()).isEqualTo(1024L);
        assertThat(command.ownerUserId()).isEqualTo(10001L);
    }

    @Test
    void shouldRejectInvalidUploadCommand() {
        assertThatThrownBy(() -> ObjectStorageCommand.upload(
                "foodmap-dev",
                "",
                "image/jpeg",
                1024L,
                10001L
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("objectKey");
    }

    @Test
    void shouldCreateSystemUploadCommandWithoutOwnerUser() {
        ObjectStorageCommand command = ObjectStorageCommand.systemUpload(
                "foodmap-log-archive",
                "logs/full/year=2026/month=06/day=05/foodmap-logs-2026-06-05.jsonl.gz",
                "application/x-ndjson+gzip",
                2048L
        );

        assertThat(command.bucketName()).isEqualTo("foodmap-log-archive");
        assertThat(command.objectKey()).contains("foodmap-logs-2026-06-05.jsonl.gz");
        assertThat(command.contentType()).isEqualTo("application/x-ndjson+gzip");
        assertThat(command.contentLength()).isEqualTo(2048L);
        assertThat(command.ownerUserId()).isNull();
    }
}
