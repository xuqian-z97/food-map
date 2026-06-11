package com.foodmap.common.redis.redisson;

import com.foodmap.common.redis.DistributedLockClient;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * FoodMap Redisson 自动配置，仅在显式启用时创建分布式锁基础设施 Bean。
 *
 * <p>本配置不会替换业务缓存的 Lettuce 客户端，Redisson 只作为 {@link DistributedLockClient} 的实现细节存在。</p>
 */
@AutoConfiguration
@ConditionalOnClass(RedissonClient.class)
@EnableConfigurationProperties(FoodMapRedissonProperties.class)
@ConditionalOnProperty(prefix = "foodmap.redis.redisson", name = "enabled", havingValue = "true")
public class FoodMapRedissonAutoConfiguration {

    /**
     * 创建 Redisson 客户端，统一应用连接池、超时和重试配置。
     *
     * @param properties Redisson 配置属性。
     * @return Redisson 客户端。
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public RedissonClient foodMapRedissonClient(FoodMapRedissonProperties properties) {
        Config config = new Config();
        if (properties.getThreads() > 0) {
            config.setThreads(properties.getThreads());
        }
        if (properties.getNettyThreads() > 0) {
            config.setNettyThreads(properties.getNettyThreads());
        }

        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress(properties.getAddress())
                .setDatabase(properties.getDatabase())
                .setConnectionPoolSize(properties.getConnectionPoolSize())
                .setConnectionMinimumIdleSize(properties.getConnectionMinimumIdleSize())
                .setIdleConnectionTimeout(properties.getIdleConnectionTimeout())
                .setConnectTimeout(properties.getConnectTimeout())
                .setTimeout(properties.getTimeout())
                .setRetryAttempts(properties.getRetryAttempts())
                .setRetryInterval(properties.getRetryInterval());

        if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
            singleServerConfig.setPassword(properties.getPassword());
        }
        if (properties.getClientName() != null && !properties.getClientName().isBlank()) {
            singleServerConfig.setClientName(properties.getClientName());
        }
        return Redisson.create(config);
    }

    /**
     * 创建基于 Redisson 的 FoodMap 分布式锁客户端，业务代码只能依赖 {@link DistributedLockClient}。
     *
     * @param redissonClient Redisson 客户端。
     * @return 分布式锁客户端。
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(DistributedLockClient.class)
    public DistributedLockClient redissonDistributedLockClient(RedissonClient redissonClient) {
        return new RedissonDistributedLockClient(redissonClient);
    }
}
