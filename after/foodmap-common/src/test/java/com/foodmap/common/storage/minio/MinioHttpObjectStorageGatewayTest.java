package com.foodmap.common.storage.minio;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

class MinioHttpObjectStorageGatewayTest {

    @Test
    void shouldPutObjectWithS3CompatibleHeaders() throws Exception {
        CapturingHttpClient httpClient = new CapturingHttpClient();
        MinioObjectStorageProperties properties = new MinioObjectStorageProperties();
        properties.setEndpoint("http://127.0.0.1:9000");
        properties.setAccessKey("foodmap");
        properties.setSecretKey("foodmap-secret");
        MinioHttpObjectStorageGateway gateway = new MinioHttpObjectStorageGateway(httpClient, properties);
        byte[] body = "archive-payload".getBytes(StandardCharsets.UTF_8);

        gateway.putObject(
                "foodmap-log-archive",
                "logs/full/a b.jsonl.gz",
                "application/x-ndjson+gzip",
                body.length,
                new ByteArrayInputStream(body)
        );

        HttpRequest captured = httpClient.request;
        assertThat(captured.method()).isEqualTo("PUT");
        assertThat(captured.uri().getRawPath()).isEqualTo("/foodmap-log-archive/logs/full/a%20b.jsonl.gz");
        assertThat(captured.headers().firstValue("Authorization").orElse(""))
                .startsWith("AWS4-HMAC-SHA256 Credential=foodmap/")
                .contains("SignedHeaders=content-length;content-type;host;x-amz-content-sha256;x-amz-date");
        assertThat(captured.headers().firstValue("X-Amz-Date")).isPresent();
        assertThat(captured.headers().firstValue("Content-Type")).contains("application/x-ndjson+gzip");
        assertThat(captured.bodyPublisher()).isPresent();
        assertThat(captured.bodyPublisher().orElseThrow().contentLength()).isEqualTo(body.length);
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
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
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
            return new OkResponse<>(request);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler
        ) {
            this.request = request;
            return CompletableFuture.completedFuture(new OkResponse<>(request));
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler
        ) {
            this.request = request;
            return CompletableFuture.completedFuture(new OkResponse<>(request));
        }
    }

    private record OkResponse<T>(HttpRequest request) implements HttpResponse<T> {
        @Override
        public int statusCode() {
            return 200;
        }

        @Override
        public T body() {
            return null;
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
