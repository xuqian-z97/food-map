package com.foodmap.common.storage;

/**
 * 对象存储基础设施异常，用于统一表达 MinIO、OSS 等对象存储调用失败。
 */
public class ObjectStorageException extends RuntimeException {

    /**
     * 创建对象存储基础设施异常。
     *
     * @param message 异常摘要，不能包含密钥、Token 或完整敏感路径。
     */
    public ObjectStorageException(String message) {
        super(message);
    }

    /**
     * 创建带根因的对象存储基础设施异常。
     *
     * @param message 异常摘要，不能包含密钥、Token 或完整敏感路径。
     * @param cause 对象存储底层异常。
     */
    public ObjectStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
