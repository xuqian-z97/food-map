package com.foodmap.common.logging;

import com.foodmap.common.security.FoodMapAuthHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet 请求日志上下文过滤器，负责生成、校验并透传 FoodMap 标准链路 ID。
 *
 * <p>该过滤器必须位于业务访问日志和 Controller 之前，确保后续 {@link SafeLog} 输出自动携带
 * requestId、traceId、serviceName 和可信用户身份。排查串日志时应优先确认本过滤器是否在 finally 中清理 MDC。</p>
 */
public class LogMdcFilter extends OncePerRequestFilter {
    private final String serviceName;

    /**
     * 创建日志上下文过滤器。
     *
     * @param serviceName 当前服务名，通常来自 {@code spring.application.name}。
     */
    public LogMdcFilter(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * 为当前 Servlet 请求初始化日志上下文，写入响应头后继续执行过滤器链。
     *
     * @param request 当前 HTTP 请求。
     * @param response 当前 HTTP 响应。
     * @param filterChain 后续过滤器链。
     * @throws ServletException Servlet 过滤器异常。
     * @throws IOException I/O 异常。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        LogContext context = LogContext.fromIncoming(
                request.getHeader(TraceHeaders.REQUEST_ID),
                request.getHeader(TraceHeaders.TRACE_ID),
                serviceName,
                request.getHeader(FoodMapAuthHeaders.ACCOUNT_ID),
                request.getHeader(FoodMapAuthHeaders.USER_ID)
        );
        context.putToMdc();
        response.setHeader(TraceHeaders.REQUEST_ID, context.requestId());
        response.setHeader(TraceHeaders.TRACE_ID, context.traceId());
        response.setHeader(TraceHeaders.SPAN_ID, context.spanId());
        try {
            filterChain.doFilter(request, response);
        } finally {
            LogContext.clearMdc();
        }
    }
}
