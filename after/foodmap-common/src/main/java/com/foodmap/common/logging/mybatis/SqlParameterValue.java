package com.foodmap.common.logging.mybatis;

/**
 * SQL 参数值，保留 MyBatis 参数名以便按字段名执行脱敏。
 *
 * @param name MyBatis 参数属性名。
 * @param value 参数实际值。
 */
public record SqlParameterValue(
        String name,
        Object value
) {
}
