package com.foodmap.common.storage.minio;

import com.foodmap.common.storage.ObjectStorageClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.net.http.HttpClient;

/**
 * FoodMap MinIO 对象存储自动配置，按需注册通用 ObjectStorageClient。
 */
@AutoConfiguration
@EnableConfigurationProperties(MinioObjectStorageProperties.class)
@ConditionalOnProperty(prefix = "foodmap.storage.minio", name = "enabled", havingValue = "true")
public class FoodMapMinioStorageAutoConfiguration {

    /**
     * 创建 MinIO HTTP 客户端。
     *
     * @param properties MinIO 对象存储配置。
     * @return Java HTTP 客户端。
     */
    @Bean
    @ConditionalOnMissingBean
    public HttpClient minioHttpClient(MinioObjectStorageProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
    }

    /**
     * 创建 MinIO HTTP 网关。
     *
     * @param minioHttpClient Java HTTP 客户端。
     * @param properties MinIO 对象存储配置。
     * @return MinIO 网关。
     */
    @Bean
    @ConditionalOnMissingBean(MinioObjectStorageGateway.class)
    public MinioObjectStorageGateway minioObjectStorageGateway(
            HttpClient minioHttpClient,
            MinioObjectStorageProperties properties
    ) {
        return new MinioHttpObjectStorageGateway(minioHttpClient, properties);
    }

    /**
     * 创建通用对象存储客户端。
     *
     * @param gateway MinIO SDK 调用网关。
     * @param properties MinIO 对象存储配置。
     * @return 通用对象存储客户端。
     */
    @Bean
    @ConditionalOnMissingBean(ObjectStorageClient.class)
    public ObjectStorageClient minioObjectStorageClient(
            MinioObjectStorageGateway gateway,
            MinioObjectStorageProperties properties
    ) {
        return new MinioObjectStorageClient(gateway, properties);
    }
}
