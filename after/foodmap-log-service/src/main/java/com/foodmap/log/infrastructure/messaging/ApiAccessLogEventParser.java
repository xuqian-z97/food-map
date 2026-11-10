package com.foodmap.log.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodmap.log.infrastructure.persistence.entity.ApiAccessLogEntity;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 接口访问日志 Kafka 事件解析器，按“JSON 顶层字段 > {@code message} 文本键值对 > {@code log} 原文”的优先级
 * 还原 {@code api_access_log} 需要的字段。
 *
 * <p>T-028 起应用日志由 logback 输出为单行 JSON，Fluent Bit 解析后 Kafka 消息同时带有 JSON 顶层字段和原始
 * {@code log} 文本；{@code message} 仍是 SafeLog 的 {@code eventName key=value ...} 文本。解析器必须优先读取
 * JSON 顶层字段，缺失时再解析 {@code message} 键值对，最后才用 {@code log} 原文兜底，否则会把整行 JSON
 * 当成事件名写入 {@code event_name varchar(128)}，同时让 method/path/status/durationMs 退化为默认值。</p>
 *
 * <p>兼容性：旧生产者的纯文本 {@code log} 载荷（{@code api.access.completed method=POST path=...}）仍可解析，
 * 避免升级期间在途消息丢失。事件名在写入实体前统一做长度保护，超长时按列宽截断。</p>
 */
@Component
public class ApiAccessLogEventParser {
    /**
     * 无法识别事件名时使用的默认事件名。
     */
    private static final String DEFAULT_EVENT_NAME = "api.access.completed";
    /**
     * 日志等级缺失时的默认值。
     */
    private static final String DEFAULT_LEVEL = "INFO";
    /**
     * HTTP 方法缺失时的默认值。
     */
    private static final String DEFAULT_METHOD = "UNKNOWN";
    /**
     * 请求路径缺失时的默认值。
     */
    private static final String DEFAULT_PATH = "/unknown";
    /**
     * 请求流水号缺失时的默认值。
     */
    private static final String UNKNOWN_REQUEST_ID = "unknown-request";
    /**
     * 服务名缺失时的默认值。
     */
    private static final String UNKNOWN_SERVICE_NAME = "unknown-service";
    /**
     * {@code api_access_log.event_name} 列宽，事件名超长时必须截断，避免入库异常。
     */
    private static final int EVENT_NAME_MAX_LENGTH = 128;
    /**
     * 解析 {@code log} 字段中单行 JSON 的只读 ObjectMapper，解析器不修改其配置，可在多线程间共享。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 将 Kafka 消息解析为接口访问摘要实体。
     *
     * <p>字段优先级固定为 JSON 顶层字段、{@code message} 文本键值对、{@code log} 原文；事件名在此基础上
     * 额外过滤 JSON 片段并做列宽截断。</p>
     *
     * @param payload Kafka JSON 消息体、原始日志对象或纯文本日志行。
     * @param topic 来源 topic。
     * @param partition 来源分区。
     * @param offset 来源 offset。
     * @return 可写入 `api_access_log` 的持久化实体。
     */
    public ApiAccessLogEntity parse(Object payload, String topic, int partition, long offset) {
        Map<String, String> jsonFields = extractJsonFields(payload);
        String rawLog = jsonFields.get("log");
        String message = jsonFields.get("message");
        Map<String, String> messageFields = parseKeyValueLog(message);
        Map<String, String> logFields = parseLogFields(rawLog);
        Map<String, String> fields = mergeByPriority(logFields, messageFields, jsonFields);

        String requestId = firstNonBlank(fields.get("requestId"), UNKNOWN_REQUEST_ID);
        ApiAccessLogEntity entity = new ApiAccessLogEntity();
        entity.setRequestId(requestId);
        entity.setTraceId(firstNonBlank(fields.get("traceId"), requestId));
        entity.setSpanId(blankToNull(fields.get("spanId")));
        entity.setServiceName(firstNonBlank(fields.get("serviceName"), UNKNOWN_SERVICE_NAME));
        entity.setEventName(resolveEventName(jsonFields.get("eventName"), message, rawLog, logFields));
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
     * 从 Kafka 载荷中提取 JSON 顶层字段。
     *
     * <p>Map 载荷直接按顶层键值展开；字符串载荷若是单行 JSON 则解析为顶层字段，否则整体作为 {@code log}
     * 原文保留，保证旧纯文本生产者仍可解析。</p>
     *
     * @param payload Kafka 消息载荷。
     * @return 顶层字段映射，键和值均已转为非空字符串。
     */
    private Map<String, String> extractJsonFields(Object payload) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (payload instanceof Map<?, ?> payloadMap) {
            payloadMap.forEach((key, value) -> putIfUsable(fields, key, value));
            return fields;
        }
        if (payload instanceof CharSequence text) {
            String raw = text.toString();
            if (looksLikeJsonObject(raw)) {
                fields.putAll(parseJsonObject(raw));
            } else if (!raw.isBlank()) {
                fields.put("log", raw);
            }
            return fields;
        }
        if (payload != null) {
            fields.put("log", String.valueOf(payload));
        }
        return fields;
    }

