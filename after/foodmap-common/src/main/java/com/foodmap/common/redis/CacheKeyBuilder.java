package com.foodmap.common.redis;

import com.foodmap.common.validation.Check;

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
     *
     * @return 新的 Redis Key 构造器。
     */
    public static CacheKeyBuilder builder() {
        return new CacheKeyBuilder();
    }

    /**
     * 设置服务名片段，例如 {@code recommendation}、{@code community}。
     *
     * @param service 服务名片段。
     * @return 当前构造器实例，便于链式调用。
     */
    public CacheKeyBuilder service(String service) {
        this.service = service;
        return this;
    }

    /**
     * 设置业务名片段，例如 {@code detail}、{@code hotStores}。
     *
     * @param biz 业务名片段。
     * @return 当前构造器实例，便于链式调用。
     */
    public CacheKeyBuilder biz(String biz) {
        this.biz = biz;
        return this;
    }

    /**
     * 设置缓存版本片段，业务结构变化时通过版本隔离旧缓存。
     *
     * @param version 缓存版本片段。
     * @return 当前构造器实例，便于链式调用。
     */
    public CacheKeyBuilder version(String version) {
        this.version = version;
        return this;
    }

    /**
     * 设置业务 Key 片段，通常是业务主键或稳定查询条件摘要。
     *
     * @param key 业务 Key 片段。
     * @return 当前构造器实例，便于链式调用。
     */
    public CacheKeyBuilder key(String key) {
        this.key = key;
        return this;
    }

    /**
     * 生成最终 Redis Key，并拒绝空片段和包含冒号的片段，避免 Key 层级被污染。
     *
     * @return 符合 FoodMap 统一格式的 Redis Key。
     */
    public String build() {
        return String.join(":",
                PREFIX,
                Check.noColon("service", service),
                Check.noColon("biz", biz),
                Check.noColon("version", version),
                Check.noColon("key", key)
        );
    }
}
