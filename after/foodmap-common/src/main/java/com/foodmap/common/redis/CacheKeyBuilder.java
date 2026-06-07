package com.foodmap.common.redis;

public final class CacheKeyBuilder {

    private static final String PREFIX = "foodmap";

    private String service;
    private String biz;
    private String version;
    private String key;

    private CacheKeyBuilder() {
    }

    public static CacheKeyBuilder builder() {
        return new CacheKeyBuilder();
    }

    public CacheKeyBuilder service(String service) {
        this.service = service;
        return this;
    }

    public CacheKeyBuilder biz(String biz) {
        this.biz = biz;
        return this;
    }

    public CacheKeyBuilder version(String version) {
        this.version = version;
        return this;
    }

    public CacheKeyBuilder key(String key) {
        this.key = key;
        return this;
    }

    public String build() {
        return String.join(":",
                PREFIX,
                requireText("service", service),
                requireText("biz", biz),
                requireText("version", version),
                requireText("key", key)
        );
    }

    private static String requireText(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (value.contains(":")) {
            throw new IllegalArgumentException(fieldName + " must not contain ':'");
        }
        return value.trim();
    }
}
