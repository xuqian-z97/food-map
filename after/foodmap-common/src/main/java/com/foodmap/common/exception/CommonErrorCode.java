package com.foodmap.common.exception;

public enum CommonErrorCode implements ErrorCode {

    BAD_REQUEST("BAD_REQUEST", "请求参数错误"),
    UNAUTHORIZED("UNAUTHORIZED", "未登录或登录状态已失效"),
    FORBIDDEN("FORBIDDEN", "无权限访问该资源"),
    NOT_FOUND("NOT_FOUND", "资源不存在"),
    CONFLICT("CONFLICT", "资源状态冲突"),
    TOO_MANY_REQUESTS("TOO_MANY_REQUESTS", "请求过于频繁"),
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
