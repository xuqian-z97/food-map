package com.foodmap.admin.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodmap.admin.dto.AdminApiAccessLogQueryRequest;
import com.foodmap.common.logging.LogMdcKeys;
import com.foodmap.common.logging.TraceHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

class HttpLogApiAccessLogClientTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldCallLogServiceWithQueryParamsAndTraceHeaders() {
        CapturingHttpClient httpClient = new CapturingHttpClient();
        AdminLogClientProperties properties = new AdminLogClientProperties();
        properties.setBaseUrl("http://log-service:8089");
        HttpLogApiAccessLogClient client = new HttpLogApiAccessLogClient(
                httpClient,
                new ObjectMapper().findAndRegisterModules(),
                properties
        );
        MDC.put(LogMdcKeys.REQUEST_ID, "req-admin");
        MDC.put(LogMdcKeys.TRACE_ID, "trace-admin");
        MDC.put(LogMdcKeys.SPAN_ID, "span-admin");

        var response = client.search(new AdminApiAccessLogQueryRequest(
                "req-admin",
                null,
                "foodmap-auth-service",
                "INFO",
                200,
                OffsetDateTime.parse("2026-06-13T00:00:00+08:00"),
                null,
                0,
                20
        ));

        assertThat(httpClient.request.method()).isEqualTo("GET");
        assertThat(httpClient.request.uri().getPath()).isEqualTo("/internal/logs/api-access");
        assertThat(httpClient.request.uri().getRawQuery()).contains("requestId=req-admin");
        assertThat(httpClient.request.uri().getRawQuery()).contains("serviceName=foodmap-auth-service");
        assertThat(httpClient.request.uri().getRawQuery()).contains("occurredFrom=2026-06-13T00%3A00%2B08%3A00");
        assertThat(httpClient.request.headers().firstValue(TraceHeaders.REQUEST_ID)).contains("req-admin");
        assertThat(httpClient.request.headers().firstValue(TraceHeaders.TRACE_ID)).contains("trace-admin");
        assertThat(httpClient.request.headers().firstValue(TraceHeaders.SPAN_ID)).contains("span-admin");
        assertThat(response.total()).isEqualTo(1L);
        assertThat(response.items().getFirst().requestId()).isEqualTo("req-admin");
    }

    private static class CapturingHttpClient extends HttpClient {
        private HttpRequest request;

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public javax.net.ssl.SSLContext sslContext() {
            return null;
        }

        @Override
        public javax.net.ssl.SSLParameters sslParameters() {
            return null;
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @Override
        public <T> HttpResponse<T> send(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler
        ) throws IOException, InterruptedException {
            this.request = request;
            return new OkResponse<>(request, """
                    {
                      "success": true,
                      "status": 200,
                      "code": "OK",
                      "message": "success",
                      "data": {
                        "items": [
                          {
                            "accessLogId": 1,
                            "requestId": "req-admin",
                            "traceId": "trace-admin",
                            "spanId": "span-admin",
                            "serviceName": "foodmap-auth-service",
                            "eventName": "api.access.completed",
                            "logLevel": "INFO",
                            "httpMethod": "POST",
                            "requestPath": "/api/auth/login",
                            "httpStatus": 200,
                            "durationMs": 32,
                            "clientIpMasked": "192.168.*.*",
                            "accountId": 11,
                            "userId": 22,
                            "gatewayRouteId": "auth-service",
                            "errorCode": null,
                            "occurredTime": "2026-06-13T01:00:00+08:00"
                          }
                        ],
                        "total": 1,
                        "pageIndex": 0,
                        "pageSize": 20,
                        "hasMore": false
                      }
                    }
                    """);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler
        ) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler
        ) {
            throw new UnsupportedOperationException("not used");
        }
    }

    private record OkResponse<T>(HttpRequest request, String bodyText) implements HttpResponse<T> {
        @Override
        public int statusCode() {
            return 200;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T body() {
            return (T) bodyText;
        }

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(java.util.Map.of(), (name, value) -> true);
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }

        @Override
        public Optional<javax.net.ssl.SSLSession> sslSession() {
            return Optional.empty();
        }
    }
}
