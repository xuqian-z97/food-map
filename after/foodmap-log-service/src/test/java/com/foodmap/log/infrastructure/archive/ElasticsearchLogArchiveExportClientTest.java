package com.foodmap.log.infrastructure.archive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.foodmap.common.search.SearchClient;
import com.foodmap.common.search.SearchHit;
import com.foodmap.common.search.SearchRequest;
import com.foodmap.common.search.SearchResponse;
import com.foodmap.log.application.port.LogArchivePayload;
import com.foodmap.log.infrastructure.persistence.entity.LogArchiveRecordEntity;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElasticsearchLogArchiveExportClientTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void shouldExportWindowLogsAsGzipJsonLinesThroughSearchClient() throws Exception {
        CapturingSearchClient searchClient = new CapturingSearchClient(new SearchResponse(List.of(
                hit("2026-06-05T00:00:01Z", "trace-1", "first log", List.of()),
                hit("2026-06-05T00:00:02Z", "trace-2", "second log", List.of())
        )));
        ElasticsearchLogArchiveProperties properties = new ElasticsearchLogArchiveProperties();
        properties.setPageSize(2);
        ElasticsearchLogArchiveExportClient client = new ElasticsearchLogArchiveExportClient(
                properties,
                OBJECT_MAPPER,
                searchClient
        );

        LogArchivePayload payload = client.export(archiveRecord());

        assertEquals("application/x-ndjson+gzip", payload.contentType());
        assertEquals("foodmap-logs-2026.06.05", searchClient.requests.getFirst().indexName());
        assertEquals("2026-06-05T00:00:00Z", rangeValue(searchClient.requests.getFirst(), "gte"));
        assertEquals("2026-06-06T00:00:00Z", rangeValue(searchClient.requests.getFirst(), "lt"));
        assertEquals(2, searchClient.requests.getFirst().body().get("size"));
        assertEquals("""
                {"@timestamp":"2026-06-05T00:00:01Z","traceId":"trace-1","message":"first log"}
                {"@timestamp":"2026-06-05T00:00:02Z","traceId":"trace-2","message":"second log"}
                """, gunzip(payload.content()));
    }

    @Test
    void shouldContinueExportWithSearchAfterUntilNoHitsRemain() throws Exception {
        CapturingSearchClient searchClient = new CapturingSearchClient(
                new SearchResponse(List.of(hit("2026-06-05T00:00:01Z", "trace-1", null, List.of("2026-06-05T00:00:01Z")))),
                new SearchResponse(List.of(hit("2026-06-05T00:00:02Z", "trace-2", null, List.of("2026-06-05T00:00:02Z")))),
                new SearchResponse(List.of())
        );
        ElasticsearchLogArchiveProperties properties = new ElasticsearchLogArchiveProperties();
        properties.setPageSize(1);
        ElasticsearchLogArchiveExportClient client = new ElasticsearchLogArchiveExportClient(
                properties,
                OBJECT_MAPPER,
                searchClient
        );

        LogArchivePayload payload = client.export(archiveRecord());

        assertEquals(3, searchClient.requests.size());
        assertEquals(List.of("2026-06-05T00:00:01Z"), searchClient.requests.get(1).body().get("search_after"));
        assertEquals(List.of("2026-06-05T00:00:02Z"), searchClient.requests.get(2).body().get("search_after"));
        assertEquals("""
                {"@timestamp":"2026-06-05T00:00:01Z","traceId":"trace-1"}
                {"@timestamp":"2026-06-05T00:00:02Z","traceId":"trace-2"}
                """, gunzip(payload.content()));
    }

    @SuppressWarnings("unchecked")
    private static String rangeValue(SearchRequest request, String key) {
        Map<String, Object> query = (Map<String, Object>) request.body().get("query");
        Map<String, Object> range = (Map<String, Object>) query.get("range");
        Map<String, Object> timestamp = (Map<String, Object>) range.get("@timestamp");
        return (String) timestamp.get(key);
    }

    private static SearchHit hit(String timestamp, String traceId, String message, List<Object> sortValues) {
        ObjectNode source = OBJECT_MAPPER.createObjectNode();
        source.put("@timestamp", timestamp);
        source.put("traceId", traceId);
        if (message != null) {
            source.put("message", message);
        }
        return new SearchHit(source, sortValues);
    }

    private static LogArchiveRecordEntity archiveRecord() {
        LogArchiveRecordEntity entity = new LogArchiveRecordEntity();
        entity.setArchiveId(1001L);
        entity.setWindowStartTime(OffsetDateTime.parse("2026-06-05T00:00:00Z"));
        entity.setWindowEndTime(OffsetDateTime.parse("2026-06-06T00:00:00Z"));
        entity.setSourceIndexPattern("foodmap-logs-2026.06.05");
        entity.setObjectKey("logs/full/year=2026/month=06/day=05/foodmap-logs-2026-06-05.jsonl.gz");
        return entity;
    }

    private static String gunzip(byte[] content) throws IOException {
        try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(content))) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static class CapturingSearchClient implements SearchClient {
        private final List<SearchResponse> responses;
        private final List<SearchRequest> requests = new ArrayList<>();

        private CapturingSearchClient(SearchResponse... responses) {
            this.responses = new ArrayList<>(List.of(responses));
        }

        @Override
        public SearchResponse search(SearchRequest request) {
            requests.add(request);
            return responses.removeFirst();
        }
    }
}
