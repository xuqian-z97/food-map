package com.foodmap.common.logging;

import org.slf4j.Logger;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class SafeLog {

    private SafeLog() {
    }

    public static void info(Logger logger, String eventName, LogField... fields) {
        if (logger != null && logger.isInfoEnabled()) {
            logger.info("{} {}", eventName, formatFields(fields));
        }
    }

    public static void warn(Logger logger, String eventName, LogField... fields) {
        if (logger != null && logger.isWarnEnabled()) {
            logger.warn("{} {}", eventName, formatFields(fields));
        }
    }

    public static void error(Logger logger, String eventName, Throwable throwable, LogField... fields) {
        if (logger != null && logger.isErrorEnabled()) {
            logger.error(eventName + " " + formatFields(fields), throwable);
        }
    }

    public static String formatFields(LogField... fields) {
        if (fields == null || fields.length == 0) {
            return "";
        }
        return Arrays.stream(fields)
                .map(field -> field.name() + "=" + LogMasker.maskByFieldName(field.name(), field.value()))
                .collect(Collectors.joining(" "));
    }
}
