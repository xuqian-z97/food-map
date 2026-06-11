package com.foodmap.common.logging;

/**
 * 安全日志字段，配合 {@link SafeLog} 输出结构化键值对。
 *
 * <p>字段名必须稳定，字段值会在输出前经过 {@link LogMasker} 脱敏。</p>
 */
public record LogField(
        String name,
        Object value
) {
    /**
     * 创建日志字段，推荐业务代码用该工厂方法减少构造细节暴露。
     *
     * @param name 稳定的日志字段名，用于日志检索和脱敏策略匹配。
     * @param value 日志字段值，输出前会由 SafeLog 统一脱敏。
     * @return 可交给 SafeLog 输出的结构化日志字段。
     */
    public static LogField of(String name, Object value) {
        return new LogField(name, value);
    }
}
