package com.foodmap.common.api;

/**
 * 统一 API 响应结构，所有对外和内部 HTTP 接口都应使用该结构承载业务结果。
 *
 * <p>排查接口问题时优先查看 {@code code} 和 {@code message}，不要从异常堆栈中推断业务语义。</p>
 */
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data
) {
    /**
     * 创建成功响应，固定使用 {@code OK} 作为成功码，便于前端和网关做统一判断。
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", "success", data);
    }

    /**
     * 创建失败响应，调用方必须传入稳定错误码，避免把异常文本当作接口契约。
     */
    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
