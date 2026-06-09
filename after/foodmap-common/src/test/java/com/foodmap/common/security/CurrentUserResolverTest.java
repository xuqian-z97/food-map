package com.foodmap.common.security;

import com.foodmap.common.exception.FoodMapException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserResolverTest {

    @Test
    void shouldResolveCurrentUserFromTrustedHeaders() {
        CurrentUser currentUser = CurrentUserResolver.fromTrustedHeaders("2001", "1001");

        assertThat(currentUser.userId()).isEqualTo(2001L);
        assertThat(currentUser.accountId()).isEqualTo(1001L);
    }

    @Test
    void shouldRejectInvalidTrustedHeaders() {
        assertThatThrownBy(() -> CurrentUserResolver.fromTrustedHeaders("abc", "1001"))
                .isInstanceOf(FoodMapException.class);
    }
}
