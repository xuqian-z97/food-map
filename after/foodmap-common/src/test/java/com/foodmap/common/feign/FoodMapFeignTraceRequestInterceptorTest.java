package com.foodmap.common.feign;

import com.foodmap.common.logging.LogMdcKeys;
import com.foodmap.common.logging.TraceHeaders;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class FoodMapFeignTraceRequestInterceptorTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void forwardsTraceHeadersFromMdcToFeignRequest() {
        MDC.put(LogMdcKeys.REQUEST_ID, "request-1");
        MDC.put(LogMdcKeys.TRACE_ID, "trace-1");
        MDC.put(LogMdcKeys.SPAN_ID, "span-1");
        RequestTemplate template = new RequestTemplate();

        FoodMapFeignTraceRequestInterceptor interceptor = new FoodMapFeignTraceRequestInterceptor();

        interceptor.apply(template);

        assertThat(template.headers().get(TraceHeaders.REQUEST_ID)).containsExactly("request-1");
        assertThat(template.headers().get(TraceHeaders.TRACE_ID)).containsExactly("trace-1");
        assertThat(template.headers().get(TraceHeaders.SPAN_ID)).containsExactly("span-1");
    }
}
