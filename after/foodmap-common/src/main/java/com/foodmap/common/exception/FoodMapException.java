package com.foodmap.common.exception;

public class FoodMapException extends RuntimeException {

    private final ErrorCode errorCode;

    public FoodMapException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
    }

    public FoodMapException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public String code() {
        return errorCode.code();
    }
}
