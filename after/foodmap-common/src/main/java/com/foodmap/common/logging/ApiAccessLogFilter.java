package com.foodmap.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * API 访问摘要日志过滤器，记录每次接口调用的结构化摘要。
 *
 * <p>本过滤器只记录方法、路径、状态码、耗时和客户端地址等定位字段，不读取请求体，不输出 Token、密码或私密内容。
 * 慢请求和异常请求使用 WARN，普通请求使用 INFO，后续 B1.5-b 会将该摘要落入独立日志 PostgreSQL。</p>
 */
public class ApiAccessLogFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiAccessLogFilter.class);

    private final long slowThresholdMs;

    /**
     * 创建 API 访问日志过滤器。
     *
     * @param slowThresholdMs 慢请求阈值，单位毫秒。
     */
    public ApiAccessLogFilter(long slowThresholdMs) {
        this.slowThresholdMs = slowThresholdMs;
    }

    /**
     * 记录当前 HTTP 请求的访问摘要。
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
        long startedAt = System.nanoTime();
        Throwable failure = null;
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException ex) {
            failure = ex;
            throw ex;
        } finally {
            writeAccessLog(request, response, startedAt, failure);
        }
    }

    /**
     * 根据状态码、耗时和异常状态选择日志等级并输出结构化字段。
     *
     * @param request 当前 HTTP 请求。
     * @param response 当前 HTTP 响应。
     * @param startedAt 请求开始纳秒时间。
     * @param failure 请求链路异常，可为空。
     */
    private void writeAccessLog(HttpServletRequest request,
                                HttpServletResponse response,
                                long startedAt,
                                Throwable failure) {
        long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
        int status = response.getStatus();
        LogField[] fields = new LogField[]{
                LogField.of("method", request.getMethod()),
                LogField.of("path", request.getRequestURI()),
                LogField.of("status", status),
                LogField.of("durationMs", durationMs),
                LogField.of("clientIp", resolveClientIp(request))
        };
        if (failure != null || status >= 500 || durationMs >= slowThresholdMs) {
            SafeLog.warn(LOGGER, "api.access.slow", fields);
            return;
        }
        SafeLog.info(LOGGER, "api.access.completed", fields);
    }

    /**
     * 解析客户端 IP 摘要，优先使用网关或代理传入的 X-Forwarded-For 首个地址。
     *
     * @param request 当前 HTTP 请求。
     * @return 客户端 IP 摘要。
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int commaIndex = forwardedFor.indexOf(',');
            return commaIndex > -1 ? forwardedFor.substring(0, commaIndex).trim() : forwardedFor.trim();
        }
        return request.getRemoteAddr();
    }
}
