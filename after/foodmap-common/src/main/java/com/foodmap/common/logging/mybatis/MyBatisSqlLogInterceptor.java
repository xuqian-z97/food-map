package com.foodmap.common.logging.mybatis;

import com.foodmap.common.logging.FoodMapLoggingProperties;
import com.foodmap.common.logging.LogField;
import com.foodmap.common.logging.LogMdcKeys;
import com.foodmap.common.logging.SafeLog;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * MyBatis SQL 日志拦截器，统一记录 SQL DEBUG 明细、慢 SQL 和异常 SQL 摘要。
 *
 * <p>该拦截器只输出经过脱敏和截断的 SQL，不改变 SQL 执行结果。生产环境默认关闭全量 DEBUG，
 * 但慢 SQL 和异常 SQL 仍以 WARN 输出，便于通过 requestId 或 traceId 定位数据库问题。</p>
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class
        }),
        @Signature(type = Executor.class, method = "query", args = {
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class
        })
})
public class MyBatisSqlLogInterceptor implements Interceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyBatisSqlLogInterceptor.class);

    private final SqlLogConfigProvider configProvider;

    /**
     * 创建 MyBatis SQL 日志拦截器。
     *
     * @param properties SQL 日志配置。
     */
    public MyBatisSqlLogInterceptor(FoodMapLoggingProperties.Sql properties) {
        this(new StaticSqlLogConfigProvider(properties));
    }

    /**
     * 创建 MyBatis SQL 日志拦截器。
     *
     * @param configProvider SQL 日志配置提供者。
     */
    public MyBatisSqlLogInterceptor(SqlLogConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    /**
     * 拦截 MyBatis Executor 的查询和更新方法，执行完成后输出 SQL 日志。
     *
     * @param invocation MyBatis 调用上下文。
     * @return 原始 MyBatis 调用结果。
     * @throws Throwable 原始 SQL 执行异常。
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) args[0];
        Object parameterObject = args.length > 1 ? args[1] : null;
        BoundSql boundSql = resolveBoundSql(mappedStatement, parameterObject, args);
        long startedAt = System.nanoTime();
        try {
            Object result = invocation.proceed();
            writeSqlLog(mappedStatement, boundSql, parameterObject, startedAt, result, null);
            return result;
        } catch (Throwable throwable) {
            writeSqlLog(mappedStatement, boundSql, parameterObject, startedAt, null, throwable);
            throw throwable;
        }
    }

    /**
     * 包装 MyBatis 目标对象。
     *
     * @param target MyBatis 目标对象。
     * @return 包装后的对象。
     */
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    /**
     * 接收 MyBatis 插件属性，当前配置统一由 Spring Boot ConfigurationProperties 管理。
     *
     * @param properties MyBatis 插件属性。
     */
    @Override
    public void setProperties(Properties properties) {
        // Spring Boot configuration properties are used instead.
    }

    /**
     * 解析 BoundSql，优先使用 MyBatis query 六参方法中已生成的 BoundSql。
     *
     * @param mappedStatement Mapper 映射语句。
     * @param parameterObject SQL 参数对象。
     * @param args MyBatis 调用参数。
     * @return MyBatis BoundSql。
     */
    private BoundSql resolveBoundSql(MappedStatement mappedStatement, Object parameterObject, Object[] args) {
        if (args.length >= 6 && args[5] instanceof BoundSql boundSql) {
            return boundSql;
        }
        return mappedStatement.getBoundSql(parameterObject);
    }

    /**
     * 根据执行结果、耗时和异常状态输出 SQL 日志。
     *
     * @param mappedStatement Mapper 映射语句。
     * @param boundSql MyBatis BoundSql。
     * @param parameterObject SQL 参数对象。
     * @param startedAt 开始纳秒时间。
     * @param result SQL 执行结果。
     * @param throwable SQL 执行异常，可为空。
     */
    private void writeSqlLog(MappedStatement mappedStatement,
                             BoundSql boundSql,
                             Object parameterObject,
                             long startedAt,
                             Object result,
                             Throwable throwable) {
        long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
        String mapperId = mappedStatement.getId();
        String requestId = MDC.get(LogMdcKeys.REQUEST_ID);
        String traceId = MDC.get(LogMdcKeys.TRACE_ID);
        FoodMapLoggingProperties.Sql currentConfig = configProvider.current();
        if (!currentConfig.isEnabled()) {
            return;
        }
        SqlLogPolicy currentPolicy = new SqlLogPolicy(currentConfig);
        String sql = SqlLogFormatter.formatActualSql(
                boundSql.getSql(),
                extractParameters(mappedStatement, boundSql, parameterObject),
                currentConfig.getMaxSqlLength()
        );
        LogField[] fields = new LogField[]{
                LogField.of("mapperId", mapperId),
                LogField.of("sqlType", mappedStatement.getSqlCommandType().name()),
                LogField.of("durationMs", durationMs),
                LogField.of("rows", resolveRows(result)),
                LogField.of("actualSql", sql)
        };
        if (throwable != null) {
            SafeLog.warn(LOGGER, "sql.execute.failed", fields);
            return;
        }
        if (currentPolicy.isSlow(durationMs)) {
            SafeLog.warn(LOGGER, "sql.execute.slow", fields);
            return;
        }
        if (currentPolicy.shouldWriteDebug(mapperId, requestId, traceId)) {
            SafeLog.debug(LOGGER, "sql.execute.debug", fields);
        }
    }

    /**
     * 按 MyBatis 参数映射顺序提取参数值。
     *
     * @param mappedStatement Mapper 映射语句。
     * @param boundSql MyBatis BoundSql。
     * @param parameterObject SQL 参数对象。
     * @return SQL 参数值列表。
     */
    private List<SqlParameterValue> extractParameters(MappedStatement mappedStatement,
                                                      BoundSql boundSql,
                                                      Object parameterObject) {
        List<SqlParameterValue> values = new ArrayList<>();
        List<ParameterMapping> mappings = boundSql.getParameterMappings();
        if (mappings == null || mappings.isEmpty()) {
            return values;
        }
        MetaObject metaObject = parameterObject == null ? null : mappedStatement.getConfiguration().newMetaObject(parameterObject);
        for (ParameterMapping mapping : mappings) {
            String propertyName = mapping.getProperty();
            Object value = resolveParameterValue(mappedStatement, boundSql, parameterObject, metaObject, propertyName);
            values.add(new SqlParameterValue(propertyName, value));
        }
        return values;
    }

    /**
     * 解析单个 MyBatis 参数值，兼容简单类型、Map/Bean 参数和动态 SQL 附加参数。
     *
     * @param mappedStatement Mapper 映射语句。
     * @param boundSql MyBatis BoundSql。
     * @param parameterObject SQL 参数对象。
     * @param metaObject 参数元对象，可为空。
     * @param propertyName 参数属性名。
     * @return 参数实际值。
     */
    private Object resolveParameterValue(MappedStatement mappedStatement,
                                         BoundSql boundSql,
                                         Object parameterObject,
                                         MetaObject metaObject,
                                         String propertyName) {
        if (boundSql.hasAdditionalParameter(propertyName)) {
            return boundSql.getAdditionalParameter(propertyName);
        }
        if (parameterObject == null) {
            return null;
        }
        if (mappedStatement.getConfiguration().getTypeHandlerRegistry().hasTypeHandler(parameterObject.getClass())) {
            return parameterObject;
        }
        if (metaObject != null && metaObject.hasGetter(propertyName)) {
            return metaObject.getValue(propertyName);
        }
        return null;
    }

    /**
     * 从 MyBatis 执行结果推断影响行数或查询行数。
     *
     * @param result SQL 执行结果。
     * @return 行数，无法推断时返回 null。
     */
    private Integer resolveRows(Object result) {
        if (result instanceof Number number) {
            return number.intValue();
        }
        if (result instanceof Collection<?> collection) {
            return collection.size();
        }
        return null;
    }
}
