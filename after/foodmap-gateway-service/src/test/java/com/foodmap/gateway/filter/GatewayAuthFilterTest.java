package com.foodmap.gateway.filter;

import com.foodmap.common.security.FoodMapAuthHeaders;
import com.foodmap.common.security.HmacTokenCodec;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayAuthFilterTest {
    private static final String TOKEN_SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void shouldStripSpoofedIdentityHeadersAndForwardOnlyUserIdForProtectedApi() {
        HmacTokenCodec tokenCodec = new HmacTokenCodec(TOKEN_SECRET);
        String token = tokenCodec.issueAccessToken(2001L, OffsetDateTime.now().plusHours(1));
        GatewayAuthFilter filter = new GatewayAuthFilter(TOKEN_SECRET);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(FoodMapAuthHeaders.ACCOUNT_ID, "8888")
                .header(FoodMapAuthHeaders.USER_ID, "9999")
                .build());
        AtomicReference<ServerWebExchange> downstreamExchange = new AtomicReference<>();

        filter.filter(exchange, chainExchange -> {
            downstreamExchange.set(chainExchange);
            return Mono.empty();
        }).block();

        assertThat(downstreamExchange.get()).isNotNull();
        assertThat(downstreamExchange.get().getRequest().getHeaders().getFirst(FoodMapAuthHeaders.ACCOUNT_ID))
                .isNull();
        assertThat(downstreamExchange.get().getRequest().getHeaders().getFirst(FoodMapAuthHeaders.USER_ID))
                .isEqualTo("2001");
    }

    @Test
    void shouldForwardOnlyUserIdForLegacyAccessTokenDuringMigration() {
        HmacTokenCodec tokenCodec = new HmacTokenCodec(TOKEN_SECRET);
        String token = tokenCodec.issueAccessToken(1001L, 2001L, OffsetDateTime.now().plusHours(1));
        GatewayAuthFilter filter = new GatewayAuthFilter(TOKEN_SECRET);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build());
        AtomicReference<ServerWebExchange> downstreamExchange = new AtomicReference<>();

        filter.filter(exchange, chainExchange -> {
            downstreamExchange.set(chainExchange);
            return Mono.empty();
        }).block();

        assertThat(downstreamExchange.get()).isNotNull();
        assertThat(downstreamExchange.get().getRequest().getHeaders().getFirst(FoodMapAuthHeaders.ACCOUNT_ID))
                .isNull();
        assertThat(downstreamExchange.get().getRequest().getHeaders().getFirst(FoodMapAuthHeaders.USER_ID))
                .isEqualTo("2001");
    }

    @Test
    void shouldRejectProtectedApiWithoutAccessToken() {
        GatewayAuthFilter filter = new GatewayAuthFilter(TOKEN_SECRET);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/me").build());
        AtomicReference<ServerWebExchange> downstreamExchange = new AtomicReference<>();

        filter.filter(exchange, chainExchange -> {
            downstreamExchange.set(chainExchange);
            return Mono.empty();
        }).block();

        assertThat(downstreamExchange.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRejectInternalProvisionPathThroughExternalGateway() {
        GatewayAuthFilter filter = new GatewayAuthFilter(TOKEN_SECRET);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/internal/users/provision").build());
        AtomicReference<ServerWebExchange> downstreamExchange = new AtomicReference<>();

        filter.filter(exchange, chainExchange -> {
            downstreamExchange.set(chainExchange);
            return Mono.empty();
        }).block();

        assertThat(downstreamExchange.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldAllowInternalHealthPathForLocalGatewayDiagnostics() {
        GatewayAuthFilter filter = new GatewayAuthFilter(TOKEN_SECRET);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/internal/auth/health").build());
        AtomicReference<ServerWebExchange> downstreamExchange = new AtomicReference<>();

        filter.filter(exchange, chainExchange -> {
            downstreamExchange.set(chainExchange);
            return Mono.empty();
        }).block();

        assertThat(downstreamExchange.get()).isNotNull();
    }
}
