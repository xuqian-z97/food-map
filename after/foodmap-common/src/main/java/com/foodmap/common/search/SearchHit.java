package com.foodmap.common.search;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * 通用搜索命中文档，保留源文档和搜索引擎返回的排序游标。
 *
 * @param source 命中的源文档。
 * @param sortValues 当前命中的排序游标，可用于 search_after 分页。
 */
public record SearchHit(
        JsonNode source,
        List<Object> sortValues
) {
}
