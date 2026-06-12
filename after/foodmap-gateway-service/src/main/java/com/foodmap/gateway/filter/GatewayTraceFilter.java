package com.foodmap.gateway.filter;

import com.foodmap.common.logging.LogContext;
import com.foodmap.common.logging.LogField;
import com.foodmap.common.logging.SafeLog;
import com.foodmap.common.logging.TraceHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关链路追踪过滤器，负责生成、校验并向下游服务透传 requestId、traceId 和 spanId。
 *
 * <p>该过滤器顺序早于认证过滤器，保证认证失败、限流失败和下游异常都能通过响应头返回同一 requestId。
 * 排查一次接口调用时，应优先使用 response 中的 requestId 或 traceId 检索网关与业务服务日志。</p>
 */
@Component
public class GatewayTraceFilter implements GlobalFilter, Ordered {
    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayTraceFilter.class);
    private static final int TRACE_FILTER_ORDER = -200;

    private final String serviceName;

    /**
     * 创建网关链路追踪过滤器。
     *
     * @param serviceName 当前网关服务名。
     */
    public GatewayTraceFilter(
            @Value("${spring.application.name:foodmap-gateway-service}") String serviceName
    ) {
        this.serviceName = serviceName;
    }

    /**
     * 为外部请求补齐链路头并透传给后续网关过滤器和业务服务。
     *
     * @param exchange 当前网关请求上下文。
     * @param chain 后续网关过滤器链。
     * @return 当前请求的响应完成信号。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        LogContext context = LogContext.fromIncoming(
                exchange.getRequest().getHeaders().getFirst(TraceHeaders.REQUEST_ID),
                exchange.getRequest().getHeaders().getFirst(TraceHeaders.TRACE_ID),
                serviceName,
                null,
                null
        );
        ServerHttpRequest requestWithTrace = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.set(TraceHeaders.REQUEST_ID, context.requestId());
                    headers.set(TraceHeaders.TRACE_ID, context.traceId());
                    headers.set(TraceHeaders.SPAN_ID, context.spanId());
                })
                .build();
        ServerWebExchange tracedExchange = exchange.mutate().request(requestWithTrace).build();
        tracedExchange.getResponse().getHeaders().set(TraceHeaders.REQUEST_ID, context.requestId());
        tracedExchange.getResponse().getHeaders().set(TraceHeaders.TRACE_ID, context.traceId());
        tracedExchange.getResponse().getHeaders().set(TraceHeaders.SPAN_ID, context.spanId());

        long startedAt = System.nanoTime();
        context.putToMdc();
        SafeLog.info(LOGGER, "gateway.request.received",
                LogField.of("method", exchange.getRequest().getMethod().name()),
                LogField.of("path", exchange.getRequest().getPath().pathWithinApplication().value()));
        LogContext.clearMdc();

        return chain.filter(tracedExchange)
                .doOnError(throwable -> writeGatewayAccessLog(tracedExchange, context, startedAt, throwable))
                .doOnSuccess(ignored -> writeGatewayAccessLog(tracedExchange, context, startedAt, null));
    }

    /**
     * 返回过滤器顺序，必须早于 {@link GatewayAuthFilter}。
     *
     * @return 网关过滤器执行顺序。
     */
    @Override
    public int getOrder() {
        return TRACE_FILTER_ORDER;
    }

    /**
     * 输出网关访问摘要日志，响应完成时短暂写入 MDC 以复用 SafeLog 的上下文字段格式。
     *
     * @param exchange 当前网关请求上下文。
     * @param context 当前请求日志上下文。
     * @param startedAt 请求开始纳秒时间。
     * @param throwable 请求异常，可为空。
     */
    private void writeGatewayAccessLog(ServerWebExchange exchange,
                                       LogContext context,
                                       long startedAt,
                                       Throwable throwable) {
        context.putToMdc();
        try {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
            Integer status = exchange.getResponse().getStatusCode() == null
                    ? null
                    : exchange.getResponse().getStatusCode().value();
            LogField[] fields = new LogField[]{
                    LogField.of("method", exchange.getRequest().getMethod().name()),
                    LogField.of("path", exchange.getRequest().getPath().pathWithinApplication().value()),
                    LogField.of("status", status),
                    LogField.of("durationMs", durationMs)
            };
            if (throwable != null || (status != null && status >= 500)) {
                SafeLog.warn(LOGGER, "gateway.request.failed", fields);
                return;
            }
            SafeLog.info(LOGGER, "gateway.request.completed", fields);
        } finally {
            LogContext.clearMdc();
        }
    }
}
