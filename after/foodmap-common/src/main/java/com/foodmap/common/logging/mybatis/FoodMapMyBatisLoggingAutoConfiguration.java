package com.foodmap.common.logging.mybatis;

import com.foodmap.common.logging.FoodMapLoggingProperties;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.plugin.Interceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * FoodMap MyBatis SQL 日志自动配置，为使用 MyBatis 的业务服务注册 SQL 日志拦截器。
 *
 * <p>本配置只在 MyBatis 类存在时生效。网关等非 MyBatis 服务不会加载该拦截器。</p>
 */
@AutoConfiguration
@ConditionalOnClass({Executor.class, Interceptor.class})
@EnableConfigurationProperties(FoodMapLoggingProperties.class)
public class FoodMapMyBatisLoggingAutoConfiguration {

    /**
     * 注册 FoodMap SQL 日志拦截器，默认启用拦截器但不默认开启全量 SQL DEBUG。
     *
     * @param configProvider SQL 日志配置提供者。
     * @return MyBatis SQL 日志拦截器。
     */
    @Bean
    @ConditionalOnMissingBean(MyBatisSqlLogInterceptor.class)
    @ConditionalOnProperty(prefix = "foodmap.logging.sql", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MyBatisSqlLogInterceptor foodMapMyBatisSqlLogInterceptor(SqlLogConfigProvider configProvider) {
        return new MyBatisSqlLogInterceptor(configProvider);
    }

    /**
     * 注册基于 Environment 的 SQL 日志配置提供者，为后续 Nacos refresh 提供动态读取入口。
     *
     * @param environment Spring Environment。
     * @param properties 启动期 FoodMap 日志配置。
     * @return SQL 日志配置提供者。
     */
    @Bean
    @ConditionalOnMissingBean(SqlLogConfigProvider.class)
    public SqlLogConfigProvider foodMapSqlLogConfigProvider(Environment environment,
                                                           FoodMapLoggingProperties properties) {
        return new EnvironmentSqlLogConfigProvider(environment, properties.getSql());
    }
}
