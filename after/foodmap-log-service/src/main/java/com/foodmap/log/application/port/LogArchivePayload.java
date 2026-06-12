package com.foodmap.log.application.port;

/**
 * 日志归档载荷，表示已从热日志存储导出的压缩内容和内容类型。
 *
 * @param content 归档内容字节，当前基础切片使用小载荷抽象，后续生产适配器可替换为流式上传。
 * @param contentType 归档内容类型。
 */
public record LogArchivePayload(
        byte[] content,
        String contentType
) {
}
