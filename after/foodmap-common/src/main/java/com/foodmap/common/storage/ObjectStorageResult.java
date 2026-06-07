package com.foodmap.common.storage;

public record ObjectStorageResult(
        String bucketName,
        String objectKey,
        String publicUrl,
        long contentLength
) {
}
