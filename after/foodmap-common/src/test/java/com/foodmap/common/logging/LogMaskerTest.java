package com.foodmap.common.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogMaskerTest {

    @Test
    void shouldMaskPhoneEmailBearerTokenAndObjectKey() {
        assertThat(LogMasker.maskPhone("13812345678")).isEqualTo("138****5678");
        assertThat(LogMasker.maskEmail("test@example.com")).isEqualTo("t***@example.com");
        assertThat(LogMasker.maskToken("Bearer abc.def.ghi")).isEqualTo("Bearer ***");
        assertThat(LogMasker.maskObjectKey("recommendation/private/user-10001/a/b/c.jpg"))
                .isEqualTo("recommendation/private/***/c.jpg");
    }

    @Test
    void shouldMaskSensitiveValueByFieldName() {
        assertThat(LogMasker.maskByFieldName("phone", "13812345678")).isEqualTo("138****5678");
        assertThat(LogMasker.maskByFieldName("email", "test@example.com")).isEqualTo("t***@example.com");
        assertThat(LogMasker.maskByFieldName("authorization", "Bearer abc.def.ghi")).isEqualTo("Bearer ***");
        assertThat(LogMasker.maskByFieldName("commentContent", "这个评论正文不能进日志")).isEqualTo("***");
    }
}
