package com.foodmap.admin.infrastructure.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodmap.admin.application.port.LogApiAccessLogClient;
import com.foodmap.admin.dto.AdminApiAccessLogQueryRequest;
import com.foodmap.admin.dto.AdminApiAccessLogResponse;
import com.foodmap.common.api.ApiResponse;
import com.foodmap.common.api.PageResponse;
import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import com.foodmap.common.logging.LogMdcKeys;
import com.foodmap.common.logging.TraceHeaders;
import org.slf4j.MDC;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 日志平台接口访问摘要 HTTP 客户端，通过内部接口查询 15 天访问摘要。
 */
public class HttpLogApiAccessLogClient implements LogApiAccessLogClient {
    private static final TypeReference<ApiResponse<PageResponse<AdminApiAccessLogResponse>>> RESPONSE_TYPE =
            new TypeReference<>() {
            };

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AdminLogClientProperties properties;

    /**
     * 创建日志平台接口访问摘要 HTTP 客户端。
     *
     * @param httpClient Java HTTP 客户端。
     * @param objectMapper JSON 解析组件。
     * @param properties 日志平台客户端配置。
     */
    public HttpLogApiAccessLogClient(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            AdminLogClientProperties properties
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * 调用日志平台内部接口查询接口访问摘要。
     *
     * @param request 后台日志查询请求。
     * @return 分页接口访问摘要。
     */
    @Override
    public PageResponse<AdminApiAccessLogResponse> search(AdminApiAccessLogQueryRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(buildRequest(request), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new FoodMapException(CommonErrorCode.BAD_GATEWAY, "日志平台响应异常");
            }
            ApiResponse<PageResponse<AdminApiAccessLogResponse>> apiResponse = objectMapper.readValue(response.body(), RESPONSE_TYPE);
            if (!apiResponse.success()) {
                throw new FoodMapException(CommonErrorCode.BAD_GATEWAY, "日志平台响应异常");
            }
            return apiResponse.data();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FoodMapException(CommonErrorCode.GATEWAY_TIMEOUT, "日志平台查询超时", ex);
        } catch (HttpTimeoutException ex) {
            throw new FoodMapException(CommonErrorCode.GATEWAY_TIMEOUT, "日志平台查询超时", ex);
        } catch (IOException ex) {
            throw new FoodMapException(CommonErrorCode.BAD_GATEWAY, "日志平台响应异常", ex);
        }
    }

    /**
     * 构建日志平台查询 HTTP 请求，并透传当前 MDC 中的链路追踪头。
     *
     * @param request 后台日志查询请求。
     * @return HTTP 请求。
     */
    private HttpRequest buildRequest(AdminApiAccessLogQueryRequest request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(normalizedBaseUrl() + "/internal/logs/api-access" + queryString(request)))
                .timeout(properties.getRequestTimeout())
                .GET();
        addHeaderIfPresent(builder, TraceHeaders.REQUEST_ID, MDC.get(LogMdcKeys.REQUEST_ID));
        addHeaderIfPresent(builder, TraceHeaders.TRACE_ID, MDC.get(LogMdcKeys.TRACE_ID));
        addHeaderIfPresent(builder, TraceHeaders.SPAN_ID, MDC.get(LogMdcKeys.SPAN_ID));
        return builder.build();
    }

    /**
     * 生成查询字符串，仅包含非空筛选条件。
     *
     * @param request 后台日志查询请求。
     * @return 查询字符串，可能为空字符串。
     */
    private String queryString(AdminApiAccessLogQueryRequest request) {
        List<String> pairs = new ArrayList<>();
        addPair(pairs, "requestId", request.requestId());
        addPair(pairs, "traceId", request.traceId());
        addPair(pairs, "serviceName", request.serviceName());
        addPair(pairs, "logLevel", request.logLevel());
        addPair(pairs, "httpStatus", request.httpStatus());
        addPair(pairs, "occurredFrom", request.occurredFrom());
        addPair(pairs, "occurredTo", request.occurredTo());
        addPair(pairs, "pageIndex", request.pageIndex());
        addPair(pairs, "pageSize", request.pageSize());
        if (pairs.isEmpty()) {
            return "";
        }
        return "?" + String.join("&", pairs);
    }

    /**
     * 添加非空查询参数。
     *
     * @param pairs 查询参数列表。
     * @param name 参数名。
     * @param value 参数值。
     */
    private void addPair(List<String> pairs, String name, Object value) {
        if (value == null) {
            return;
        }
        String text = value instanceof OffsetDateTime offsetDateTime ? offsetDateTime.toString() : String.valueOf(value);
        if (text.isBlank()) {
            return;
        }
        pairs.add(encode(name) + "=" + encode(text.trim()));
    }

    /**
     * 添加非空 HTTP 请求头。
     *
     * @param builder HTTP 请求构建器。
     * @param name 请求头名称。
     * @param value 请求头值。
     */
    private void addHeaderIfPresent(HttpRequest.Builder builder, String name, String value) {
        if (value != null && !value.isBlank()) {
            builder.header(name, value.trim());
        }
    }

    /**
     * URL 编码查询参数。
     *
     * @param value 原始参数文本。
     * @return 编码后的参数文本。
     */
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    /**
     * 归一化日志服务基础地址，避免拼接重复斜杠。
     *
     * @return 不带结尾斜杠的基础地址。
     */
    private String normalizedBaseUrl() {
        String baseUrl = properties.getBaseUrl();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
