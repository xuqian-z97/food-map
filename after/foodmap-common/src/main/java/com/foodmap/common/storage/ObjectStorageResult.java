package com.foodmap.common.storage;

/**
 * 对象存储上传结果，返回业务服务需要保存的最小存储元数据。
 *
 * <p>公开 URL 可能为空，私有图片应通过签名 URL 或媒体服务受控接口访问。</p>
 */
public record ObjectStorageResult(
        String bucketName,
        String objectKey,
        String publicUrl,
        long contentLength
) {
}
