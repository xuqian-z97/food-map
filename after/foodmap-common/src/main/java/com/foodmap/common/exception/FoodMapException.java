package com.foodmap.common.exception;

/**
 * FoodMap 业务异常基类，用于携带稳定错误码并交给全局异常处理转换为统一 API 响应。
 *
 * <p>业务代码应抛出该异常或其子类，不应直接把内部异常信息暴露到接口响应或日志中。</p>
 */
public class FoodMapException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 使用错误码默认提示创建业务异常，适合通用错误场景。
     *
     * @param errorCode 稳定错误码。
     */
    public FoodMapException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
    }

    /**
     * 使用业务自定义提示创建异常，但错误码仍必须保持稳定，便于前端和日志检索。
     *
     * @param errorCode 稳定错误码。
     * @param message 可展示业务错误提示。
     */
    public FoodMapException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 返回原始错误码对象，供全局异常处理读取默认提示和稳定错误码。
     *
     * @return 原始错误码对象。
     */
    public ErrorCode errorCode() {
        return errorCode;
    }

    /**
     * 返回错误对应的 HTTP 数字状态码，供统一异常处理设置响应状态。
     *
     * @return HTTP 数字状态码。
     */
    public int status() {
        return errorCode.status();
    }

    /**
     * 返回稳定错误码字符串，适合接口响应和结构化日志字段。
     *
     * @return 稳定错误码字符串。
     */
    public String code() {
        return errorCode.code();
    }
}
