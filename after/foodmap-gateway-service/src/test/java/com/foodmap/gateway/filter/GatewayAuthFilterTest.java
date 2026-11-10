package com.foodmap.gateway.filter;

import com.foodmap.common.security.AccessTokenDenylistClient;
import com.foodmap.common.security.FoodMapAuthHeaders;
import com.foodmap.common.security.HmacTokenCodec;
import com.foodmap.gateway.security.GatewayAdminTokenVerifier;
import com.foodmap.gateway.security.TestAdminTokens;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关认证过滤器测试：App Token 链路回归、`X-Admin-*` 剥头与后台独立验签分支。
 */
@SuppressWarnings("deprecation")
class GatewayAuthFilterTest {
    private static final String TOKEN_SECRET = "0123456789abcdef0123456789abcdef";

    /**
     * 后台测试密钥与验签器，按两端共享契约独立签发。
     */
    private final RSAKey adminKeyPair = TestAdminTokens.generateKey();
    private final GatewayAdminTokenVerifier adminVerifier = TestAdminTokens.verifierFor(adminKeyPair);

    @Test
    void shouldStripSpoofedIdentityHeadersAndForwardOnlyUserIdForProtectedApi() {
        HmacTokenCodec tokenCodec = new HmacTokenCodec(TOKEN_SECRET);
        String token = tokenCodec.issueAccessToken(2001L, OffsetDateTime.now().plusHours(1));
        GatewayAuthFilter filter = new GatewayAuthFilter(TOKEN_SECRET, adminVerifier);
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
        GatewayAuthFilter filter = new GatewayAuthFilter(TOKEN_SECRET, adminVerifier);
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
        GatewayAuthFilter filter = new GatewayAuthFilter(TOKEN_SECRET, adminVerifier);
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
    void shouldRejectProtectedApiWhenAccessTokenIsDenylisted() {
        HmacTokenCodec tokenCodec = new HmacTokenCodec(TOKEN_SECRET);
        String token = tokenCodec.issueAccessToken(2001L, OffsetDateTime.now().plusHours(1));
        RecordingAccessTokenDenylist recordingDenylist = new RecordingAccessTokenDenylist(tokenCodec.tokenHash(token));
        GatewayAuthFilter filter = new GatewayAuthFilter(TOKEN_SECRET, recordingDenylist, adminVerifier);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build());
        AtomicReference<ServerWebExchange> downstreamExchange = new AtomicReference<>();

        filter.filter(exchange, chainExchange -> {
            downstreamExchange.set(chainExchange);
            return Mono.empty();
        }).block();

        assertThat(recordingDenylist.queriedHash()).isEqualTo(tokenCodec.tokenHash(token));
        assertThat(downstreamExchange.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {".", "..", ".appended"})
    void shouldNotBypassDenylistWithAppendedTokenSegments(String suffix) {
        HmacTokenCodec codec = new HmacTokenCodec(TOKEN_SECRET);
        String token = codec.issueAccessToken(2001L, OffsetDateTime.now().plusHours(1));
        RecordingAccessTokenDenylist denylist = new RecordingAccessTokenDenylist(codec.tokenHash(token));
        GatewayAuthFilter filter = new GatewayAuthFilter(TOKEN_SECRET, denylist, adminVerifier);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token + suffix).build());
        AtomicReference<ServerWebExchange> downstream = new AtomicReference<>();

