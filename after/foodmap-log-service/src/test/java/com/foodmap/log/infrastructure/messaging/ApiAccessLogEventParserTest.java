package com.foodmap.log.infrastructure.messaging;

import com.foodmap.log.infrastructure.persistence.entity.ApiAccessLogEntity;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiAccessLogEventParserTest {

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
}
