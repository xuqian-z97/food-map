package com.foodmap.common.web;

import com.foodmap.common.api.ApiResponse;
import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class FoodMapGlobalExceptionHandlerTest {

    @Test
    void shouldConvertBusinessExceptionToUnifiedResponse() {
        FoodMapGlobalExceptionHandler handler = new FoodMapGlobalExceptionHandler();

        ResponseEntity<ApiResponse<Void>> response = handler.handleFoodMapException(
                new FoodMapException(CommonErrorCode.UNAUTHORIZED, "登录状态已失效"));

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().status()).isEqualTo(401);
        assertThat(response.getBody().code()).isEqualTo("UNAUTHORIZED");
        assertThat(response.getBody().message()).isEqualTo("登录状态已失效");
    }

    @Test
    void shouldHideUnexpectedExceptionDetail() {
        FoodMapGlobalExceptionHandler handler = new FoodMapGlobalExceptionHandler();

        ResponseEntity<ApiResponse<Void>> response = handler.handleUnexpected(
                new IllegalStateException("unexpected dependency failure"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).isEqualTo("系统内部错误");
    }
}
