package com.foodmap.common.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void shouldCreateOkResponseWithHttpStatus() {
        ApiResponse<String> response = ApiResponse.ok("pong");

        assertThat(response.success()).isTrue();
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.code()).isEqualTo("OK");
        assertThat(response.data()).isEqualTo("pong");
    }

    @Test
    void shouldCreateFailureResponseWithNumericStatusAndStableCode() {
        ApiResponse<Void> response = ApiResponse.fail(401, "UNAUTHORIZED", "登录状态已失效");

        assertThat(response.success()).isFalse();
        assertThat(response.status()).isEqualTo(401);
        assertThat(response.code()).isEqualTo("UNAUTHORIZED");
        assertThat(response.message()).isEqualTo("登录状态已失效");
        assertThat(response.data()).isNull();
    }
}
