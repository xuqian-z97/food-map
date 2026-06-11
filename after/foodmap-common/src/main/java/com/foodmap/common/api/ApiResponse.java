package com.foodmap.common.api;

/**
 * 统一 API 响应结构，所有对外和内部 HTTP 接口都应使用该结构承载业务结果。
 *
 * <p>排查接口问题时优先查看 {@code code} 和 {@code message}，不要从异常堆栈中推断业务语义。</p>
 */
public record ApiResponse<T>(
        boolean success,
        int status,
        String code,
        String message,
        T data
) {
    /**
     * 创建成功响应，固定使用 {@code OK} 作为成功码，便于前端和网关做统一判断。
     *
     * @param data 响应业务数据。
     * @param <T> 响应数据类型。
     * @return HTTP 200 语义的统一成功响应。
     */
    public static <T> ApiResponse<T> ok(T data) {
        return success(200, "OK", "success", data);
    }

    /**
     * 创建成功响应并指定 HTTP 数字状态码，适合 201、202 等具有明确语义的成功场景。
     *
     * @param status HTTP 数字状态码。
     * @param code 稳定业务码。
     * @param message 可展示提示。
     * @param data 响应业务数据。
     * @param <T> 响应数据类型。
     * @return 指定 HTTP 语义的统一成功响应。
     */
    public static <T> ApiResponse<T> success(int status, String code, String message, T data) {
        return new ApiResponse<>(true, status, code, message, data);
    }

    /**
     * 创建失败响应，调用方必须传入稳定错误码，避免把异常文本当作接口契约。
     *
     * @param status HTTP 数字状态码。
     * @param code 稳定业务码。
     * @param message 可展示错误提示。
     * @param <T> 响应数据类型。
     * @return 指定 HTTP 语义的统一失败响应。
     */
    public static <T> ApiResponse<T> fail(int status, String code, String message) {
        return new ApiResponse<>(false, status, code, message, null);
    }

    /**
     * 创建 HTTP 500 语义的兼容失败响应，主要用于老代码迁移期避免散落修改。
     *
     * @param code 稳定业务码。
     * @param message 可展示错误提示。
     * @param <T> 响应数据类型。
     * @return HTTP 500 语义的统一失败响应。
     */
    public static <T> ApiResponse<T> fail(String code, String message) {
        return fail(500, code, message);
    }
}
