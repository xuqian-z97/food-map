package com.foodmap.log.infrastructure.messaging;

import com.foodmap.log.infrastructure.persistence.entity.ApiAccessLogEntity;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 接口访问日志 Kafka 事件解析器，兼容结构化字段和 SafeLog 文本键值对。
 */
@Component
public class ApiAccessLogEventParser {
    private static final String DEFAULT_EVENT_NAME = "api.access.completed";
    private static final String DEFAULT_LEVEL = "INFO";
    private static final String DEFAULT_METHOD = "UNKNOWN";
    private static final String DEFAULT_PATH = "/unknown";

    /**
     * 将 Kafka 消息解析为接口访问摘要实体。
     *
     * @param payload Kafka JSON 消息体或原始日志对象。
     * @param topic 来源 topic。
     * @param partition 来源分区。
     * @param offset 来源 offset。
     * @return 可写入 `api_access_log` 的持久化实体。
     */
    public ApiAccessLogEntity parse(Object payload, String topic, int partition, long offset) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (payload instanceof Map<?, ?> payloadMap) {
            payloadMap.forEach((key, value) -> {
                if (key != null && value != null) {
                    fields.put(String.valueOf(key), String.valueOf(value));
                }
            });
        } else if (payload != null) {
            fields.put("log", String.valueOf(payload));
        }
        String log = fields.get("log");
        fields.putAll(parseKeyValueLog(log));
        String eventName = firstNonBlank(fields.get("eventName"), extractEventName(log), DEFAULT_EVENT_NAME);
        ApiAccessLogEntity entity = new ApiAccessLogEntity();
        entity.setRequestId(firstNonBlank(fields.get("requestId"), "unknown-request"));
        entity.setTraceId(firstNonBlank(fields.get("traceId"), entity.getRequestId()));
        entity.setSpanId(blankToNull(fields.get("spanId")));
        entity.setServiceName(firstNonBlank(fields.get("serviceName"), "unknown-service"));
        entity.setEventName(eventName);
        entity.setLogLevel(firstNonBlank(fields.get("level"), fields.get("logLevel"), DEFAULT_LEVEL));
        entity.setHttpMethod(firstNonBlank(fields.get("method"), fields.get("httpMethod"), DEFAULT_METHOD));
        entity.setRequestPath(firstNonBlank(fields.get("path"), fields.get("requestPath"), DEFAULT_PATH));
        entity.setHttpStatus(parseInteger(firstNonBlank(fields.get("status"), fields.get("httpStatus")), 0));
        entity.setDurationMs(parseLong(fields.get("durationMs"), 0L));
        entity.setClientIpMasked(blankToNull(firstNonBlank(fields.get("clientIpMasked"), fields.get("clientIp"))));
        entity.setAccountId(parseNullableLong(fields.get("accountId")));
        entity.setUserId(parseNullableLong(fields.get("userId")));
        entity.setGatewayRouteId(blankToNull(fields.get("gatewayRouteId")));
        entity.setErrorCode(blankToNull(firstNonBlank(fields.get("errorCode"), fields.get("code"))));
        entity.setOccurredTime(parseOccurredTime(firstNonBlank(fields.get("timestamp"), fields.get("@timestamp"))));
        entity.setSourceTopic(topic);
        entity.setSourcePartition(partition);
        entity.setSourceOffset(offset);
        return entity;
    }

    /**
     * 从 SafeLog 的首个 token 中提取事件名。
     *
     * @param log 原始日志文本。
     * @return 事件名，可为空。
     */
    private String extractEventName(String log) {
        if (log == null || log.isBlank()) {
            return null;
        }
        int firstSpace = log.indexOf(' ');
        return firstSpace > 0 ? log.substring(0, firstSpace) : log;
    }

    /**
     * 解析 SafeLog `key=value` 文本字段。
     *
     * @param log 原始日志文本。
     * @return 解析得到的字段映射。
     */
    private Map<String, String> parseKeyValueLog(String log) {
        Map<String, String> parsed = new LinkedHashMap<>();
        if (log == null || log.isBlank()) {
            return parsed;
        }
        String[] tokens = log.split("\\s+");
        for (String token : tokens) {
            int equalsIndex = token.indexOf('=');
            if (equalsIndex > 0 && equalsIndex < token.length() - 1) {
                parsed.put(token.substring(0, equalsIndex), token.substring(equalsIndex + 1));
            }
        }
        return parsed;
    }

    /**
     * 从多个候选值中选择第一个非空字符串。
     *
     * @param values 候选值。
     * @return 第一个非空字符串。
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    /**
     * 将空字符串转为空引用。
     *
     * @param value 待处理字符串。
     * @return 非空字符串或 null。
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * 解析整数，失败时返回默认值。
     *
     * @param value 字符串值。
     * @param defaultValue 默认值。
     * @return 整数值。
     */
    private int parseInteger(String value, int defaultValue) {
        try {
            return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * 解析长整数，失败时返回默认值。
     *
     * @param value 字符串值。
     * @param defaultValue 默认值。
     * @return 长整数值。
     */
    private long parseLong(String value, long defaultValue) {
        try {
            return value == null || value.isBlank() ? defaultValue : Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * 解析可空长整数。
     *
     * @param value 字符串值。
     * @return 长整数值或 null。
     */
    private Long parseNullableLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 解析事件发生时间，缺失或格式异常时使用当前 UTC 时间。
     *
     * @param value 时间字符串。
     * @return 事件发生时间。
     */
    private OffsetDateTime parseOccurredTime(String value) {
        if (value == null || value.isBlank()) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}
