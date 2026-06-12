package com.foodmap.log.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

import java.time.OffsetDateTime;

/**
 * 接口访问摘要日志持久化实体，对应 `foodmap_log_db.api_access_log`。
 */
public class ApiAccessLogEntity extends BaseEntity {
    /**
     * 接口访问摘要日志业务主键。
     */
    private Long accessLogId;
    /**
     * 请求流水号，用于查询一次接口调用相关日志。
     */
    private String requestId;
    /**
     * 链路追踪号，用于串联跨服务调用日志。
     */
    private String traceId;
    /**
     * 当前日志所在调用片段编号。
     */
    private String spanId;
    /**
     * 产生日志的服务名称。
     */
    private String serviceName;
    /**
     * 访问日志事件名，如 api.access.completed 或 api.access.slow。
     */
    private String eventName;
    /**
     * 日志等级，如 INFO、WARN、ERROR。
     */
    private String logLevel;
    /**
     * HTTP 请求方法。
     */
    private String httpMethod;
    /**
     * HTTP 请求路径，不包含敏感请求体。
     */
    private String requestPath;
    /**
     * HTTP 响应状态码。
     */
    private Integer httpStatus;
    /**
     * 接口耗时，单位毫秒。
     */
    private Long durationMs;
    /**
     * 脱敏后的客户端 IP 摘要。
     */
    private String clientIpMasked;
    /**
     * 认证账号业务主键，可为空。
     */
    private Long accountId;
    /**
     * 用户业务主键，可为空。
     */
    private Long userId;
    /**
     * 网关路由标识，可为空。
     */
    private String gatewayRouteId;
    /**
     * 稳定业务错误码或系统错误码，可为空。
     */
    private String errorCode;
    /**
     * 接口访问发生时间。
     */
    private OffsetDateTime occurredTime;
    /**
     * 来源 Kafka topic。
     */
    private String sourceTopic;
    /**
     * 来源 Kafka 分区。
     */
    private Integer sourcePartition;
    /**
     * 来源 Kafka offset。
     */
    private Long sourceOffset;

    public Long getAccessLogId() {
        return accessLogId;
    }

    public void setAccessLogId(Long accessLogId) {
        this.accessLogId = accessLogId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getClientIpMasked() {
        return clientIpMasked;
    }

    public void setClientIpMasked(String clientIpMasked) {
        this.clientIpMasked = clientIpMasked;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getGatewayRouteId() {
        return gatewayRouteId;
    }

    public void setGatewayRouteId(String gatewayRouteId) {
        this.gatewayRouteId = gatewayRouteId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public OffsetDateTime getOccurredTime() {
        return occurredTime;
    }

    public void setOccurredTime(OffsetDateTime occurredTime) {
        this.occurredTime = occurredTime;
    }

    public String getSourceTopic() {
        return sourceTopic;
    }

    public void setSourceTopic(String sourceTopic) {
        this.sourceTopic = sourceTopic;
    }

    public Integer getSourcePartition() {
        return sourcePartition;
    }

    public void setSourcePartition(Integer sourcePartition) {
        this.sourcePartition = sourcePartition;
    }

    public Long getSourceOffset() {
        return sourceOffset;
    }

    public void setSourceOffset(Long sourceOffset) {
        this.sourceOffset = sourceOffset;
    }
}
