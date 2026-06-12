package com.foodmap.common.storage.minio;

import com.foodmap.common.storage.ObjectStorageCommand;
import com.foodmap.common.storage.ObjectStorageResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class MinioObjectStorageClientTest {

    @Test
    void shouldUploadObjectThroughMinioGateway() {
        CapturingMinioGateway gateway = new CapturingMinioGateway();
        MinioObjectStorageProperties properties = new MinioObjectStorageProperties();
        properties.setPublicUrlBase("http://127.0.0.1:9000");
        MinioObjectStorageClient client = new MinioObjectStorageClient(gateway, properties);
        ObjectStorageCommand command = ObjectStorageCommand.systemUpload(
                "foodmap-log-archive",
                "logs/full/year=2026/month=06/day=05/foodmap-logs-2026-06-05.jsonl.gz",
                "application/x-ndjson+gzip",
                15L
        );

        ObjectStorageResult result = client.putObject(command, InputStream.nullInputStream());

        assertThat(gateway.bucketName).isEqualTo(command.bucketName());
        assertThat(gateway.objectKey).isEqualTo(command.objectKey());
        assertThat(gateway.contentType).isEqualTo(command.contentType());
        assertThat(gateway.contentLength).isEqualTo(command.contentLength());
        assertThat(result.bucketName()).isEqualTo(command.bucketName());
        assertThat(result.objectKey()).isEqualTo(command.objectKey());
        assertThat(result.publicUrl()).isEqualTo("http://127.0.0.1:9000/foodmap-log-archive/logs/full/year=2026/month=06/day=05/foodmap-logs-2026-06-05.jsonl.gz");
        assertThat(result.contentLength()).isEqualTo(command.contentLength());
    }

    private static class CapturingMinioGateway implements MinioObjectStorageGateway {
        private String bucketName;
        private String objectKey;
        private String contentType;
        private long contentLength;

        @Override
        public void putObject(
                String bucketName,
                String objectKey,
                String contentType,
                long contentLength,
                InputStream inputStream
        ) throws IOException {
            this.bucketName = bucketName;
            this.objectKey = objectKey;
            this.contentType = contentType;
            this.contentLength = contentLength;
            inputStream.readAllBytes();
        }
    }
}
