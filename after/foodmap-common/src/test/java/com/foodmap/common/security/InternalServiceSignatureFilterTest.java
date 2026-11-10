package com.foodmap.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.StreamUtils;

import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class InternalServiceSignatureFilterTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final String AUTH_SERVICE = "foodmap-auth-service";
    private static final byte[] BODY = "{\"userId\":1}".getBytes(StandardCharsets.UTF_8);

    @Test
    void shouldPassValidRequestAndReplayBody() throws Exception {
        MockHttpServletRequest request = request("/internal/users/provision");
        long timestamp = System.currentTimeMillis();
        String nonce = "filter-nonce-1";
        addSignatureHeaders(request, AUTH_SERVICE, timestamp, nonce, BODY);
        request.setContent(BODY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<byte[]> downstreamBody = new AtomicReference<>();
        FilterChain chain = (req, res) -> downstreamBody.set(StreamUtils.copyToByteArray(req.getInputStream()));

        filter().doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(downstreamBody.get()).isEqualTo(BODY);
    }

    @Test
    void shouldRejectRequestWithoutSignature() throws Exception {
        MockHttpServletRequest request = request("/internal/users/provision");
        request.setContent(BODY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Boolean> downstreamCalled = new AtomicReference<>(false);
        FilterChain chain = (req, res) -> downstreamCalled.set(true);

        filter().doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
        assertThat(downstreamCalled.get()).isFalse();
    }

    @Test
    void shouldSkipHealthPath() throws Exception {
        MockHttpServletRequest request = request("/internal/auth/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Boolean> downstreamCalled = new AtomicReference<>(false);
        FilterChain chain = (req, res) -> downstreamCalled.set(true);

        filter().doFilter(request, response, chain);

        assertThat(downstreamCalled.get()).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private static MockHttpServletRequest request(String path) {
        return new MockHttpServletRequest("POST", path);
    }

    private static void addSignatureHeaders(
            MockHttpServletRequest request,
            String service,
            long timestamp,
            String nonce,
            byte[] body
    ) {
        request.addHeader(InternalServiceHeaders.SERVICE, service);
        request.addHeader(InternalServiceHeaders.TIMESTAMP, Long.toString(timestamp));
        request.addHeader(InternalServiceHeaders.NONCE, nonce);
        request.addHeader(InternalServiceHeaders.SIGNATURE, InternalServiceSignature.sign(
                SECRET, service, timestamp, nonce, "POST", request.getRequestURI(), body));
    }

    private static InternalServiceSignatureFilter filter() {
        InternalServiceAuthProperties properties = new InternalServiceAuthProperties();
        properties.setEnabled(true);
        properties.setVerifyEnabled(true);
        properties.setSecret(SECRET);
        properties.validate();
        return new InternalServiceSignatureFilter(
                new InternalServiceRequestVerifier(properties, new InMemoryNonceStore()));
    }

    private static final class InMemoryNonceStore implements InternalServiceNonceStore {

        private final Set<String> claimed = new HashSet<>();

        @Override
        public boolean claim(String serviceName, String nonce, long ttlSeconds) {
            return claimed.add(serviceName + ":" + nonce);
        }
    }
}
