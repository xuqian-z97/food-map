package com.foodmap.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FoodMapExceptionTest {

    @Test
    void shouldKeepStableErrorCodeAndMessage() {
        FoodMapException exception = new FoodMapException(CommonErrorCode.UNAUTHORIZED, "登录状态已失效");

        assertThat(exception.errorCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED);
        assertThat(exception.code()).isEqualTo("UNAUTHORIZED");
        assertThat(exception.getMessage()).isEqualTo("登录状态已失效");
    }
}
