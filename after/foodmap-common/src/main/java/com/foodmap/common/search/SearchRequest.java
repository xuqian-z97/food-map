package com.foodmap.common.search;

import java.util.Map;

/**
 * 通用搜索请求，承载索引名称和搜索引擎原生查询体。
 *
 * @param indexName 索引名称或索引模式。
 * @param body 搜索请求体，调用方负责表达领域查询语义。
 */
public record SearchRequest(
        String indexName,
        Map<String, Object> body
) {
}
