package com.foodmap.log.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodmap.log.infrastructure.persistence.entity.ApiAccessLogEntity;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiAccessLogEventParserTest {
    /**
     * T-028 实际产出的单行 JSON 日志夹具。
     *
     * <p>该行由 `foodmap-common` 的 logback JSON encoder 输出：timestamp 为 ISO_OFFSET_DATE_TIME（+08:00），
     * logType 为 api-access，requestId/traceId/spanId/serviceName/accountId/userId 来自 MDC，
     * SafeLog 的 `eventName key=value ...` 文本保存在 message 中。</p>
     */
    private static final String T028_API_ACCESS_JSON_LINE = """
            {"timestamp":"2026-11-09T16:20:31.482+08:00","level":"INFO","logType":"api-access","logger":"com.foodmap.common.logging.ApiAccessLogFilter","thread":"http-nio-8081-exec-1","requestId":"req-t028-8f3c1d","traceId":"trace-t028-8f3c1d","spanId":"span-t028-0001","serviceName":"foodmap-auth-service","accountId":"10001","userId":"20001","message":"api.access.completed method=POST path=/api/auth/login status=200 durationMs=35 clientIp=10.0.0.1"}
            """;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void shouldParseSafeLogTextAndKafkaPosition() {
        ApiAccessLogEventParser parser = new ApiAccessLogEventParser();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("log", "api.access.completed requestId=req-1 traceId=trace-1 spanId=span-1 serviceName=foodmap-auth-service method=POST path=/api/auth/login status=200 durationMs=35 clientIp=10.0.0.1 accountId=100 userId=200");
        payload.put("level", "INFO");

        ApiAccessLogEntity entity = parser.parse(payload, "foodmap.logs.api-access", 2, 9L);

        assertEquals("req-1", entity.getRequestId());
        assertEquals("trace-1", entity.getTraceId());
        assertEquals("api.access.completed", entity.getEventName());
        assertEquals("foodmap-auth-service", entity.getServiceName());
        assertEquals("POST", entity.getHttpMethod());
        assertEquals("/api/auth/login", entity.getRequestPath());
        assertEquals(200, entity.getHttpStatus());
        assertEquals(35L, entity.getDurationMs());
        assertEquals(100L, entity.getAccountId());
        assertEquals(200L, entity.getUserId());
        assertEquals("foodmap.logs.api-access", entity.getSourceTopic());
        assertEquals(2, entity.getSourcePartition());
        assertEquals(9L, entity.getSourceOffset());
    }

    @Test
    void shouldPreferStructuredFieldsWhenPresent() {
        ApiAccessLogEventParser parser = new ApiAccessLogEventParser();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", "req-structured");
        payload.put("traceId", "trace-structured");
        payload.put("serviceName", "foodmap-user-service");
        payload.put("eventName", "api.access.slow");
        payload.put("method", "GET");
        payload.put("path", "/api/users/me");
        payload.put("status", 504);
        payload.put("durationMs", 1200);

        ApiAccessLogEntity entity = parser.parse(payload, "foodmap.logs.api-access", 0, 1L);

        assertEquals("req-structured", entity.getRequestId());
        assertEquals("trace-structured", entity.getTraceId());
        assertEquals("api.access.slow", entity.getEventName());
        assertEquals("foodmap-user-service", entity.getServiceName());
        assertEquals(504, entity.getHttpStatus());
        assertEquals(1200L, entity.getDurationMs());
    }

    @Test
    void shouldParseLegacyPlainTextPayloadWhenPayloadIsString() {
        ApiAccessLogEventParser parser = new ApiAccessLogEventParser();

        ApiAccessLogEntity entity = parser.parse(
                "api.access.completed requestId=req-legacy traceId=trace-legacy serviceName=foodmap-gateway-service "
                        + "method=GET path=/api/stores status=200 durationMs=12 clientIp=10.0.0.9",
                "foodmap.logs.api-access", 0, 6L);

        assertEquals("api.access.completed", entity.getEventName());
        assertEquals("req-legacy", entity.getRequestId());
        assertEquals("trace-legacy", entity.getTraceId());
        assertEquals("foodmap-gateway-service", entity.getServiceName());
        assertEquals("GET", entity.getHttpMethod());
        assertEquals("/api/stores", entity.getRequestPath());
        assertEquals(200, entity.getHttpStatus());
        assertEquals(12L, entity.getDurationMs());
    }

    @Test
    void shouldParseT028JsonLinePayloadWithTopLevelFields() throws Exception {
        JsonNode fixtureLine = OBJECT_MAPPER.readTree(T028_API_ACCESS_JSON_LINE.strip());
        assertEquals("api-access", fixtureLine.path("logType").asText(),
                "T-028 夹具必须是 api-access 路由类型的 JSON 行");

        ApiAccessLogEventParser parser = new ApiAccessLogEventParser();
        ApiAccessLogEntity entity = parser.parse(t028KafkaPayload(), "foodmap.logs.api-access", 3, 42L);

        assertEquals("api.access.completed", entity.getEventName());
        assertFalse(entity.getEventName().startsWith("{"), "事件名不能是整行 JSON");
        assertTrue(entity.getEventName().length() <= 128, "事件名必须在 event_name 列宽内");
        assertEquals("POST", entity.getHttpMethod());
        assertEquals("/api/auth/login", entity.getRequestPath());
        assertEquals(200, entity.getHttpStatus());
        assertEquals(35L, entity.getDurationMs());
        assertEquals("req-t028-8f3c1d", entity.getRequestId());
        assertEquals("trace-t028-8f3c1d", entity.getTraceId());
        assertEquals("span-t028-0001", entity.getSpanId());
        assertEquals("foodmap-auth-service", entity.getServiceName());
        assertEquals("INFO", entity.getLogLevel());
        assertEquals("10.0.0.1", entity.getClientIpMasked());
        assertEquals(10001L, entity.getAccountId());
        assertEquals(20001L, entity.getUserId());
        assertEquals(OffsetDateTime.parse("2026-11-09T16:20:31.482+08:00"), entity.getOccurredTime());
        assertEquals("foodmap.logs.api-access", entity.getSourceTopic());
        assertEquals(3, entity.getSourcePartition());
        assertEquals(42L, entity.getSourceOffset());
    }

    @Test
    void shouldParseT028JsonLineFromRawJsonPayload() {
        ApiAccessLogEventParser parser = new ApiAccessLogEventParser();

        ApiAccessLogEntity entity = parser.parse(
                T028_API_ACCESS_JSON_LINE.strip(), "foodmap.logs.api-access", 0, 7L);

        assertEquals("api.access.completed", entity.getEventName());
        assertEquals("POST", entity.getHttpMethod());
        assertEquals("/api/auth/login", entity.getRequestPath());
        assertEquals(200, entity.getHttpStatus());
        assertEquals(35L, entity.getDurationMs());
        assertEquals("req-t028-8f3c1d", entity.getRequestId());
        assertEquals("trace-t028-8f3c1d", entity.getTraceId());
        assertEquals("foodmap-auth-service", entity.getServiceName());
        assertEquals(OffsetDateTime.parse("2026-11-09T16:20:31.482+08:00"), entity.getOccurredTime());
    }

    @Test
    void shouldPreferJsonTopLevelFieldsOverMessageKeyValueText() {
        ApiAccessLogEventParser parser = new ApiAccessLogEventParser();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventName", "api.access.slow");
        payload.put("method", "GET");
        payload.put("path", "/api/json/winner");
        payload.put("status", 504);
        payload.put("durationMs", 1200);
        payload.put("message", "api.access.completed method=POST path=/api/message/loser status=200 durationMs=35");

        ApiAccessLogEntity entity = parser.parse(payload, "foodmap.logs.api-access", 0, 2L);

        assertEquals("api.access.slow", entity.getEventName());
        assertEquals("GET", entity.getHttpMethod());
        assertEquals("/api/json/winner", entity.getRequestPath());
        assertEquals(504, entity.getHttpStatus());
        assertEquals(1200L, entity.getDurationMs());
    }

    @Test
    void shouldPreferMessageKeyValueTextOverLogRawText() {
        ApiAccessLogEventParser parser = new ApiAccessLogEventParser();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("log", "api.access.completed method=POST path=/api/log/loser status=200 durationMs=35");
        payload.put("message", "api.access.slow method=PATCH path=/api/message/winner status=429 durationMs=900");

        ApiAccessLogEntity entity = parser.parse(payload, "foodmap.logs.api-access", 0, 3L);

        assertEquals("api.access.slow", entity.getEventName());
        assertEquals("PATCH", entity.getHttpMethod());
        assertEquals("/api/message/winner", entity.getRequestPath());
        assertEquals(429, entity.getHttpStatus());
        assertEquals(900L, entity.getDurationMs());
    }

    @Test
    void shouldTruncateOverlongEventNameToColumnWidth() {
        ApiAccessLogEventParser parser = new ApiAccessLogEventParser();
        String overlongEventName = "event." + "x".repeat(200);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", overlongEventName + " method=GET path=/api/overlong status=201 durationMs=5");

        ApiAccessLogEntity entity = parser.parse(payload, "foodmap.logs.api-access", 0, 4L);

        assertEquals(128, entity.getEventName().length());
        assertEquals(overlongEventName.substring(0, 128), entity.getEventName());
        assertEquals("GET", entity.getHttpMethod());
        assertEquals("/api/overlong", entity.getRequestPath());
        assertEquals(201, entity.getHttpStatus());
        assertEquals(5L, entity.getDurationMs());
    }

    @Test
    void shouldFallBackToMessageEventNameWhenJsonEventNameIsJsonText() {
        ApiAccessLogEventParser parser = new ApiAccessLogEventParser();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventName", "{\"unexpected\":\"json\"}");
        payload.put("message", "api.access.completed method=GET path=/api/stores status=200 durationMs=3");

        ApiAccessLogEntity entity = parser.parse(payload, "foodmap.logs.api-access", 0, 8L);

        assertEquals("api.access.completed", entity.getEventName());
    }

    @Test
    void shouldNeverUseWholeJsonLineAsEventNameWhenOnlyLogPayloadExists() {
        ApiAccessLogEventParser parser = new ApiAccessLogEventParser();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("log", "{\"timestamp\":\"2026-11-09T16:20:31.482+08:00\",\"level\":\"INFO\",\"logType\":\"api-access\",\"serviceName\":\"foodmap-gateway-service\"}");

        ApiAccessLogEntity entity = parser.parse(payload, "foodmap.logs.api-access", 0, 5L);

        assertEquals("api.access.completed", entity.getEventName());
        assertFalse(entity.getEventName().contains("{"));
        assertTrue(entity.getEventName().length() <= 128);
        assertEquals("foodmap-gateway-service", entity.getServiceName());
        assertNotNull(entity.getOccurredTime());
    }

    /**
     * 构造 Fluent Bit `Format json` 输出到 Kafka、再被 JsonDeserializer 还原后的典型载荷。
     *
     * <p>解析后的顶层字段与原始 `log` 文本同时存在，与 `Preserve_Key` 的采集行为一致。</p>
     *
     * @return T-028 日志行对应的 Kafka 载荷。
     */
    private Map<String, Object> t028KafkaPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("log", T028_API_ACCESS_JSON_LINE.strip());
        payload.put("timestamp", "2026-11-09T16:20:31.482+08:00");
        payload.put("level", "INFO");
        payload.put("logType", "api-access");
        payload.put("logger", "com.foodmap.common.logging.ApiAccessLogFilter");
        payload.put("thread", "http-nio-8081-exec-1");
        payload.put("requestId", "req-t028-8f3c1d");
        payload.put("traceId", "trace-t028-8f3c1d");
        payload.put("spanId", "span-t028-0001");
        payload.put("serviceName", "foodmap-auth-service");
        payload.put("accountId", "10001");
        payload.put("userId", "20001");
        payload.put("message", "api.access.completed method=POST path=/api/auth/login status=200 durationMs=35 clientIp=10.0.0.1");
        return payload;
    }
}
