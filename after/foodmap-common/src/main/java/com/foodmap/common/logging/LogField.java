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
     */
    public static LogField of(String name, Object value) {
        return new LogField(name, value);
    }
}
