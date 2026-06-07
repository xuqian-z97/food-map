package com.foodmap.common.logging;

import org.slf4j.Logger;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 安全日志门面，统一输出事件名和脱敏后的结构化字段。
 *
 * <p>业务代码不应直接拼接关键业务日志，应通过该类确保字段格式稳定且敏感值被脱敏。</p>
 */
public final class SafeLog {

    private SafeLog() {
    }

    /**
     * 输出关键业务成功事件，例如推荐创建成功或评论发布成功。
     */
    public static void info(Logger logger, String eventName, LogField... fields) {
        if (logger != null && logger.isInfoEnabled()) {
            logger.info("{} {}", eventName, formatFields(fields));
        }
    }

    /**
     * 输出可恢复问题，例如参数错误、权限失败、幂等重复和外部依赖降级。
     */
    public static void warn(Logger logger, String eventName, LogField... fields) {
        if (logger != null && logger.isWarnEnabled()) {
            logger.warn("{} {}", eventName, formatFields(fields));
        }
    }

    /**
     * 输出系统异常或不可恢复错误，异常对象用于保留堆栈，字段用于定位业务上下文。
     */
    public static void error(Logger logger, String eventName, Throwable throwable, LogField... fields) {
        if (logger != null && logger.isErrorEnabled()) {
            logger.error(eventName + " " + formatFields(fields), throwable);
        }
    }

    /**
     * 将日志字段格式化为稳定的 {@code key=value} 形式，并在输出前统一脱敏。
     */
    public static String formatFields(LogField... fields) {
        if (fields == null || fields.length == 0) {
            return "";
        }
        return Arrays.stream(fields)
                .map(field -> field.name() + "=" + LogMasker.maskByFieldName(field.name(), field.value()))
                .collect(Collectors.joining(" "));
    }
}
