package com.foodmap.common.exception;

/**
 * FoodMap 通用错误码，适用于所有微服务的基础 HTTP 和业务错误场景。
 *
 * <p>服务专属错误码应在各自服务内扩展，不能随意复用不匹配的通用错误码。</p>
 */
public enum CommonErrorCode implements ErrorCode {

    /**
     * 请求参数错误，通常由 Bean Validation 或请求体格式校验失败触发。
     */
    BAD_REQUEST("BAD_REQUEST", "请求参数错误"),
    /**
     * 未认证或认证状态失效，通常用于 Access Token 缺失、过期或无效。
     */
    UNAUTHORIZED("UNAUTHORIZED", "未登录或登录状态已失效"),
    /**
     * 已认证但无权访问目标资源，常见于可见范围、资源归属或角色权限校验失败。
     */
    FORBIDDEN("FORBIDDEN", "无权限访问该资源"),
    /**
     * 目标资源不存在，或为了保护隐私而对无权限资源返回不存在。
     */
    NOT_FOUND("NOT_FOUND", "资源不存在"),
    /**
     * 资源状态冲突，常见于重复申请、重复绑定、非法状态流转或幂等冲突。
     */
    CONFLICT("CONFLICT", "资源状态冲突"),
    /**
     * 请求过于频繁，通常由网关限流、登录保护、上传保护或评论保护触发。
     */
    TOO_MANY_REQUESTS("TOO_MANY_REQUESTS", "请求过于频繁"),
    /**
     * 未预期系统错误，排查时应结合 traceId、requestId 和服务日志定位根因。
     */
    INTERNAL_ERROR("INTERNAL_ERROR", "系统内部错误");

    private final String code;
    private final String defaultMessage;

    CommonErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }
}