    /**
     * 将单个键值写入字段映射，忽略空键、空值和空白值。
     *
     * @param fields 目标字段映射。
     * @param key 字段名。
     * @param value 字段值，允许数值、布尔等非字符串类型。
     */
    private void putIfUsable(Map<String, String> fields, Object key, Object value) {
        if (key == null || value == null) {
            return;
        }
        String text = value instanceof CharSequence ? value.toString() : String.valueOf(value);
        if (!text.isBlank()) {
            fields.put(String.valueOf(key), text);
        }
    }

    /**
     * 解析 {@code log} 原文：单行 JSON 按 JSON 字段解析，其余文本按 SafeLog 键值对解析。
     *
     * @param rawLog 原始日志文本，可为空。
     * @return 解析得到的字段映射，无法解析时返回空映射。
     */
    private Map<String, String> parseLogFields(String rawLog) {
        if (rawLog == null || rawLog.isBlank()) {
            return new LinkedHashMap<>();
        }
        if (looksLikeJsonObject(rawLog)) {
            return parseJsonObject(rawLog);
        }
        return parseKeyValueLog(rawLog);
    }

    /**
     * 解析单行 JSON 对象为字段映射。
     *
     * <p>只保留可转为文本的标量值；嵌套对象和数组转为紧凑 JSON 文本，null 值直接跳过。解析失败时返回空映射，
     * 由调用方继续走文本键值对或默认值兜底，不能因单条脏日志中断消费。</p>
     *
     * @param json 单行 JSON 文本。
     * @return 解析得到的字段映射。
     */
    private Map<String, String> parseJsonObject(String json) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (json == null || json.isBlank()) {
            return fields;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            if (root == null || !root.isObject()) {
                return fields;
            }
            Iterator<Map.Entry<String, JsonNode>> iterator = root.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                JsonNode value = entry.getValue();
                if (value == null || value.isNull()) {
                    continue;
                }
                String text = value.isValueNode() ? value.asText() : value.toString();
                if (text != null && !text.isBlank()) {
                    fields.put(entry.getKey(), text);
                }
            }
        } catch (JsonProcessingException ex) {
            return new LinkedHashMap<>();
        }
        return fields;
    }

    /**
     * 按“低优先级在前、高优先级在后”的顺序合并字段，后写入的非空值覆盖先写入的值。
     *
     * @param logFields 来自 {@code log} 原文的字段，优先级最低。
     * @param messageFields 来自 {@code message} 文本键值对的字段，优先级居中。
     * @param jsonFields 来自 JSON 顶层字段的映射，优先级最高。
     * @return 合并后的字段映射。
     */
    private Map<String, String> mergeByPriority(Map<String, String> logFields,
                                                Map<String, String> messageFields,
                                                Map<String, String> jsonFields) {
        Map<String, String> merged = new LinkedHashMap<>();
        putAllNonBlank(merged, logFields);
        putAllNonBlank(merged, messageFields);
        putAllNonBlank(merged, jsonFields);
        return merged;
    }

    /**
     * 将源映射中的非空值写入目标映射。
     *
     * @param target 目标字段映射。
     * @param source 源字段映射。
     */
    private void putAllNonBlank(Map<String, String> target, Map<String, String> source) {
        source.forEach((key, value) -> {
            if (key != null && value != null && !value.isBlank()) {
                target.put(key, value);
            }
        });
    }

    /**
     * 按优先级解析访问事件名：JSON 顶层 {@code eventName}、{@code message} 首个 token、{@code log} 原文首个 token。
     *
     * <p>任何看起来像 JSON 片段的候选值都会被跳过，避免把整行 JSON 写入 {@code event_name}；最终结果按
     * {@link #EVENT_NAME_MAX_LENGTH} 截断，保证不超过数据库列宽。</p>
     *
     * @param jsonEventName JSON 顶层事件名。
     * @param message SafeLog 文本，首个空格分隔 token 是事件名。
     * @param rawLog 原始日志文本。
     * @param logFields {@code log} 原文解析出的字段，用于从 JSON 原文的 message 中提取事件名。
     * @return 可直接写入 {@code event_name} 的事件名。
     */
    private String resolveEventName(String jsonEventName,
                                    String message,
                                    String rawLog,
                                    Map<String, String> logFields) {
        String logEventName = looksLikeJsonObject(rawLog)
                ? firstPlausibleEventName(logFields.get("eventName"), extractFirstToken(logFields.get("message")))
                : extractFirstToken(rawLog);
        String candidate = firstPlausibleEventName(jsonEventName, extractFirstToken(message), logEventName);
        return truncate(candidate == null ? DEFAULT_EVENT_NAME : candidate, EVENT_NAME_MAX_LENGTH);
    }

    /**
     * 从候选值中选出第一个可信事件名，候选值按优先级排列。
     *
     * @param values 按优先级排列的候选事件名。
     * @return 第一个可信事件名；没有可信候选时返回 null。
     */
    private String firstPlausibleEventName(String... values) {
        for (String value : values) {
            if (isPlausibleEventName(value)) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * 提取空格分隔文本的首个 token。
     *
     * @param text 待提取文本，可为空。
     * @return 首个非空白 token，文本为空时返回 null。
     */
    private String extractFirstToken(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();
        String[] tokens = trimmed.split("\\s+", 2);
        return tokens.length == 0 ? null : tokens[0];
    }

    /**
     * 判断候选事件名是否可信。
     *
     * <p>可信事件名只允许字母、数字、点、下划线、短横线和冒号，不能以 JSON 花括号或方括号开头，
     * 也不能包含双引号、等号或空白。这样即使采集链路只传来整行 JSON，也不会被当成事件名。</p>
     *
     * @param value 候选事件名。
     * @return 是可信事件名时返回 true。
     */
    private boolean isPlausibleEventName(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String candidate = value.trim();
        if (candidate.startsWith("{") || candidate.startsWith("[")) {
            return false;
        }
        for (int index = 0; index < candidate.length(); index++) {
            char character = candidate.charAt(index);
            boolean allowed = Character.isLetterOrDigit(character)
                    || character == '.'
                    || character == '_'
                    || character == '-'
                    || character == ':';
            if (!allowed) {
                return false;
            }
        }
        return true;
    }

    /**
     * 解析 SafeLog {@code key=value} 文本字段。
     *
     * @param log 原始日志文本或 SafeLog message 文本。
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
     * 判断文本是否是单行 JSON 对象。
     *
     * @param text 待判断文本。
     * @return 以花括号包裹时返回 true。
     */
    private boolean looksLikeJsonObject(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        return trimmed.length() >= 2 && trimmed.startsWith("{") && trimmed.endsWith("}");
    }

    /**
     * 按最大长度截断字符串。
     *
     * @param value 待截断字符串，可为 null。
     * @param maxLength 允许的最大长度。
     * @return 截断后的字符串；入参为 null 时返回 null。
     */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * 从多个候选值中选择第一个非空字符串。
     *
     * @param values 候选值。
     * @return 第一个非空字符串；全部为空时返回空字符串。
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
     * <p>logback 输出的 ISO_OFFSET_DATE_TIME 既可能是 {@code +08:00}，也可能是 UTC 的 {@code Z}，
     * {@link OffsetDateTime#parse(CharSequence)} 都支持。</p>
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
