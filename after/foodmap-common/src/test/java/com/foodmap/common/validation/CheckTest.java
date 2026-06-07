package com.foodmap.common.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckTest {

    @Test
    void shouldReturnTrimmedTextWhenValueHasContent() {
        assertThat(Check.notBlank("storeName", "  小馆  ")).isEqualTo("小馆");
    }

    @Test
    void shouldRejectBlankTextWithFieldName() {
        assertThatThrownBy(() -> Check.notBlank("storeName", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("storeName");
    }

    @Test
    void shouldRejectTextContainingForbiddenColon() {
        assertThatThrownBy(() -> Check.noColon("service", "recommendation:detail"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("service");
    }

    @Test
    void shouldReturnPositiveLongValues() {
        assertThat(Check.positive("ownerUserId", 10001L)).isEqualTo(10001L);
        assertThat(Check.positive("contentLength", 1024L)).isEqualTo(1024L);
    }

    @Test
    void shouldRejectNullOrNonPositiveLongValues() {
        assertThatThrownBy(() -> Check.positive("ownerUserId", (Long) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ownerUserId");
        assertThatThrownBy(() -> Check.positive("contentLength", 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contentLength");
    }
}
