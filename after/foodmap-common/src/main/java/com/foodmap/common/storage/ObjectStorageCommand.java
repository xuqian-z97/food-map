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
}
