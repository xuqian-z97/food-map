package com.foodmap.admin.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

/**
 * 后台操作记录表持久化实体，对应 `admin_operation_logs` 表，不能作为 API 响应直接返回。
 */
public class AdminOperationLogEntity extends BaseEntity {
    /**
     * 后台操作业务主键。
     */
    private Long operationId;
    /**
     * 后台管理员业务主键。
     */
    private Long adminUserId;
    /**
     * 操作模块，如 RBAC、CONTENT、USER、STORE、LOG。
     */
    private String operationModule;
    /**
     * 操作类型，如 CREATE、UPDATE、DECIDE、EXPORT、QUERY。
     */
    private String operationType;
    /**
     * 被操作对象类型。
     */
    private String targetType;
    /**
     * 被操作对象业务主键。
     */
    private Long targetId;
    /**
     * 操作脱敏摘要。
     */
    private String operationSummary;
    /**
     * 操作结果，如 SUCCESS、FAILED。
     */
    private String operationResult;
    /**
     * 操作请求流水号。
     */
    private String requestId;
    /**
     * 操作链路追踪号。
     */
    private String traceId;

    public Long getOperationId() {
        return operationId;
    }

    public void setOperationId(Long operationId) {
        this.operationId = operationId;
    }

    public Long getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(Long adminUserId) {
        this.adminUserId = adminUserId;
    }

    public String getOperationModule() {
        return operationModule;
    }

    public void setOperationModule(String operationModule) {
        this.operationModule = operationModule;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getOperationSummary() {
        return operationSummary;
    }

    public void setOperationSummary(String operationSummary) {
        this.operationSummary = operationSummary;
    }

    public String getOperationResult() {
        return operationResult;
    }

    public void setOperationResult(String operationResult) {
        this.operationResult = operationResult;
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
}
