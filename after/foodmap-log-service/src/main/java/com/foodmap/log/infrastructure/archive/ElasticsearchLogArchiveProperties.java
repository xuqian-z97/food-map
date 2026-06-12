package com.foodmap.log.infrastructure.archive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Elasticsearch 全量日志归档导出配置，控制分页大小、时间字段和单窗口最大页数。
 */
@Component
@ConfigurationProperties(prefix = "foodmap.log.archive.elasticsearch")
public class ElasticsearchLogArchiveProperties {
    /**
     * 是否启用 Elasticsearch 真实导出适配器，默认关闭以避免本地误连外部依赖。
     */
    private boolean enabled;
    /**
     * 单次 `_search` 查询返回日志条数。
     */
    private int pageSize = 1000;
    /**
     * 单个归档窗口最多查询页数，用于避免异常游标导致无限导出。
     */
    private int maxPages = 1000;
    /**
     * 日志时间字段名，默认使用 Logstash 写入的 `@timestamp`。
     */
    private String timestampField = "@timestamp";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public String getTimestampField() {
        return timestampField;
    }

    public void setTimestampField(String timestampField) {
        this.timestampField = timestampField;
    }
}
