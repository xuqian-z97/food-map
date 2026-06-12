package com.foodmap.common.search;

/**
 * 搜索基础设施异常，用于统一表达 Elasticsearch/OpenSearch 等搜索引擎调用失败。
 */
public class SearchException extends RuntimeException {

    /**
     * 创建搜索基础设施异常。
     *
     * @param message 异常摘要，不能包含密码、Token 或完整敏感信息。
     */
    public SearchException(String message) {
        super(message);
    }

    /**
     * 创建带根因的搜索基础设施异常。
     *
     * @param message 异常摘要，不能包含密码、Token 或完整敏感信息。
     * @param cause 搜索调用底层异常。
     */
    public SearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
