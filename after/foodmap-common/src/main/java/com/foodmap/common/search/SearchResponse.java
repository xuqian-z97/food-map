package com.foodmap.common.search;

import java.util.List;

/**
 * 通用搜索响应，当前基础版本只暴露命中文档列表。
 *
 * @param hits 搜索命中文档列表。
 */
public record SearchResponse(
        List<SearchHit> hits
) {
}
