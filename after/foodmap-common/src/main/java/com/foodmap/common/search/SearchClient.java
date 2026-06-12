package com.foodmap.common.search;

/**
 * 通用搜索客户端端口，统一隔离业务代码和具体搜索引擎 HTTP/SDK 访问细节。
 */
public interface SearchClient {

    /**
     * 执行一次搜索查询。
     *
     * @param request 搜索请求，包含索引名称和搜索引擎原生查询体。
     * @return 搜索响应，包含命中源文档和分页游标。
     */
    SearchResponse search(SearchRequest request);
}
