package com.foodmap.common.logging;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class LogMdcFilterTest {

    @Test
    void shouldPutTraceContextIntoMdcAndResponseHeaders() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/auth/health");
        request.addHeader(TraceHeaders.REQUEST_ID, "req-b15-a");
        request.addHeader(TraceHeaders.TRACE_ID, "trace-b15-a");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInChain = new AtomicReference<>();
        AtomicReference<String> traceIdInChain = new AtomicReference<>();
        AtomicReference<String> serviceNameInChain = new AtomicReference<>();
        jakarta.servlet.FilterChain chain = (chainRequest, chainResponse) -> {
            requestIdInChain.set(MDC.get(LogMdcKeys.REQUEST_ID));
            traceIdInChain.set(MDC.get(LogMdcKeys.TRACE_ID));
            serviceNameInChain.set(MDC.get(LogMdcKeys.SERVICE_NAME));
        };

        new LogMdcFilter("foodmap-auth-service").doFilter(request, response, chain);

        assertThat(requestIdInChain.get()).isEqualTo("req-b15-a");
        assertThat(traceIdInChain.get()).isEqualTo("trace-b15-a");
        assertThat(serviceNameInChain.get()).isEqualTo("foodmap-auth-service");
        assertThat(response.getHeader(TraceHeaders.REQUEST_ID)).isEqualTo("req-b15-a");
        assertThat(response.getHeader(TraceHeaders.TRACE_ID)).isEqualTo("trace-b15-a");
        assertThat(MDC.get(LogMdcKeys.REQUEST_ID)).isNull();
        assertThat(MDC.get(LogMdcKeys.TRACE_ID)).isNull();
    }

    @Test
    void shouldGenerateSafeTraceContextWhenIncomingHeadersAreUnsafe() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader(TraceHeaders.REQUEST_ID, "bad request id with spaces");
        request.addHeader(TraceHeaders.TRACE_ID, "bad\ntrace");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInChain = new AtomicReference<>();
        AtomicReference<String> traceIdInChain = new AtomicReference<>();
        jakarta.servlet.FilterChain chain = (chainRequest, chainResponse) -> {
            requestIdInChain.set(MDC.get(LogMdcKeys.REQUEST_ID));
            traceIdInChain.set(MDC.get(LogMdcKeys.TRACE_ID));
        };

        new LogMdcFilter("foodmap-auth-service").doFilter(request, response, chain);

        assertThat(requestIdInChain.get()).isNotBlank().isNotEqualTo("bad request id with spaces");
        assertThat(traceIdInChain.get()).isNotBlank().isNotEqualTo("bad\ntrace");
        assertThat(response.getHeader(TraceHeaders.REQUEST_ID)).isEqualTo(requestIdInChain.get());
        assertThat(response.getHeader(TraceHeaders.TRACE_ID)).isEqualTo(traceIdInChain.get());
    }
}