        filter.filter(exchange, next -> {
            downstream.set(next);
            return Mono.empty();
        }).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(downstream.get()).isNull();
        assertThat(denylist.queriedHash()).as("结构非法应在查询 denylist 之前拒绝").isNull();
    }

    @Test
    void shouldRejectInternalProvisionPathThroughExternalGateway() {
        GatewayAuthFilter filter = new GatewayAuthFilter(TOKEN_SECRET, adminVerifier);
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
        GatewayAuthFilter filter = new GatewayAuthFilter(TOKEN_SECRET, adminVerifier);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/internal/auth/health").build());
        AtomicReference<ServerWebExchange> downstreamExchange = new AtomicReference<>();

        filter.filter(exchange, chainExchange -> {
            downstreamExchange.set(chainExchange);
            return Mono.empty();
        }).block();

        assertThat(downstreamExchange.get()).isNotNull();
    }

    @Test
    void shouldStripAdminHeadersWithCaseVariantsAndMultiValues() {
        GatewayAuthFilter filter = new GatewayAuthFilter(TOKEN_SECRET, adminVerifier);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/auth/login")
                .header("X-Admin-User-Id", "8888")
                .header("x-admin-permissions", "LOG_ACCESS_READ")
                .header("X-ADMIN-CUSTOM", "a")
                .header("X-Admin-Multi", "1")
                .header("X-Admin-Multi", "2")
                .header("X-Adminish-Similar", "keep")
                .build());
        AtomicReference<ServerWebExchange> downstreamExchange = new AtomicReference<>();

        filter.filter(exchange, chainExchange -> {
            downstreamExchange.set(chainExchange);
            return Mono.empty();
        }).block();

        HttpHeaders downstreamHeaders = downstreamExchange.get().getRequest().getHeaders();
        assertThat(downstreamHeaders.keySet())
                .noneMatch(name -> name.toLowerCase(java.util.Locale.ROOT).startsWith("x-admin-"));
        assertThat(downstreamHeaders.getFirst("X-Adminish-Similar")).isEqualTo("keep");
    }

    @Test
    void shouldAllowExactAdminLoginPostWithoutToken() {
        GatewayAuthFilter filter = new GatewayAuthFilter(TOKEN_SECRET, adminVerifier);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/admin/auth/login").build());
        AtomicReference<ServerWebExchange> downstreamExchange = new AtomicReference<>();

        filter.filter(exchange, chainExchange -> {
            downstreamExchange.set(chainExchange);
            return Mono.empty();
        }).block();

        assertThat(downstreamExchange.get()).isNotNull();
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRejectAdminLoginWithNonPostMethod() {
        GatewayAuthFilter filter = new GatewayAuthFilter(TOKEN_SECRET, adminVerifier);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/admin/auth/login").build());

        filter.filter(exchange, chainExchange -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldForwardAdminRequestWithValidAdminToken() {
        String adminToken = TestAdminTokens.issueToken(adminKeyPair, 100001L);
        GatewayAuthFilter filter = new GatewayAuthFilter(TOKEN_SECRET, adminVerifier);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/admin/logs/api-access")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .header("X-Admin-Permissions", "LOG_ACCESS_READ")
                .build());
        AtomicReference<ServerWebExchange> downstreamExchange = new AtomicReference<>();

        filter.filter(exchange, chainExchange -> {
            downstreamExchange.set(chainExchange);
            return Mono.empty();
        }).block();

        assertThat(downstreamExchange.get()).isNotNull();
        HttpHeaders downstreamHeaders = downstreamExchange.get().getRequest().getHeaders();
        assertThat(downstreamHeaders.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + adminToken);
        assertThat(downstreamHeaders.keySet())
                .noneMatch(name -> name.toLowerCase(java.util.Locale.ROOT).startsWith("x-admin-"));
        assertThat(downstreamHeaders.getFirst(FoodMapAuthHeaders.USER_ID)).as("后台分支不得签发 App 用户身份头").isNull();
    }

    @Test
    void shouldRejectAdminPathWithoutTokenOrWithDuplicatedCredential() {
        GatewayAuthFilter filter = new GatewayAuthFilter(TOKEN_SECRET, adminVerifier);

        MockServerWebExchange noToken = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/admin/logs/api-access").build());
        filter.filter(noToken, chain -> Mono.empty()).block();
        assertThat(noToken.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        String adminToken = TestAdminTokens.issueToken(adminKeyPair, 100001L);
        MockServerWebExchange duplicated = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/admin/logs/api-access")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .build());
        filter.filter(duplicated, chain -> Mono.empty()).block();
        assertThat(duplicated.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRejectAdminPathWhenBearerCredentialIsAppAccessToken() {
        HmacTokenCodec tokenCodec = new HmacTokenCodec(TOKEN_SECRET);
        String appToken = tokenCodec.issueAccessToken(2001L, OffsetDateTime.now().plusHours(1));
        GatewayAuthFilter filter = new GatewayAuthFilter(TOKEN_SECRET, adminVerifier);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/admin/logs/api-access")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + appToken)
                .build());

        filter.filter(exchange, chain -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRejectAppPathWhenBearerCredentialIsAdminAccessToken() {
        String adminToken = TestAdminTokens.issueToken(adminKeyPair, 100001L);
        GatewayAuthFilter filter = new GatewayAuthFilter(TOKEN_SECRET, adminVerifier);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .build());

        filter.filter(exchange, chain -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private static final class RecordingAccessTokenDenylist implements AccessTokenDenylistClient {
        private final String deniedHash;
        private String queriedHash;

        private RecordingAccessTokenDenylist(String deniedHash) {
            this.deniedHash = deniedHash;
        }

        @Override
        public void deny(String accessTokenHash, OffsetDateTime expiresTime) {
            // Gateway only reads the denylist.
        }

        @Override
        public boolean contains(String accessTokenHash) {
            queriedHash = accessTokenHash;
            return deniedHash.equals(queriedHash);
        }

        private String queriedHash() {
            return queriedHash;
        }
    }
}
