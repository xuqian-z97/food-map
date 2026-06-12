package com.foodmap.common.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class LogContextTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldUseValidIncomingRequestAndTraceIds() {
        LogContext context = LogContext.fromIncoming("req-123", "trace-456", "foodmap-auth-service", "1001", "2001");

        assertThat(context.requestId()).isEqualTo("req-123");
        assertThat(context.traceId()).isEqualTo("trace-456");
        assertThat(context.serviceName()).isEqualTo("foodmap-auth-service");
        assertThat(context.accountId()).isEqualTo("1001");
        assertThat(context.userId()).isEqualTo("2001");
    }

    @Test
    void shouldGenerateIdsWhenIncomingValuesAreUnsafe() {
        LogContext context = LogContext.fromIncoming("../bad", "", "foodmap-auth-service", null, null);

        assertThat(context.requestId()).matches("[A-Za-z0-9_.-]{16,64}");
        assertThat(context.traceId()).matches("[A-Za-z0-9_.-]{16,64}");
        assertThat(context.spanId()).matches("[A-Za-z0-9_.-]{16,64}");
    }

    @Test
    void shouldPutAndClearMdcFields() {
        LogContext context = LogContext.fromIncoming("req-123", "trace-456", "foodmap-auth-service", "1001", "2001");

        context.putToMdc();

        assertThat(MDC.get(LogMdcKeys.REQUEST_ID)).isEqualTo("req-123");
        assertThat(MDC.get(LogMdcKeys.TRACE_ID)).isEqualTo("trace-456");
        assertThat(MDC.get(LogMdcKeys.SERVICE_NAME)).isEqualTo("foodmap-auth-service");
        assertThat(MDC.get(LogMdcKeys.ACCOUNT_ID)).isEqualTo("1001");
        assertThat(MDC.get(LogMdcKeys.USER_ID)).isEqualTo("2001");

        LogContext.clearMdc();

        assertThat(MDC.get(LogMdcKeys.REQUEST_ID)).isNull();
        assertThat(MDC.get(LogMdcKeys.TRACE_ID)).isNull();
    }
}
