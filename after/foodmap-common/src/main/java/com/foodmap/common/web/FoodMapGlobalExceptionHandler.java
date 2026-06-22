package com.foodmap.common.web;

import com.foodmap.common.api.ApiResponse;
import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import com.foodmap.common.logging.LogField;
import com.foodmap.common.logging.SafeLog;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * FoodMap Web MVC 全局异常拦截器，负责把常见异常转换为统一 API 响应结构。
 *
 * <p>该处理器只面向 Servlet Web 服务，网关等 WebFlux 服务应使用各自的响应适配；排查异常时优先查看
 * 服务端安全日志中的错误码、请求路径和异常类型，不能把堆栈细节暴露给客户端。</p>
 */
@RestControllerAdvice
public class FoodMapGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(FoodMapGlobalExceptionHandler.class);

    /**
     * 处理业务异常，按照异常携带的稳定错误码和 HTTP 数字状态码返回。
     *
     * @param exception 业务异常。
     * @return 统一错误响应。
     */
    @ExceptionHandler(FoodMapException.class)
    public ResponseEntity<ApiResponse<Void>> handleFoodMapException(FoodMapException exception) {
        SafeLog.warn(log, "foodmap_business_exception",
                LogField.of("code", exception.code()),
                LogField.of("status", exception.status()));
        return error(exception.status(), exception.code(), exception.getMessage());
    }

    /**
     * 处理 Bean Validation 请求体校验失败，返回首个字段错误提示。
     *
     * @param exception 请求体字段校验异常。
     * @return HTTP 400 统一错误响应。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(this::formatFieldError)
                .orElse(CommonErrorCode.BAD_REQUEST.defaultMessage());
        return error(CommonErrorCode.BAD_REQUEST.status(), CommonErrorCode.BAD_REQUEST.code(), message);
    }

    /**
     * 处理 query、path 或方法级参数校验失败。
     *
     * @param exception 参数约束异常。
     * @return HTTP 400 统一错误响应。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .orElse(CommonErrorCode.BAD_REQUEST.defaultMessage());
        return error(CommonErrorCode.BAD_REQUEST.status(), CommonErrorCode.BAD_REQUEST.code(), message);
    }

    /**
     * 处理 JSON 请求体无法解析的场景，例如格式错误或字段类型不匹配。
     *
     * @param exception JSON 解析异常。
     * @return HTTP 400 统一错误响应。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException exception) {
        return error(CommonErrorCode.BAD_REQUEST.status(), CommonErrorCode.BAD_REQUEST.code(), "请求体格式错误");
    }

    /**
     * 处理缺少必填请求参数的场景。
     *
     * @param exception 缺少请求参数异常。
     * @return HTTP 400 统一错误响应。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameter(MissingServletRequestParameterException exception) {
        return error(CommonErrorCode.BAD_REQUEST.status(), CommonErrorCode.BAD_REQUEST.code(),
                exception.getParameterName() + "不能为空");
    }

    /**
     * 处理请求参数类型不匹配的场景。
     *
     * @param exception 参数类型转换异常。
     * @return HTTP 400 统一错误响应。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return error(CommonErrorCode.BAD_REQUEST.status(), CommonErrorCode.BAD_REQUEST.code(),
                exception.getName() + "格式错误");
    }

    /**
     * 处理 HTTP 方法不支持的场景，并在 Allow 头中返回服务端支持的方法。
     *
     * @param exception 请求方法不支持异常。
     * @return HTTP 405 统一错误响应。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        HttpHeaders headers = new HttpHeaders();
        if (exception.getSupportedMethods() != null) {
            headers.setAllow(Arrays.stream(exception.getSupportedMethods())
                    .map(org.springframework.http.HttpMethod::valueOf)
                    .collect(Collectors.toSet()));
        }
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(headers)
                .body(ApiResponse.fail(CommonErrorCode.METHOD_NOT_ALLOWED.status(),
                        CommonErrorCode.METHOD_NOT_ALLOWED.code(),
                        CommonErrorCode.METHOD_NOT_ALLOWED.defaultMessage()));
    }

    /**
     * 处理 Spring 抛出的带 HTTP 状态异常，避免框架默认错误结构泄露到接口契约中。
     *
     * @param exception Spring 响应状态异常。
     * @return 统一错误响应。
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        String message = exception.getReason() == null ? HttpStatus.valueOf(status).getReasonPhrase() : exception.getReason();
        return error(status, mapStatusToCode(status), message);
    }

    /**
     * 处理未匹配到 MVC 资源或路由的场景，避免错误路径被兜底为系统内部错误。
     *
     * @param exception 未找到资源异常。
     * @return HTTP 404 统一错误响应。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException exception) {
        return error(CommonErrorCode.NOT_FOUND.status(),
                CommonErrorCode.NOT_FOUND.code(),
                CommonErrorCode.NOT_FOUND.defaultMessage());
    }

    /**
     * 处理非法参数异常，通常由项目级基础校验工具或简单业务前置判断抛出。
     *
     * @param exception 非法参数异常。
     * @return HTTP 400 统一错误响应。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        return error(CommonErrorCode.BAD_REQUEST.status(), CommonErrorCode.BAD_REQUEST.code(), exception.getMessage());
    }

    /**
     * 兜底处理未预期异常，只返回通用错误提示，详细异常写入服务端日志。
     *
     * @param exception 未预期异常。
     * @return HTTP 500 统一错误响应。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        SafeLog.error(log, "foodmap_unexpected_exception", exception,
                LogField.of("code", CommonErrorCode.INTERNAL_ERROR.code()),
                LogField.of("status", CommonErrorCode.INTERNAL_ERROR.status()));
        return error(CommonErrorCode.INTERNAL_ERROR.status(),
                CommonErrorCode.INTERNAL_ERROR.code(),
                CommonErrorCode.INTERNAL_ERROR.defaultMessage());
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + " " + fieldError.getDefaultMessage();
    }

    private ResponseEntity<ApiResponse<Void>> error(int status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.fail(status, code, message));
    }

    private String mapStatusToCode(int status) {
        return switch (status) {
            case 400 -> CommonErrorCode.BAD_REQUEST.code();
            case 401 -> CommonErrorCode.UNAUTHORIZED.code();
            case 403 -> CommonErrorCode.FORBIDDEN.code();
            case 404 -> CommonErrorCode.NOT_FOUND.code();
            case 405 -> CommonErrorCode.METHOD_NOT_ALLOWED.code();
            case 409 -> CommonErrorCode.CONFLICT.code();
            case 422 -> CommonErrorCode.UNPROCESSABLE_ENTITY.code();
            case 429 -> CommonErrorCode.TOO_MANY_REQUESTS.code();
            case 502 -> CommonErrorCode.BAD_GATEWAY.code();
            case 503 -> CommonErrorCode.SERVICE_UNAVAILABLE.code();
            case 504 -> CommonErrorCode.GATEWAY_TIMEOUT.code();
            default -> status >= 500 ? CommonErrorCode.INTERNAL_ERROR.code() : CommonErrorCode.BAD_REQUEST.code();
        };
    }
}
