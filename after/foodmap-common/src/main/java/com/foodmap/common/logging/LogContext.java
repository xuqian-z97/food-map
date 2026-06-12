package com.foodmap.common.logging;

import org.slf4j.MDC;

import java.util.regex.Pattern;

/**
 * 单次请求的日志上下文，负责校验或生成 requestId、traceId、spanId 并写入 MDC。
 *
 * @param requestId 单次 HTTP 请求流水号。
 * @param traceId 跨服务链路追踪号。
 * @param spanId 当前调用段 ID。
 * @param serviceName 当前服务名。
 * @param accountId 当前账号业务主键，可为空。
 * @param userId 当前用户业务主键，可为空。
 */
public record LogContext(
        String requestId,
        String traceId,
        String spanId,
        String serviceName,
        String accountId,
        String userId
) {
    private static final Pattern SAFE_ID_PATTERN = Pattern.compile("[A-Za-z0-9_.-]{1,128}");

    /**
     * 根据上游请求头和当前服务信息创建日志上下文。
     *
     * @param incomingRequestId 上游传入的请求流水号。
     * @param incomingTraceId 上游传入的链路追踪号。
     * @param serviceName 当前服务名。
     * @param accountId 当前账号业务主键，可为空。
     * @param userId 当前用户业务主键，可为空。
     * @return 已补齐安全 ID 的日志上下文。
     */
    public static LogContext fromIncoming(String incomingRequestId,
                                          String incomingTraceId,
                                          String serviceName,
                                          String accountId,
                                          String userId) {
        return new LogContext(
                safeOrGenerate(incomingRequestId),
                safeOrGenerate(incomingTraceId),
                TraceIdGenerator.nextId(),
                blankToNull(serviceName),
                blankToNull(accountId),
                blankToNull(userId)
        );
    }

    /**
     * 将日志上下文字段写入当前线程 MDC。
     */
    public void putToMdc() {
        put(LogMdcKeys.REQUEST_ID, requestId);
        put(LogMdcKeys.TRACE_ID, traceId);
        put(LogMdcKeys.SPAN_ID, spanId);
        put(LogMdcKeys.SERVICE_NAME, serviceName);
        put(LogMdcKeys.ACCOUNT_ID, accountId);
        put(LogMdcKeys.USER_ID, userId);
    }

    /**
     * 清理当前线程中的 FoodMap 日志上下文，避免线程复用导致串日志。
     */
    public static void clearMdc() {
        MDC.remove(LogMdcKeys.REQUEST_ID);
        MDC.remove(LogMdcKeys.TRACE_ID);
        MDC.remove(LogMdcKeys.SPAN_ID);
        MDC.remove(LogMdcKeys.SERVICE_NAME);
        MDC.remove(LogMdcKeys.ACCOUNT_ID);
        MDC.remove(LogMdcKeys.USER_ID);
    }

    /**
     * 返回安全上游 ID；上游未传或包含非法字符时生成新 ID。
     *
     * @param value 上游传入 ID。
     * @return 可安全写入日志和响应头的 ID。
     */
    private static String safeOrGenerate(String value) {
        String checked = blankToNull(value);
        if (checked == null || !SAFE_ID_PATTERN.matcher(checked).matches()) {
            return TraceIdGenerator.nextId();
        }
        return checked;
    }

    /**
     * 去除空白文本，空字符串统一转为 null。
     *
     * @param value 原始文本。
     * @return 去除首尾空格后的文本或 null。
     */
    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * 向 MDC 写入非空字段。
     *
     * @param key MDC 字段名。
     * @param value MDC 字段值。
     */
    private void put(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }
}
