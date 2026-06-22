package com.foodmap.common.feign;

import com.foodmap.common.logging.LogMdcKeys;
import com.foodmap.common.logging.TraceHeaders;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;

/**
 * Feign 链路头透传拦截器，确保内部服务调用可以继续通过 requestId 和 traceId 串联日志。
 */
public class FoodMapFeignTraceRequestInterceptor implements RequestInterceptor {

    /**
     * 将当前线程 MDC 中的 FoodMap 标准链路头写入 Feign 请求。
     *
     * @param template Feign 请求模板。
     */
    @Override
    public void apply(RequestTemplate template) {
        addHeaderIfPresent(template, TraceHeaders.REQUEST_ID, MDC.get(LogMdcKeys.REQUEST_ID));
        addHeaderIfPresent(template, TraceHeaders.TRACE_ID, MDC.get(LogMdcKeys.TRACE_ID));
        addHeaderIfPresent(template, TraceHeaders.SPAN_ID, MDC.get(LogMdcKeys.SPAN_ID));
    }

    /**
     * 添加非空请求头。
     *
     * @param template Feign 请求模板。
     * @param name 请求头名称。
     * @param value 请求头值。
     */
    private void addHeaderIfPresent(RequestTemplate template, String name, String value) {
        if (value != null && !value.isBlank()) {
            template.header(name, value.trim());
        }
    }
}
