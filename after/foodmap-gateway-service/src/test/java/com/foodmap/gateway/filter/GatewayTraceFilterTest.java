package com.foodmap.gateway.filter;

import com.foodmap.common.logging.TraceHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayTraceFilterTest {

    @Test
    void shouldGenerateTraceHeadersBeforeDownstreamFilters() {
        GatewayTraceFilter filter = new GatewayTraceFilter("foodmap-gateway-service");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/me").build());
        AtomicReference<ServerWebExchange> downstreamExchange = new AtomicReference<>();

        filter.filter(exchange, chainExchange -> {
            downstreamExchange.set(chainExchange);
            return Mono.empty();
        }).block();

        assertThat(downstreamExchange.get()).isNotNull();
        String requestId = downstreamExchange.get().getRequest().getHeaders().getFirst(TraceHeaders.REQUEST_ID);
        String traceId = downstreamExchange.get().getRequest().getHeaders().getFirst(TraceHeaders.TRACE_ID);
        assertThat(requestId).isNotBlank();
        assertThat(traceId).isNotBlank();
        assertThat(downstreamExchange.get().getRequest().getHeaders().getFirst(TraceHeaders.SPAN_ID)).isNotBlank();
        assertThat(exchange.getResponse().getHeaders().getFirst(TraceHeaders.REQUEST_ID)).isEqualTo(requestId);
        assertThat(exchange.getResponse().getHeaders().getFirst(TraceHeaders.TRACE_ID)).isEqualTo(traceId);
    }

    @Test
    void shouldPreserveSafeIncomingTraceHeaders() {
        GatewayTraceFilter filter = new GatewayTraceFilter("foodmap-gateway-service");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/auth/login")
                .header(TraceHeaders.REQUEST_ID, "req-from-client")
                .header(TraceHeaders.TRACE_ID, "trace-from-client")
                .build());
        AtomicReference<ServerWebExchange> downstreamExchange = new AtomicReference<>();

        filter.filter(exchange, chainExchange -> {
            downstreamExchange.set(chainExchange);
            return Mono.empty();
        }).block();

        assertThat(downstreamExchange.get()).isNotNull();
        assertThat(downstreamExchange.get().getRequest().getHeaders().getFirst(TraceHeaders.REQUEST_ID))
                .isEqualTo("req-from-client");
        assertThat(downstreamExchange.get().getRequest().getHeaders().getFirst(TraceHeaders.TRACE_ID))
                .isEqualTo("trace-from-client");
        assertThat(exchange.getResponse().getHeaders().getFirst(TraceHeaders.REQUEST_ID)).isEqualTo("req-from-client");
        assertThat(exchange.getResponse().getHeaders().getFirst(TraceHeaders.TRACE_ID)).isEqualTo("trace-from-client");
    }
}
