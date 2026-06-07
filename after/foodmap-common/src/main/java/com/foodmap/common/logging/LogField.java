package com.foodmap.common.logging;

public record LogField(
        String name,
        Object value
) {
    public static LogField of(String name, Object value) {
        return new LogField(name, value);
    }
}
