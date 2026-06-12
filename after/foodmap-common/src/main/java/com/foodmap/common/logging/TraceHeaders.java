package com.foodmap.common.logging;

/**
 * FoodMap 链路追踪 HTTP 请求头常量，由网关生成并在内部服务之间透传。
 */
public final class TraceHeaders {
    /**
     * 单次 HTTP 请求流水号请求头。
     */
    public static final String REQUEST_ID = "X-Request-Id";
    /**
     * FoodMap 内部链路追踪号请求头。
     */
    public static final String TRACE_ID = "X-Trace-Id";
    /**
     * FoodMap 内部调用段请求头。
     */
    public static final String SPAN_ID = "X-Span-Id";

    private TraceHeaders() {
    }
}
