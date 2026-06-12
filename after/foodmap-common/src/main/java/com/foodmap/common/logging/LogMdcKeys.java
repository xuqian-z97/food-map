package com.foodmap.common.logging;

/**
 * FoodMap 日志 MDC 字段名常量，统一 requestId、traceId 和用户身份在线程上下文中的命名。
 */
public final class LogMdcKeys {
    /**
     * 单次 HTTP 请求流水号，用于查询一次接口调用内的日志。
     */
    public static final String REQUEST_ID = "requestId";
    /**
     * 跨网关、微服务和消息的链路追踪号。
     */
    public static final String TRACE_ID = "traceId";
    /**
     * 链路中单个调用段 ID。
     */
    public static final String SPAN_ID = "spanId";
    /**
     * 当前产生日志的服务名。
     */
    public static final String SERVICE_NAME = "serviceName";
    /**
     * 当前登录账号业务主键。
     */
    public static final String ACCOUNT_ID = "accountId";
    /**
     * 当前登录用户业务主键。
     */
    public static final String USER_ID = "userId";

    private LogMdcKeys() {
    }
}
