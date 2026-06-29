package com.foodmap.common.security;

import com.foodmap.common.exception.FoodMapException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserResolverTest {

    @Test
    void shouldResolveCurrentUserFromTrustedHeaders() {
        CurrentUser currentUser = CurrentUserResolver.fromTrustedHeaders("2001", null);

        assertThat(currentUser.userId()).isEqualTo(2001L);
        assertThat(currentUser.accountId()).isNull();
    }

    @Test
    void shouldResolveCurrentUserFromUserIdOnlyTrustedHeader() {
        CurrentUser currentUser = CurrentUserResolver.fromTrustedHeaders("2001");

        assertThat(currentUser.userId()).isEqualTo(2001L);
        assertThat(currentUser.accountId()).isNull();
    }

    @Test
    void shouldRejectInvalidTrustedHeaders() {
        assertThatThrownBy(() -> CurrentUserResolver.fromTrustedHeaders("abc", null))
                .isInstanceOf(FoodMapException.class);
    }

    @Test
    void shouldRejectMissingUserIdEvenWhenLegacyAccountIdExists() {
        assertThatThrownBy(() -> CurrentUserResolver.fromTrustedHeaders(null, "1001"))
                .isInstanceOf(FoodMapException.class);
    }
}
