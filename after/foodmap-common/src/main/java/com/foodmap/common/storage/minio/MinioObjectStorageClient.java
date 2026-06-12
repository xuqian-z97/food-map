package com.foodmap.common.storage.minio;

import com.foodmap.common.storage.ObjectStorageClient;
import com.foodmap.common.storage.ObjectStorageCommand;
import com.foodmap.common.storage.ObjectStorageException;
import com.foodmap.common.storage.ObjectStorageResult;

import java.io.InputStream;

/**
 * MinIO 对象存储客户端实现，负责把通用上传命令转换为 MinIO 网关调用。
 */
public class MinioObjectStorageClient implements ObjectStorageClient {
    private final MinioObjectStorageGateway gateway;
    private final MinioObjectStorageProperties properties;

    /**
     * 创建 MinIO 对象存储客户端。
     *
     * @param gateway MinIO SDK 调用网关。
     * @param properties MinIO 对象存储配置。
     */
    public MinioObjectStorageClient(
            MinioObjectStorageGateway gateway,
            MinioObjectStorageProperties properties
    ) {
        this.gateway = gateway;
        this.properties = properties;
    }

    /**
     * 上传对象到 MinIO，并返回通用对象存储结果。
     *
     * @param command 上传命令。
     * @param inputStream 上传内容流。
     * @return 对象存储上传结果。
     */
    @Override
    public ObjectStorageResult putObject(ObjectStorageCommand command, InputStream inputStream) {
        try {
            gateway.putObject(
                    command.bucketName(),
                    command.objectKey(),
                    command.contentType(),
                    command.contentLength(),
                    inputStream
            );
            return new ObjectStorageResult(
                    command.bucketName(),
                    command.objectKey(),
                    publicUrl(command),
                    command.contentLength()
            );
        } catch (Exception ex) {
            throw new ObjectStorageException("MinIO object upload failed", ex);
        }
    }

    /**
     * 根据公开访问基础地址拼接对象 URL；未配置公开地址时返回空。
     *
     * @param command 上传命令。
     * @return 对象公开访问 URL，未配置时为空。
     */
    private String publicUrl(ObjectStorageCommand command) {
        String publicUrlBase = properties.getPublicUrlBase();
        if (publicUrlBase == null || publicUrlBase.isBlank()) {
            return null;
        }
        return trimTrailingSlash(publicUrlBase) + "/" + command.bucketName() + "/" + trimLeadingSlash(command.objectKey());
    }

    /**
     * 去除结尾斜杠，避免 URL 拼接出现重复分隔符。
     *
     * @param value 原始地址。
     * @return 不带结尾斜杠的地址。
     */
    private String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * 去除开头斜杠，避免 objectKey 被拼成绝对路径。
     *
     * @param value 原始对象 Key。
     * @return 不带开头斜杠的对象 Key。
     */
    private String trimLeadingSlash(String value) {
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }
}
