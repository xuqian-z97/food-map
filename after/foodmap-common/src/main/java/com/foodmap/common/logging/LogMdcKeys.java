package com.foodmap.common.logging;

/**
 * FoodMap 日志 MDC 字段名常量，统一 requestId、traceId、logType 和用户身份在线程上下文中的命名。
 *
 * <p>{@link #LOG_TYPE} 用于日志分类和采集路由，取值来自本类中的 {@code LOG_TYPE_*} 常量；
 * 任何按 message 文本猜测日志类型的做法都是错误的，事件名调整后会静默失效。</p>
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
    /**
     * 日志类型，供 JSON 日志字段和 Fluent Bit 采集路由使用，默认 {@link #LOG_TYPE_APPLICATION}。
     *
     * <p>该字段必须写入 MDC，不能依赖 message 文本区分日志类型。</p>
     */
    public static final String LOG_TYPE = "logType";
    /**
     * 普通业务日志和框架日志，未显式声明日志类型时的默认值，路由到 application 日志主题。
     */
    public static final String LOG_TYPE_APPLICATION = "application";
    /**
     * 接口访问摘要日志，由 Servlet 访问日志过滤器输出，路由到 api-access 日志主题。
     */
    public static final String LOG_TYPE_API_ACCESS = "api-access";
    /**
     * SQL 明细、慢 SQL 和异常 SQL 日志，由 MyBatis 日志拦截器输出，路由到 sql 日志主题。
     */
    public static final String LOG_TYPE_SQL = "sql";
    /**
     * 审计日志，记录关键业务动作事实和业务主键，路由到 audit 日志主题。
     */
    public static final String LOG_TYPE_AUDIT = "audit";
    /**
     * 安全日志，记录登录失败、权限拒绝等安全事件，路由到 security 日志主题。
     */
    public static final String LOG_TYPE_SECURITY = "security";

    private LogMdcKeys() {
    }
}
