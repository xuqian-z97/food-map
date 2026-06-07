package com.foodmap.common.storage;

import java.io.InputStream;

/**
 * 对象存储统一客户端接口，业务服务通过该接口访问 MinIO 或阿里云 OSS。
 *
 * <p>业务代码不应直接依赖具体 SDK，后续替换存储实现时只需要调整适配器。</p>
 */
public interface ObjectStorageClient {

    /**
     * 上传对象并返回存储结果；实现类必须在日志中脱敏 objectKey 和用户相关路径。
     */
    ObjectStorageResult putObject(ObjectStorageCommand command, InputStream inputStream);
}
