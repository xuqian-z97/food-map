package com.foodmap.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class SafeLogLogTypeTest {
    private final LoggerContext loggerContext = new LoggerContext();
    private final Logger logger = loggerContext.getLogger("com.foodmap.common.logging.SafeLogLogTypeTest");
    private final Map<String, String> logTypeObservedAtAppend = new ConcurrentHashMap<>();

    @BeforeEach
    void attachAppender() {
        AppenderBase<ILoggingEvent> appender = new AppenderBase<>() {
            /**
             * 在日志输出当刻读取 MDC，模拟 JSON 编码器同步读取 logType 的时机。
             *
             * @param event 当前日志事件。
             */
            @Override
            protected void append(ILoggingEvent event) {
                logTypeObservedAtAppend.put(event.getFormattedMessage(), MDC.get(LogMdcKeys.LOG_TYPE));
            }
        };
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldWriteApplicationLogTypeByDefaultAndClearAfterOutput() {
        SafeLog.info(logger, "recommendation.create.success", LogField.of("userId", 10001L));

        assertThat(logTypeObservedAtAppend.values()).containsExactly(LogMdcKeys.LOG_TYPE_APPLICATION);
        assertThat(MDC.get(LogMdcKeys.LOG_TYPE)).isNull();
    }

    @Test
    void shouldWriteExplicitLogTypeAndRestorePreviousValue() {
        MDC.put(LogMdcKeys.LOG_TYPE, LogMdcKeys.LOG_TYPE_AUDIT);

        SafeLog.info(logger, LogMdcKeys.LOG_TYPE_API_ACCESS, "api.access.completed",
                LogField.of("status", 200));

        assertThat(logTypeObservedAtAppend.values()).containsExactly(LogMdcKeys.LOG_TYPE_API_ACCESS);
        assertThat(MDC.get(LogMdcKeys.LOG_TYPE)).isEqualTo(LogMdcKeys.LOG_TYPE_AUDIT);
    }

    @Test
    void shouldFallbackToApplicationWhenLogTypeIsBlank() {
        SafeLog.warn(logger, "  ", "dependency.degraded", LogField.of("dependency", "redis"));

        assertThat(logTypeObservedAtAppend.values()).containsExactly(LogMdcKeys.LOG_TYPE_APPLICATION);
    }

    @Test
    void shouldFallbackToApplicationWhenLogTypeIsUnknown() {
        SafeLog.warn(logger, "client-ip", "dependency.degraded", LogField.of("dependency", "redis"));

        assertThat(logTypeObservedAtAppend.values()).containsExactly(LogMdcKeys.LOG_TYPE_APPLICATION);
    }

    @Test
    void shouldWriteSqlLogTypeForErrorEvent() {
        SafeLog.error(logger, LogMdcKeys.LOG_TYPE_SQL, "sql.execute.failed", new IllegalStateException("boom"),
                LogField.of("mapperId", "FoodMapUserDefineMapper.selectById"));

        assertThat(logTypeObservedAtAppend.values()).containsExactly(LogMdcKeys.LOG_TYPE_SQL);
        assertThat(MDC.get(LogMdcKeys.LOG_TYPE)).isNull();
    }

    @Test
    void shouldDefaultLogTypeToApplicationWhenRequestContextIsWrittenToMdc() {
        LogContext context = LogContext.fromIncoming("req-t028", "trace-t028", "foodmap-auth-service", null, null);

        context.putToMdc();

        assertThat(MDC.get(LogMdcKeys.LOG_TYPE)).isEqualTo(LogMdcKeys.LOG_TYPE_APPLICATION);

        LogContext.clearMdc();

        assertThat(MDC.get(LogMdcKeys.LOG_TYPE)).isNull();
    }
}
