package com.foodmap.common.security;

import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalServiceRequestVerifierTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final String AUTH_SERVICE = "foodmap-auth-service";
    private static final String ADMIN_SERVICE = "foodmap-admin-service";
    private static final String PROVISION_PATH = "/internal/users/provision";
    private static final byte[] BODY = "{\"userId\":1}".getBytes(StandardCharsets.UTF_8);

    @Test
    void shouldAcceptValidRequest() {
        InternalServiceRequestVerifier verifier = verifier(new InMemoryNonceStore());
        long timestamp = System.currentTimeMillis();
        String nonce = "nonce-valid";

        assertThatCode(() -> verifier.verify(
                AUTH_SERVICE, String.valueOf(timestamp), nonce,
                signature(AUTH_SERVICE, timestamp, nonce, BODY),
                "POST", PROVISION_PATH, BODY
        )).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectMissingSignatureHeader() {
        InternalServiceRequestVerifier verifier = verifier(new InMemoryNonceStore());
        long timestamp = System.currentTimeMillis();

        assertRejected(verifier, AUTH_SERVICE, String.valueOf(timestamp), "nonce-1", null, PROVISION_PATH, BODY,
                CommonErrorCode.UNAUTHORIZED);
    }

    @Test
    void shouldRejectStaleTimestamp() {
        InternalServiceRequestVerifier verifier = verifier(new InMemoryNonceStore());
        long staleTimestamp = System.currentTimeMillis() - 6 * 60 * 1000L;
        String nonce = "nonce-stale";

        assertRejected(verifier, AUTH_SERVICE, String.valueOf(staleTimestamp), nonce,
                signature(AUTH_SERVICE, staleTimestamp, nonce, BODY), PROVISION_PATH, BODY,
                CommonErrorCode.UNAUTHORIZED);
    }

    @Test
    void shouldRejectReplayedNonce() {
        InternalServiceRequestVerifier verifier = verifier(new InMemoryNonceStore());
        long timestamp = System.currentTimeMillis();
        String nonce = "nonce-replay";
        String signature = signature(AUTH_SERVICE, timestamp, nonce, BODY);

        verifier.verify(AUTH_SERVICE, String.valueOf(timestamp), nonce, signature, "POST", PROVISION_PATH, BODY);

        assertRejected(verifier, AUTH_SERVICE, String.valueOf(timestamp), nonce, signature, PROVISION_PATH, BODY,
                CommonErrorCode.UNAUTHORIZED);
    }

    @Test
    void shouldRejectNonWhitelistedService() {
        InternalServiceRequestVerifier verifier = verifier(new InMemoryNonceStore());
        long timestamp = System.currentTimeMillis();
        String nonce = "nonce-admin";

        assertRejected(verifier, ADMIN_SERVICE, String.valueOf(timestamp), nonce,
                signature(ADMIN_SERVICE, timestamp, nonce, BODY), PROVISION_PATH, BODY,
                CommonErrorCode.FORBIDDEN);
    }

    @Test
    void shouldRejectUnregisteredInternalPath() {
        InternalServiceRequestVerifier verifier = verifier(new InMemoryNonceStore());
        long timestamp = System.currentTimeMillis();
        String nonce = "nonce-unknown";
        String path = "/internal/users/delete";

        assertRejected(verifier, AUTH_SERVICE, String.valueOf(timestamp), nonce,
                signature(AUTH_SERVICE, timestamp, nonce, BODY), path, BODY,
                CommonErrorCode.FORBIDDEN);
    }

    @Test
    void shouldRejectTamperedBody() {
        InternalServiceRequestVerifier verifier = verifier(new InMemoryNonceStore());
        long timestamp = System.currentTimeMillis();
        String nonce = "nonce-tampered";

        assertRejected(verifier, AUTH_SERVICE, String.valueOf(timestamp), nonce,
                signature(AUTH_SERVICE, timestamp, nonce, BODY), PROVISION_PATH,
                "{\"userId\":2}".getBytes(StandardCharsets.UTF_8),
                CommonErrorCode.UNAUTHORIZED);
    }

    @Test
    void shouldFailClosedWhenNonceStoreUnavailable() {
        InMemoryNonceStore store = new InMemoryNonceStore();
        store.unavailable = true;
        InternalServiceRequestVerifier verifier = verifier(store);
        long timestamp = System.currentTimeMillis();
        String nonce = "nonce-redis-down";

        assertRejected(verifier, AUTH_SERVICE, String.valueOf(timestamp), nonce,
                signature(AUTH_SERVICE, timestamp, nonce, BODY), PROVISION_PATH, BODY,
                CommonErrorCode.UNAUTHORIZED);
    }

    private static void assertRejected(
            InternalServiceRequestVerifier verifier,
            String service,
            String timestamp,
            String nonce,
            String signature,
            String path,
            byte[] body,
            CommonErrorCode expectedCode
    ) {
        assertThatThrownBy(() -> verifier.verify(service, timestamp, nonce, signature, "POST", path, body))
                .isInstanceOfSatisfying(FoodMapException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(expectedCode));
    }

    private static String signature(String service, long timestamp, String nonce, byte[] body) {
        return InternalServiceSignature.sign(SECRET, service, timestamp, nonce, "POST", PROVISION_PATH, body);
    }

    private static InternalServiceRequestVerifier verifier(InternalServiceNonceStore store) {
        InternalServiceAuthProperties properties = new InternalServiceAuthProperties();
        properties.setEnabled(true);
        properties.setVerifyEnabled(true);
        properties.setSecret(SECRET);
        properties.validate();
        return new InternalServiceRequestVerifier(properties, store);
    }

    private static final class InMemoryNonceStore implements InternalServiceNonceStore {

        private final Set<String> claimed = new HashSet<>();
        private boolean unavailable;

        @Override
        public boolean claim(String serviceName, String nonce, long ttlSeconds) {
            if (unavailable) {
                throw new IllegalStateException("redis unavailable");
            }
            return claimed.add(serviceName + ":" + nonce);
        }
    }
}
