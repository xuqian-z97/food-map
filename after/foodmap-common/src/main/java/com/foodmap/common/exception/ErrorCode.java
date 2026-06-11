package com.foodmap.common.exception;

/**
 * 统一错误码接口，业务异常和接口响应都通过它暴露稳定错误语义。
 *
 * <p>排查问题时应优先根据 {@link #code()} 定位错误类型，而不是依赖本地化提示文本。</p>
 */
public interface ErrorCode {

    /**
     * 稳定错误码，必须可枚举、可文档化，并且不能随提示语变化而变化。
     *
     * @return 稳定业务错误码。
     */
    String code();

    /**
     * 错误对应的 HTTP 数字状态码，用于统一异常响应和客户端分类处理。
     *
     * @return HTTP 数字状态码。
     */
    int status();

    /**
     * 默认错误提示，用于未提供业务自定义提示时返回给调用方。
     *
     * @return 默认可展示错误提示。
     */
    String defaultMessage();
}
