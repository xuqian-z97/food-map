package com.foodmap.log.infrastructure.archive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodmap.common.search.SearchClient;
import com.foodmap.common.search.SearchHit;
import com.foodmap.common.search.SearchRequest;
import com.foodmap.common.search.SearchResponse;
import com.foodmap.log.application.port.LogArchiveExportClient;
import com.foodmap.log.application.port.LogArchivePayload;
import com.foodmap.log.infrastructure.persistence.entity.LogArchiveRecordEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Elasticsearch 全量日志归档导出适配器，按归档窗口查询热日志并生成 gzip JSON Lines。
 */
@Component
@ConditionalOnProperty(prefix = "foodmap.log.archive.elasticsearch", name = "enabled", havingValue = "true")
public class ElasticsearchLogArchiveExportClient implements LogArchiveExportClient {
    private static final String CONTENT_TYPE = "application/x-ndjson+gzip";

    private final ElasticsearchLogArchiveProperties properties;
    private final ObjectMapper objectMapper;
    private final SearchClient searchClient;

    /**
     * 创建 Elasticsearch 归档导出适配器。
     *
     * @param properties Elasticsearch 归档导出配置。
     * @param objectMapper JSON 序列化和解析组件。
     * @param searchClient 通用搜索客户端。
     */
    public ElasticsearchLogArchiveExportClient(
            ElasticsearchLogArchiveProperties properties,
            ObjectMapper objectMapper,
            SearchClient searchClient
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.searchClient = searchClient;
    }

    /**
     * 查询指定窗口内的 Elasticsearch 热日志，并压缩为对象存储归档载荷。
     *
     * @param record 日志归档记录，包含窗口、来源索引和目标对象信息。
     * @return gzip JSON Lines 归档载荷。
     */
    @Override
    public LogArchivePayload export(LogArchiveRecordEntity record) {
        try {
            return new LogArchivePayload(exportPages(record), CONTENT_TYPE);
        } catch (IOException ex) {
            throw new IllegalStateException("Elasticsearch archive gzip export failed", ex);
        }
    }

    /**
     * 分页查询 Elasticsearch 并把每页 `_source` 写入同一个 gzip JSON Lines 文件。
     *
     * @param record 日志归档记录。
     * @return 压缩后的 JSON Lines 字节。
     */
    private byte[] exportPages(LogArchiveRecordEntity record) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        List<Object> searchAfter = List.of();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            for (int page = 0; page < Math.max(1, properties.getMaxPages()); page++) {
                SearchResponse response = search(record, searchAfter);
                if (response.hits().isEmpty()) {
                    gzip.finish();
                    return output.toByteArray();
                }
                List<Object> nextSearchAfter = writeHits(gzip, response.hits());
                if (nextSearchAfter.isEmpty()) {
                    gzip.finish();
                    return output.toByteArray();
                }
                searchAfter = nextSearchAfter;
            }
        }
        throw new IllegalStateException("Elasticsearch archive export exceeded maxPages=" + properties.getMaxPages());
    }

    /**
     * 执行单页 Elasticsearch 查询。
     *
     * @param record 日志归档记录。
     * @param searchAfter 上一页最后一条日志的排序游标，第一页传空集合。
     * @return 通用搜索响应。
     */
    private SearchResponse search(LogArchiveRecordEntity record, List<Object> searchAfter) {
        return searchClient.search(new SearchRequest(record.getSourceIndexPattern(), buildSearchBody(record, searchAfter)));
    }

    /**
     * 构建 Elasticsearch 查询体，按归档窗口过滤并按时间升序输出。
     *
     * @param record 日志归档记录。
     * @param searchAfter 上一页最后一条日志的排序游标。
     * @return Elasticsearch 查询体。
     */
    private Map<String, Object> buildSearchBody(LogArchiveRecordEntity record, List<Object> searchAfter) {
        String timestampField = properties.getTimestampField();
        Map<String, Object> rangeCondition = new LinkedHashMap<>();
        rangeCondition.put("gte", formatTime(record.getWindowStartTime()));
        rangeCondition.put("lt", formatTime(record.getWindowEndTime()));

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("size", Math.max(1, properties.getPageSize()));
        requestBody.put("track_total_hits", false);
        requestBody.put("_source", true);
        requestBody.put("sort", List.of(Map.of(timestampField, Map.of("order", "asc"))));
        requestBody.put("query", Map.of("range", Map.of(timestampField, rangeCondition)));
        if (!searchAfter.isEmpty()) {
            requestBody.put("search_after", searchAfter);
        }
        return requestBody;
    }

    /**
     * 从搜索命中中提取源文档写入 gzip，并返回下一页游标。
     *
     * @param gzip gzip 输出流。
     * @param hits 搜索命中列表。
     * @return 下一页 search_after 游标，无法继续分页时为空列表。
     */
    private List<Object> writeHits(GZIPOutputStream gzip, List<SearchHit> hits) throws IOException {
        List<Object> lastSort = List.of();
        for (SearchHit hit : hits) {
            gzip.write(objectMapper.writeValueAsBytes(hit.source()));
            gzip.write('\n');
            if (!hit.sortValues().isEmpty()) {
                lastSort = hit.sortValues();
            }
        }
        return lastSort;
    }

    /**
     * 格式化 Elasticsearch 时间窗口边界。
     *
     * @param time 时间边界。
     * @return ISO-8601 时间字符串。
     */
    private String formatTime(OffsetDateTime time) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time);
    }
}
