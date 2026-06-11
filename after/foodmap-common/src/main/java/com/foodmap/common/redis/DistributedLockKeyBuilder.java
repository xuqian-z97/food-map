package com.foodmap.common.redis;

/**
 * 分布式锁 Key 构造器，统一生成 {@code foodmap:{service}:lock:{version}:{bizKey}} 格式的 Redis Key。
 *
 * <p>锁 Key 必须根据业务主键或唯一性数据构造；排查锁竞争时优先检查 bizKey 是否能准确表达临界资源。</p>
 */
public final class DistributedLockKeyBuilder {

    private String service;
    private String version;
    private String bizKey;

    private DistributedLockKeyBuilder() {
    }

    /**
     * 创建新的分布式锁 Key 构造器。
     *
     * @return 分布式锁 Key 构造器。
     */
    public static DistributedLockKeyBuilder builder() {
        return new DistributedLockKeyBuilder();
    }

    /**
     * 设置服务名片段，例如 {@code auth}、{@code recommendation}。
     *
     * @param service 服务名片段。
     * @return 当前构造器。
     */
    public DistributedLockKeyBuilder service(String service) {
        this.service = service;
        return this;
    }

    /**
     * 设置锁版本片段，锁粒度变化时可通过版本隔离旧 Key。
     *
     * @param version 锁版本片段。
     * @return 当前构造器。
     */
    public DistributedLockKeyBuilder version(String version) {
        this.version = version;
        return this;
    }

    /**
     * 设置业务 Key 片段，通常包含业务主键或唯一性数据摘要。
     *
     * @param bizKey 业务 Key 片段。
     * @return 当前构造器。
     */
    public DistributedLockKeyBuilder bizKey(String bizKey) {
        this.bizKey = bizKey;
        return this;
    }

    /**
     * 生成最终 Redis 锁 Key，并复用统一 Redis Key 校验规则。
     *
     * @return 统一格式的 Redis 锁 Key。
     */
    public String build() {
        return CacheKeyBuilder.builder()
                .service(service)
                .biz("lock")
                .version(version)
                .key(bizKey)
                .build();
    }
}
