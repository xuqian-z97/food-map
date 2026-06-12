package com.foodmap.common.logging.mybatis;

import com.foodmap.common.logging.FoodMapLoggingProperties;

/**
 * SQL 日志配置提供者，供 MyBatis 拦截器在每次 SQL 执行时读取当前配置。
 *
 * <p>B1.5-b 阶段先通过 Environment 动态读取配置；后续 Nacos refresh 更新 Environment 后，
 * 拦截器无需重建即可读取新的 DEBUG 开关、Mapper 范围和定向 requestId/traceId。</p>
 */
public interface SqlLogConfigProvider {

    /**
     * 返回当前 SQL 日志配置。
     *
     * @return 当前 SQL 日志配置。
     */
    FoodMapLoggingProperties.Sql current();
}
