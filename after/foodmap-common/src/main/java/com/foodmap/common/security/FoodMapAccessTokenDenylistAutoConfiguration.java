package com.foodmap.common.security;

import com.foodmap.common.redis.redisson.FoodMapRedissonAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Access Token 拒绝名单自动配置，提供无 Redis 场景的安全兜底 Bean。
 *
 * <p>真实 Redisson 实现由 Redis 自动配置优先创建；当 Redis 未启用或测试未提供相关 Bean 时，本配置补充
 * {@link NoopAccessTokenDenylistClient}，避免认证服务和网关启动失败。</p>
 */
@AutoConfiguration(after = FoodMapRedissonAutoConfiguration.class)
public class FoodMapAccessTokenDenylistAutoConfiguration {

    /**
     * 创建空操作 Access Token 拒绝名单客户端。
     *
     * @return 空操作拒绝名单客户端。
     */
    @Bean
    @ConditionalOnMissingBean(AccessTokenDenylistClient.class)
    public AccessTokenDenylistClient noopAccessTokenDenylistClient() {
        return new NoopAccessTokenDenylistClient();
    }
}
