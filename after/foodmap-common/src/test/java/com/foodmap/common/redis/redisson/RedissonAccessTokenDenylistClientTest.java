package com.foodmap.common.redis.redisson;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class RedissonAccessTokenDenylistClientTest {

    @Test
    void shouldWriteDenylistKeyWithTtlUntilAccessTokenExpires() {
        RecordingRedisson recordingRedisson = new RecordingRedisson();
        RedissonAccessTokenDenylistClient client = new RedissonAccessTokenDenylistClient(recordingRedisson.client());

        client.deny("tokenHash", OffsetDateTime.now().plusMinutes(30));

        assertThat(recordingRedisson.key()).isEqualTo("foodmap:auth:access-token-denylist:v1:tokenHash");
        assertThat(recordingRedisson.value()).isEqualTo("1");
        assertThat(recordingRedisson.ttl()).isPositive();
        assertThat(recordingRedisson.timeUnit()).isEqualTo(TimeUnit.MILLISECONDS);
    }

    @Test
    void shouldReadDenylistKeyByAccessTokenHash() {
        RecordingRedisson recordingRedisson = new RecordingRedisson();
        recordingRedisson.exists(true);
        RedissonAccessTokenDenylistClient client = new RedissonAccessTokenDenylistClient(recordingRedisson.client());

        boolean contains = client.contains("tokenHash");

        assertThat(contains).isTrue();
        assertThat(recordingRedisson.key()).isEqualTo("foodmap:auth:access-token-denylist:v1:tokenHash");
    }

    private static final class RecordingRedisson {
        private String key;
        private String value;
        private Long ttl;
        private TimeUnit timeUnit;
        private boolean exists;

        private RedissonClient client() {
            return (RedissonClient) Proxy.newProxyInstance(
                    RedissonClient.class.getClassLoader(),
                    new Class<?>[]{RedissonClient.class},
                    (proxy, method, args) -> {
                        if ("getBucket".equals(method.getName())) {
                            key = (String) args[0];
                            return bucket();
                        }
                        if ("toString".equals(method.getName())) {
                            return "RecordingRedissonClient";
                        }
                        if ("hashCode".equals(method.getName())) {
                            return System.identityHashCode(proxy);
                        }
                        if ("equals".equals(method.getName())) {
                            return proxy == args[0];
                        }
                        throw new UnsupportedOperationException("Unexpected RedissonClient method: " + method);
                    }
            );
        }

        private RBucket<String> bucket() {
            return (RBucket<String>) Proxy.newProxyInstance(
                    RBucket.class.getClassLoader(),
                    new Class<?>[]{RBucket.class},
                    (proxy, method, args) -> {
                        if ("set".equals(method.getName()) && args != null && args.length == 3) {
                            value = (String) args[0];
                            ttl = (Long) args[1];
                            timeUnit = (TimeUnit) args[2];
                            return null;
                        }
                        if ("isExists".equals(method.getName())) {
                            return exists;
                        }
                        if ("toString".equals(method.getName())) {
                            return "RecordingBucket";
                        }
                        if ("hashCode".equals(method.getName())) {
                            return System.identityHashCode(proxy);
                        }
                        if ("equals".equals(method.getName())) {
                            return proxy == args[0];
                        }
                        throw new UnsupportedOperationException("Unexpected RBucket method: " + method);
                    }
            );
        }

        private String key() {
            return key;
        }

        private String value() {
            return value;
        }

        private Long ttl() {
            return ttl;
        }

        private TimeUnit timeUnit() {
            return timeUnit;
        }

        private void exists(boolean exists) {
            this.exists = exists;
        }
    }
}
