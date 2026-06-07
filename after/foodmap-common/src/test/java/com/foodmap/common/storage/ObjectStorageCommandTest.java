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
}
