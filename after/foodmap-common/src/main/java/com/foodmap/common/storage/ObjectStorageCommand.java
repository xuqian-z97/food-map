package com.foodmap.common.storage;

import com.foodmap.common.validation.Check;

/**
 * 对象存储上传命令，集中描述上传目标、文件类型、大小和所属用户。
 *
 * <p>该模型用于在进入具体存储 SDK 前完成基础校验，避免各服务重复校验上传元数据。</p>
 */
public record ObjectStorageCommand(
        String bucketName,
        String objectKey,
        String contentType,
        long contentLength,
        Long ownerUserId
) {
    /**
     * 创建上传命令，并校验必填字段和正数约束；业务层应在调用前完成文件类型白名单校验。
     */
    public static ObjectStorageCommand upload(
            String bucketName,
            String objectKey,
            String contentType,
            long contentLength,
            Long ownerUserId
    ) {
        return new ObjectStorageCommand(
                Check.notBlank("bucketName", bucketName),
                Check.notBlank("objectKey", objectKey),
                Check.notBlank("contentType", contentType),
                Check.positive("contentLength", contentLength),
                Check.positive("ownerUserId", ownerUserId)
        );
    }

    /**
     * 创建系统级上传命令，适用于日志归档、系统导出文件等不归属于单个用户的对象。
     *
     * @param bucketName 存储桶名称。
     * @param objectKey 对象 Key。
     * @param contentType 内容类型。
     * @param contentLength 内容长度。
     * @return ownerUserId 为空的系统上传命令。
     */
    public static ObjectStorageCommand systemUpload(
            String bucketName,
            String objectKey,
            String contentType,
            long contentLength
    ) {
        return new ObjectStorageCommand(
                Check.notBlank("bucketName", bucketName),
                Check.notBlank("objectKey", objectKey),
                Check.notBlank("contentType", contentType),
                Check.positive("contentLength", contentLength),
                null
        );
    }
}
