package com.foodmap.common.logging.mybatis;

import com.foodmap.common.logging.LogMasker;

import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Locale;

/**
 * SQL 日志格式化器，负责 SQL 归一化、参数替换、脱敏和长度截断。
 */
public final class SqlLogFormatter {

    private SqlLogFormatter() {
    }

    /**
     * 使用已脱敏参数替换 SQL 中的占位符。
     *
     * @param sql MyBatis 生成的 SQL。
     * @param parameters 按占位符顺序排列的参数值。
     * @param maxLength 最大输出长度。
     * @return 可安全输出到日志的 SQL 文本。
     */
    public static String formatActualSql(String sql, List<SqlParameterValue> parameters, int maxLength) {
        String normalizedSql = normalizeSql(sql);
        if (normalizedSql.isBlank() || parameters == null || parameters.isEmpty()) {
            return truncate(normalizedSql, maxLength);
        }

        StringBuilder builder = new StringBuilder();
        int parameterIndex = 0;
        for (int i = 0; i < normalizedSql.length(); i++) {
            char current = normalizedSql.charAt(i);
            if (current == '?' && parameterIndex < parameters.size()) {
                builder.append(toSqlLiteral(parameters.get(parameterIndex)));
                parameterIndex++;
            } else {
                builder.append(current);
            }
        }
        return truncate(builder.toString(), maxLength);
    }

    /**
     * 将 SQL 空白字符归一化为单个空格。
     *
     * @param sql 原始 SQL。
     * @return 归一化后的 SQL。
     */
    public static String normalizeSql(String sql) {
        if (sql == null || sql.isBlank()) {
            return "";
        }
        return sql.replaceAll("\\s+", " ").trim();
    }

    /**
     * 根据 SQL 首词推断 SQL 类型。
     *
     * @param sql SQL 文本。
     * @return SQL 类型，无法识别时返回 UNKNOWN。
     */
    public static String detectSqlType(String sql) {
        String normalized = normalizeSql(sql);
        if (normalized.isBlank()) {
            return "UNKNOWN";
        }
        int firstSpace = normalized.indexOf(' ');
        String firstWord = firstSpace > -1 ? normalized.substring(0, firstSpace) : normalized;
        return firstWord.toUpperCase(Locale.ROOT);
    }

    /**
     * 按最大长度截断文本。
     *
     * @param value 原始文本。
     * @param maxLength 最大长度。
     * @return 截断后的文本。
     */
    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    /**
     * 将单个参数转为 SQL 字面量，敏感值先脱敏。
     *
     * @param parameter SQL 参数值。
     * @return SQL 字面量。
     */
    private static String toSqlLiteral(SqlParameterValue parameter) {
        Object value = parameter.value();
        if (value == null) {
            return "null";
        }
        String masked = LogMasker.maskByFieldName(parameter.name(), value);
        if (value instanceof Number && masked.equals(String.valueOf(value))) {
            return masked;
        }
        if (value instanceof Boolean && masked.equals(String.valueOf(value))) {
            return masked;
        }
        if (value instanceof TemporalAccessor && masked.equals(String.valueOf(value))) {
            return "'" + escape(masked) + "'";
        }
        return "'" + escape(masked) + "'";
    }

    /**
     * 转义 SQL 字符串字面量中的单引号。
     *
     * @param value 原始文本。
     * @return 已转义文本。
     */
    private static String escape(String value) {
        return value.replace("'", "''");
    }
}
