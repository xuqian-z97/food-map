package com.foodmap.common.redis;

/**
 * Redis Key 构造器，强制所有业务缓存使用统一格式 {@code foodmap:{service}:{biz}:{version}:{key}}。
 *
 * <p>排查缓存串数据或脏数据时，优先检查 service、biz 和 version 是否按业务边界设置。</p>
 */
public final class CacheKeyBuilder {

    /**
     * FoodMap 项目 Redis Key 固定前缀，用于避免与其他系统或本地测试数据冲突。
     */
    private static final String PREFIX = "foodmap";

    private String service;
    private String biz;
    private String version;
    private String key;

    private CacheKeyBuilder() {
    }

    /**
     * 创建新的 Key 构造器实例。
     */
    public static CacheKeyBuilder builder() {
        return new CacheKeyBuilder();
    }

    /**
     * 设置服务名片段，例如 {@code recommendation}、{@code community}。
     */
    public CacheKeyBuilder service(String service) {
        this.service = service;
        return this;
    }

    /**
     * 设置业务名片段，例如 {@code detail}、{@code hotStores}。
     */
    public CacheKeyBuilder biz(String biz) {
        this.biz = biz;
        return this;
    }

    /**
     * 设置缓存版本片段，业务结构变化时通过版本隔离旧缓存。
     */
    public CacheKeyBuilder version(String version) {
        this.version = version;
        return this;
    }

    /**
     * 设置业务 Key 片段，通常是业务主键或稳定查询条件摘要。
     */
    public CacheKeyBuilder key(String key) {
        this.key = key;
        return this;
    }

    /**
     * 生成最终 Redis Key，并拒绝空片段和包含冒号的片段，避免 Key 层级被污染。
     */
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
