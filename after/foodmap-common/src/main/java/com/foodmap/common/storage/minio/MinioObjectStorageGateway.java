package com.foodmap.common.storage.minio;

import java.io.InputStream;

/**
 * MinIO SDK 调用网关，隔离具体 SDK API，便于 ObjectStorageClient 做单元测试和后续替换实现。
 */
public interface MinioObjectStorageGateway {

    /**
     * 上传对象到指定 MinIO bucket。
     *
     * @param bucketName bucket 名称。
     * @param objectKey 对象 Key。
     * @param contentType 内容类型。
     * @param contentLength 内容长度。
     * @param inputStream 对象内容流，由调用方负责提供。
     * @throws Exception MinIO SDK 或网络调用失败。
     */
    void putObject(
            String bucketName,
            String objectKey,
            String contentType,
            long contentLength,
            InputStream inputStream
    ) throws Exception;
}
