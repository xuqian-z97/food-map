package com.foodmap.common.search.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodmap.common.search.SearchClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * FoodMap 通用搜索自动配置，按需注册 Elasticsearch SearchClient。
 */
@AutoConfiguration
@EnableConfigurationProperties(ElasticsearchSearchProperties.class)
public class FoodMapSearchAutoConfiguration {

    /**
     * 创建通用 Elasticsearch SearchClient。
     *
     * @param properties Elasticsearch 搜索配置。
     * @param objectMapper JSON 序列化和解析组件。
     * @return 通用搜索客户端。
     */
    @Bean
    @ConditionalOnMissingBean(SearchClient.class)
    @ConditionalOnProperty(prefix = "foodmap.search.elasticsearch", name = "enabled", havingValue = "true")
    public SearchClient elasticsearchSearchClient(
            ElasticsearchSearchProperties properties,
            ObjectMapper objectMapper
    ) {
        return new ElasticsearchSearchClient(properties, objectMapper);
    }
}
