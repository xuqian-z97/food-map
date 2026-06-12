package com.foodmap.common.logging.mybatis;

import com.foodmap.common.logging.FoodMapLoggingProperties;

/**
 * 静态 SQL 日志配置提供者，用于测试或显式固定配置的场景。
 */
public class StaticSqlLogConfigProvider implements SqlLogConfigProvider {
    private final FoodMapLoggingProperties.Sql properties;

    /**
     * 创建静态 SQL 日志配置提供者。
     *
     * @param properties 固定 SQL 日志配置。
     */
    public StaticSqlLogConfigProvider(FoodMapLoggingProperties.Sql properties) {
        this.properties = properties;
    }

    /**
     * 返回固定 SQL 日志配置。
     *
     * @return 固定 SQL 日志配置。
     */
    @Override
    public FoodMapLoggingProperties.Sql current() {
        return properties;
    }
}
