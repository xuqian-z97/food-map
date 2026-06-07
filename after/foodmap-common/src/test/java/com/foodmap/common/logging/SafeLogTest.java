package com.foodmap.common.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SafeLogTest {

    @Test
    void shouldFormatFieldsWithSensitiveValueMasked() {
        String formatted = SafeLog.formatFields(
                LogField.of("userId", 10001L),
                LogField.of("phone", "13812345678"),
                LogField.of("commentContent", "这个评论正文不能进日志")
        );

        assertThat(formatted)
                .contains("userId=10001")
                .contains("phone=138****5678")
                .contains("commentContent=***")
                .doesNotContain("13812345678")
                .doesNotContain("这个评论正文不能进日志");
    }
}
