package com.foodmap.common.logging.mybatis;

import com.foodmap.common.logging.FoodMapLoggingProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

import java.util.ArrayList;

/**
 * 基于 Spring Environment 的 SQL 日志配置提供者。
 *
 * <p>每次读取都会从 Environment 重新绑定 `foodmap.logging.sql`，因此可以承接后续 Nacos
 * 配置刷新带来的 Environment 变更，同时保留启动时默认值作为回退。</p>
 */
public class EnvironmentSqlLogConfigProvider implements SqlLogConfigProvider {
    private static final String PREFIX = "foodmap.logging.sql";

    private final Environment environment;
    private final FoodMapLoggingProperties.Sql defaults;

    /**
     * 创建 Environment SQL 日志配置提供者。
     *
     * @param environment Spring Environment。
     * @param defaults 启动期默认 SQL 日志配置。
     */
    public EnvironmentSqlLogConfigProvider(Environment environment, FoodMapLoggingProperties.Sql defaults) {
        this.environment = environment;
        this.defaults = copyOf(defaults);
    }

    /**
     * 从 Environment 读取最新 SQL 日志配置。
     *
     * @return 当前 SQL 日志配置。
     */
    @Override
    public FoodMapLoggingProperties.Sql current() {
        FoodMapLoggingProperties.Sql target = copyOf(defaults);
        return Binder.get(environment)
                .bind(PREFIX, Bindable.ofInstance(target))
                .orElse(target);
    }

    /**
     * 复制 SQL 日志配置，避免运行期绑定污染默认配置对象。
     *
     * @param source 原始配置。
     * @return 复制后的配置。
     */
    private static FoodMapLoggingProperties.Sql copyOf(FoodMapLoggingProperties.Sql source) {
        FoodMapLoggingProperties.Sql target = new FoodMapLoggingProperties.Sql();
        if (source == null) {
            return target;
        }
        target.setEnabled(source.isEnabled());
        target.setDebugEnabled(source.isDebugEnabled());
        target.setSlowThresholdMs(source.getSlowThresholdMs());
        target.setSampleRate(source.getSampleRate());
        target.setMapperIncludes(new ArrayList<>(source.getMapperIncludes()));
        target.setMapperExcludes(new ArrayList<>(source.getMapperExcludes()));
        target.setRequestIds(new ArrayList<>(source.getRequestIds()));
        target.setTraceIds(new ArrayList<>(source.getTraceIds()));
        target.setMaxSqlLength(source.getMaxSqlLength());
        return target;
    }
}
