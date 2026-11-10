package com.foodmap.common.logging;

import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 安全日志门面，统一输出事件名、脱敏后的结构化字段和日志类型。
 *
 * <p>业务代码不应直接拼接关键业务日志，应通过该类确保字段格式稳定且敏感值被脱敏。</p>
 *
 * <p>每次输出都会在 MDC 中临时写入 {@link LogMdcKeys#LOG_TYPE}，使 JSON 日志带稳定的 logType 字段，
 * 供 Fluent Bit 按字段路由到 application、api-access、sql、audit、security 日志主题；禁止通过 message
 * 文本区分日志类型。</p>
 */
public final class SafeLog {

    private SafeLog() {
    }

    /**
     * 输出关键业务成功事件，例如推荐创建成功或评论发布成功，日志类型默认为 application。
     *
     * @param logger 业务类持有的 Slf4j Logger。
     * @param eventName 稳定的业务事件名，用于检索和告警聚合。
     * @param fields 需要输出的结构化业务字段。
     */
    public static void info(Logger logger, String eventName, LogField... fields) {
        info(logger, LogMdcKeys.LOG_TYPE_APPLICATION, eventName, fields);
    }

    /**
     * 按指定日志类型输出关键业务成功事件，例如审计日志必须显式传 audit。
     *
     * @param logger 业务类持有的 Slf4j Logger。
     * @param logType 日志类型，取值来自 {@link LogMdcKeys#LOG_TYPE} 相关常量，空值回退 application。
     * @param eventName 稳定的业务事件名，用于检索和告警聚合。
     * @param fields 需要输出的结构化业务字段。
     */
    public static void info(Logger logger, String logType, String eventName, LogField... fields) {
        if (logger != null && logger.isInfoEnabled()) {
            withLogType(logType, () -> logger.info("{} {}", eventName, formatFields(fields)));
        }
    }

    /**
     * 输出排查型调试日志，例如按 requestId 临时打开的 SQL 明细，日志类型默认为 application。
     *
     * @param logger 业务类持有的 Slf4j Logger。
     * @param eventName 稳定的调试事件名，用于检索排查。
     * @param fields 需要输出的结构化业务字段。
     */
    public static void debug(Logger logger, String eventName, LogField... fields) {
        debug(logger, LogMdcKeys.LOG_TYPE_APPLICATION, eventName, fields);
    }

    /**
     * 按指定日志类型输出排查型调试日志，SQL 明细必须显式传 sql。
     *
     * @param logger 业务类持有的 Slf4j Logger。
     * @param logType 日志类型，取值来自 {@link LogMdcKeys#LOG_TYPE} 相关常量，空值回退 application。
     * @param eventName 稳定的调试事件名，用于检索排查。
     * @param fields 需要输出的结构化业务字段。
     */
    public static void debug(Logger logger, String logType, String eventName, LogField... fields) {
        if (logger != null && logger.isDebugEnabled()) {
            withLogType(logType, () -> logger.debug("{} {}", eventName, formatFields(fields)));
        }
    }

    /**
     * 输出可恢复问题，例如参数错误、权限失败、幂等重复和外部依赖降级，日志类型默认为 application。
     *
     * @param logger 业务类持有的 Slf4j Logger。
     * @param eventName 稳定的告警事件名，用于检索和告警聚合。
     * @param fields 需要输出的结构化业务字段。
     */
    public static void warn(Logger logger, String eventName, LogField... fields) {
        warn(logger, LogMdcKeys.LOG_TYPE_APPLICATION, eventName, fields);
    }

    /**
     * 按指定日志类型输出可恢复问题，访问摘要和 SQL 慢查询必须显式传对应类型。
     *
     * @param logger 业务类持有的 Slf4j Logger。
     * @param logType 日志类型，取值来自 {@link LogMdcKeys#LOG_TYPE} 相关常量，空值回退 application。
     * @param eventName 稳定的告警事件名，用于检索和告警聚合。
     * @param fields 需要输出的结构化业务字段。
     */
    public static void warn(Logger logger, String logType, String eventName, LogField... fields) {
        if (logger != null && logger.isWarnEnabled()) {
            withLogType(logType, () -> logger.warn("{} {}", eventName, formatFields(fields)));
        }
    }

    /**
     * 输出系统异常或不可恢复错误，异常对象用于保留堆栈，日志类型默认为 application。
     *
     * @param logger 业务类持有的 Slf4j Logger。
     * @param eventName 稳定的错误事件名，用于检索和告警聚合。
     * @param throwable 需要保留堆栈的异常对象。
     * @param fields 需要输出的结构化业务字段。
     */
    public static void error(Logger logger, String eventName, Throwable throwable, LogField... fields) {
        error(logger, LogMdcKeys.LOG_TYPE_APPLICATION, eventName, throwable, fields);
    }

    /**
     * 按指定日志类型输出系统异常或不可恢复错误。
     *
     * @param logger 业务类持有的 Slf4j Logger。
     * @param logType 日志类型，取值来自 {@link LogMdcKeys#LOG_TYPE} 相关常量，空值回退 application。
     * @param eventName 稳定的错误事件名，用于检索和告警聚合。
     * @param throwable 需要保留堆栈的异常对象。
     * @param fields 需要输出的结构化业务字段。
     */
    public static void error(Logger logger, String logType, String eventName, Throwable throwable, LogField... fields) {
        if (logger != null && logger.isErrorEnabled()) {
            withLogType(logType, () -> logger.error(eventName + " " + formatFields(fields), throwable));
        }
    }

    /**
     * 将日志字段格式化为稳定的 {@code key=value} 形式，并在输出前统一脱敏。
     *
     * @param fields 待格式化的结构化日志字段。
     * @return 脱敏后的稳定日志字段文本。
     */
    public static String formatFields(LogField... fields) {
        String contextFields = formatContextFields();
        String businessFields = formatBusinessFields(fields);
        if (contextFields.isBlank()) {
            return businessFields;
        }
        if (businessFields.isBlank()) {
            return contextFields;
        }
        return contextFields + " " + businessFields;
    }

    /**
     * 在 MDC 中临时写入本次日志类型并在输出后恢复原值，避免线程复用导致日志类型串到其他事件。
     *
     * @param logType 本次日志类型，空值回退 application。
     * @param action 实际执行日志输出的动作。
     */
    private static void withLogType(String logType, Runnable action) {
        String previous = MDC.get(LogMdcKeys.LOG_TYPE);
        MDC.put(LogMdcKeys.LOG_TYPE, resolveLogType(logType));
        try {
            action.run();
        } finally {
            if (previous == null || previous.isBlank()) {
                MDC.remove(LogMdcKeys.LOG_TYPE);
            } else {
                MDC.put(LogMdcKeys.LOG_TYPE, previous);
            }
        }
    }

    /**
     * 归一化日志类型，只允许 FoodMap 已定义的分类，避免空白值或未知值写入 MDC 后产生无法路由的日志。
     *
     * <p>未知分类会回退为 application，保证 JSON 中 logType 始终是可路由的稳定取值。</p>
     *
     * @param logType 调用点传入的日志类型。
     * @return 可直接写入 MDC 的日志类型。
     */
    private static String resolveLogType(String logType) {
        if (logType == null) {
            return LogMdcKeys.LOG_TYPE_APPLICATION;
        }
        String normalized = logType.trim();
        return switch (normalized) {
            case LogMdcKeys.LOG_TYPE_APPLICATION,
                 LogMdcKeys.LOG_TYPE_API_ACCESS,
                 LogMdcKeys.LOG_TYPE_SQL,
                 LogMdcKeys.LOG_TYPE_AUDIT,
                 LogMdcKeys.LOG_TYPE_SECURITY -> normalized;
            default -> LogMdcKeys.LOG_TYPE_APPLICATION;
        };
    }

    /**
     * 将业务日志字段格式化为稳定的 {@code key=value} 形式。
     *
     * @param fields 待格式化的结构化日志字段。
     * @return 脱敏后的业务日志字段文本。
     */
    private static String formatBusinessFields(LogField... fields) {
        if (fields == null || fields.length == 0) {
            return "";
        }
        return Arrays.stream(fields)
                .map(field -> field.name() + "=" + LogMasker.maskByFieldName(field.name(), field.value()))
                .collect(Collectors.joining(" "));
    }

    /**
     * 从 MDC 提取 FoodMap 标准链路字段，确保业务日志自动带上 requestId 和 traceId。
     *
     * @return 格式化后的链路上下文字段。
     */
    private static String formatContextFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(LogMdcKeys.REQUEST_ID, MDC.get(LogMdcKeys.REQUEST_ID));
        fields.put(LogMdcKeys.TRACE_ID, MDC.get(LogMdcKeys.TRACE_ID));
        fields.put(LogMdcKeys.SPAN_ID, MDC.get(LogMdcKeys.SPAN_ID));
        fields.put(LogMdcKeys.SERVICE_NAME, MDC.get(LogMdcKeys.SERVICE_NAME));
        fields.put(LogMdcKeys.ACCOUNT_ID, MDC.get(LogMdcKeys.ACCOUNT_ID));
        fields.put(LogMdcKeys.USER_ID, MDC.get(LogMdcKeys.USER_ID));
        return fields.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> entry.getKey() + "=" + LogMasker.maskByFieldName(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(" "));
    }
}
