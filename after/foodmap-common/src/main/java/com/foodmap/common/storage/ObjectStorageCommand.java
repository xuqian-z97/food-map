package com.foodmap.common.storage;

public record ObjectStorageCommand(
        String bucketName,
        String objectKey,
        String contentType,
        long contentLength,
        Long ownerUserId
) {
    public static ObjectStorageCommand upload(
            String bucketName,
            String objectKey,
            String contentType,
            long contentLength,
            Long ownerUserId
    ) {
        return new ObjectStorageCommand(
                requireText("bucketName", bucketName),
                requireText("objectKey", objectKey),
                requireText("contentType", contentType),
                requirePositive("contentLength", contentLength),
                requirePositive("ownerUserId", ownerUserId)
        );
    }

    private static String requireText(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static long requirePositive(String fieldName, long value) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static Long requirePositive(String fieldName, Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
}
