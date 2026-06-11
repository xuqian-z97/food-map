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
    BAD_REQUEST(400, "BAD_REQUEST", "请求参数错误"),
    /**
     * 未认证或认证状态失效，通常用于 Access Token 缺失、过期或无效。
     */
    UNAUTHORIZED(401, "UNAUTHORIZED", "未登录或登录状态已失效"),
    /**
     * 已认证但无权访问目标资源，常见于可见范围、资源归属或角色权限校验失败。
     */
    FORBIDDEN(403, "FORBIDDEN", "无权限访问该资源"),
    /**
     * 目标资源不存在，或为了保护隐私而对无权限资源返回不存在。
     */
    NOT_FOUND(404, "NOT_FOUND", "资源不存在"),
    /**
     * 请求方法不被当前接口支持，排查时应核对 API 文档和网关路由配置。
     */
    METHOD_NOT_ALLOWED(405, "METHOD_NOT_ALLOWED", "请求方法不支持"),
    /**
     * 资源状态冲突，常见于重复申请、重复绑定、非法状态流转或幂等冲突。
     */
    CONFLICT(409, "CONFLICT", "资源状态冲突"),
    /**
     * 请求语法正确但业务语义无法处理，常见于状态不满足、规则不通过等场景。
     */
    UNPROCESSABLE_ENTITY(422, "UNPROCESSABLE_ENTITY", "请求语义无法处理"),
    /**
     * 请求过于频繁，通常由网关限流、登录保护、上传保护或评论保护触发。
     */
    TOO_MANY_REQUESTS(429, "TOO_MANY_REQUESTS", "请求过于频繁"),
    /**
     * 上游服务返回无效响应，主要用于网关或内部调用封装。
     */
    BAD_GATEWAY(502, "BAD_GATEWAY", "上游服务响应异常"),
    /**
     * 服务暂时不可用，常见于维护、过载或依赖组件不可用。
     */
    SERVICE_UNAVAILABLE(503, "SERVICE_UNAVAILABLE", "服务暂时不可用"),
    /**
     * 等待上游服务响应超时，主要用于网关或内部调用封装。
     */
    GATEWAY_TIMEOUT(504, "GATEWAY_TIMEOUT", "上游服务响应超时"),
    /**
     * 未预期系统错误，排查时应结合 traceId、requestId 和服务日志定位根因。
     */
    INTERNAL_ERROR(500, "INTERNAL_ERROR", "系统内部错误");

    private final int status;
    private final String code;
    private final String defaultMessage;

    CommonErrorCode(int status, String code, String defaultMessage) {
        this.status = status;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    /**
     * 返回错误对应的 HTTP 数字状态码，供统一异常响应设置响应状态。
     *
     * @return HTTP 数字状态码。
     */
    @Override
    public int status() {
        return status;
    }

    /**
     * 返回稳定业务错误码，供前端和日志检索细分错误场景。
     *
     * @return 稳定业务错误码。
     */
    @Override
    public String code() {
        return code;
    }

    /**
     * 返回默认错误提示，业务异常未提供自定义消息时使用。
     *
     * @return 默认可展示错误提示。
     */
    @Override
    public String defaultMessage() {
        return defaultMessage;
    }
}
