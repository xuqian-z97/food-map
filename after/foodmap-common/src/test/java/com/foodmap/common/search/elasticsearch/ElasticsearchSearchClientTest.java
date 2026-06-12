package com.foodmap.common.search.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodmap.common.search.SearchHit;
import com.foodmap.common.search.SearchRequest;
import com.foodmap.common.search.SearchResponse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElasticsearchSearchClientTest {

    @Test
    void shouldPostSearchBodyAndParseHits() {
        CapturingHttpClient httpClient = new CapturingHttpClient("""
                {
                  "hits": {
                    "hits": [
                      {
                        "_source": {
                          "traceId": "trace-1",
                          "message": "first log"
                        },
                        "sort": ["2026-06-05T00:00:01Z", 1001]
                      }
                    ]
                  }
                }
                """);
        ElasticsearchSearchProperties properties = new ElasticsearchSearchProperties();
        properties.setBaseUrl("http://elasticsearch.test/");
        properties.setApiKey("test-api-key");
        ElasticsearchSearchClient client = new ElasticsearchSearchClient(properties, new ObjectMapper(), httpClient);

        SearchResponse response = client.search(new SearchRequest(
                "foodmap-logs-*",
                Map.of(
                        "size", 1,
                        "query", Map.of("match_all", Map.of())
                )
        ));

        assertEquals("/foodmap-logs-*/_search", httpClient.requests.getFirst().uri().getPath());
        assertTrue(httpClient.requestBodies.getFirst().contains("\"size\":1"));
        assertEquals("ApiKey test-api-key", httpClient.requests.getFirst().headers().firstValue("Authorization").orElseThrow());
        assertEquals(1, response.hits().size());
        SearchHit hit = response.hits().getFirst();
        assertEquals("trace-1", hit.source().path("traceId").asText());
        assertEquals(List.of("2026-06-05T00:00:01Z", 1001L), hit.sortValues());
    }

    private static class CapturingHttpClient extends HttpClient {
        private final String responseBody;
        private final List<HttpRequest> requests = new ArrayList<>();
        private final List<String> requestBodies = new ArrayList<>();

        private CapturingHttpClient(String responseBody) {
            this.responseBody = responseBody;
        }

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
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
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
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
            requests.add(request);
            requestBodies.add(readBody(request));
            @SuppressWarnings("unchecked")
            T body = (T) responseBody;
            return new CapturingHttpResponse<>(request, body);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException("async send is not used in this test");
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler
        ) {
            throw new UnsupportedOperationException("async send is not used in this test");
        }

        private String readBody(HttpRequest request) throws IOException, InterruptedException {
            Optional<HttpRequest.BodyPublisher> publisher = request.bodyPublisher();
            if (publisher.isEmpty()) {
                return "";
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            CountDownLatch latch = new CountDownLatch(1);
            publisher.get().subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(ByteBuffer item) {
                    byte[] bytes = new byte[item.remaining()];
                    item.get(bytes);
                    output.writeBytes(bytes);
                }

                @Override
                public void onError(Throwable throwable) {
                    latch.countDown();
                }

                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IOException("request body was not published");
            }
            return output.toString(StandardCharsets.UTF_8);
        }
    }

    private record CapturingHttpResponse<T>(HttpRequest request, T body) implements HttpResponse<T> {
        @Override
        public int statusCode() {
            return 200;
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (name, value) -> true);
        }

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }
    }
}
