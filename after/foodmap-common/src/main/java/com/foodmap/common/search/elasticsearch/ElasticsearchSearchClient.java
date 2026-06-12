package com.foodmap.common.search.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodmap.common.search.SearchClient;
import com.foodmap.common.search.SearchException;
import com.foodmap.common.search.SearchHit;
import com.foodmap.common.search.SearchRequest;
import com.foodmap.common.search.SearchResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Elasticsearch 搜索客户端实现，统一处理 HTTP 调用、认证头、超时和 hits 解析。
 */
public class ElasticsearchSearchClient implements SearchClient {
    private final ElasticsearchSearchProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * 创建 Elasticsearch 搜索客户端。
     *
     * @param properties Elasticsearch 搜索配置。
     * @param objectMapper JSON 序列化和解析组件。
     */
    public ElasticsearchSearchClient(
            ElasticsearchSearchProperties properties,
            ObjectMapper objectMapper
    ) {
        this(
                properties,
                objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(properties.getConnectTimeout())
                        .build()
        );
    }

    /**
     * 创建可注入 HTTP 客户端的 Elasticsearch 搜索客户端，主要用于单元测试。
     *
     * @param properties Elasticsearch 搜索配置。
     * @param objectMapper JSON 序列化和解析组件。
     * @param httpClient HTTP 客户端。
     */
    ElasticsearchSearchClient(
            ElasticsearchSearchProperties properties,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    /**
     * 执行 Elasticsearch `_search` 查询并解析命中文档。
     *
     * @param request 搜索请求，包含索引名称和搜索引擎原生查询体。
     * @return 搜索响应。
     */
    @Override
    public SearchResponse search(SearchRequest request) {
        try {
            HttpRequest httpRequest = buildRequest(request);
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new SearchException("Elasticsearch search failed, status=" + response.statusCode());
            }
            return parseResponse(response.body());
        } catch (IOException ex) {
            throw new SearchException("Elasticsearch search IO failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SearchException("Elasticsearch search interrupted", ex);
        }
    }

    /**
     * 构建 Elasticsearch HTTP 搜索请求。
     *
     * @param request 搜索请求。
     * @return HTTP 请求。
     */
    private HttpRequest buildRequest(SearchRequest request) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(normalizedBaseUrl() + "/" + request.indexName() + "/_search"))
                .timeout(properties.getRequestTimeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request.body()), StandardCharsets.UTF_8));
        authorizationHeader().ifPresent(header -> builder.header("Authorization", header));
        return builder.build();
    }

    /**
     * 解析 Elasticsearch `_search` 响应为通用 SearchResponse。
     *
     * @param responseBody Elasticsearch 响应体。
     * @return 通用搜索响应。
     */
    private SearchResponse parseResponse(String responseBody) throws IOException {
        JsonNode hits = objectMapper.readTree(responseBody).path("hits").path("hits");
        List<SearchHit> searchHits = new ArrayList<>();
        if (hits.isArray()) {
            for (JsonNode hit : hits) {
                JsonNode source = hit.path("_source");
                if (!source.isMissingNode()) {
                    searchHits.add(new SearchHit(source, sortValues(hit.path("sort"))));
                }
            }
        }
        return new SearchResponse(searchHits);
    }

    /**
     * 转换 Elasticsearch hit.sort 为通用分页游标值。
     *
     * @param sort Elasticsearch 返回的 sort 数组。
     * @return 排序值列表。
     */
    private List<Object> sortValues(JsonNode sort) {
        List<Object> values = new ArrayList<>();
        if (!sort.isArray()) {
            return values;
        }
        for (JsonNode item : sort) {
            if (item.isTextual()) {
                values.add(item.asText());
            } else if (item.isIntegralNumber()) {
                values.add(item.asLong());
            } else if (item.isFloatingPointNumber()) {
                values.add(item.asDouble());
            } else if (item.isBoolean()) {
                values.add(item.asBoolean());
            } else {
                values.add(objectMapper.convertValue(item, Object.class));
            }
        }
        return values;
    }

    /**
     * 归一化 Elasticsearch 基础地址，避免拼接重复斜杠。
     *
     * @return 不带结尾斜杠的基础地址。
     */
    private String normalizedBaseUrl() {
        String baseUrl = properties.getBaseUrl();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    /**
     * 生成可选认证头；API Key 优先，Basic Auth 作为兼容选项。
     *
     * @return 认证头值，未配置认证时为空。
     */
    private Optional<String> authorizationHeader() {
        if (hasText(properties.getApiKey())) {
            return Optional.of("ApiKey " + properties.getApiKey());
        }
        if (hasText(properties.getUsername()) && hasText(properties.getPassword())) {
            String token = properties.getUsername() + ":" + properties.getPassword();
            return Optional.of("Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8)));
        }
        return Optional.empty();
    }

    /**
     * 判断配置文本是否存在有效内容。
     *
     * @param value 配置文本。
     * @return 存在非空白内容时返回 true。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
