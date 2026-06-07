package com.foodmap.common.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentUserTest {

    @Test
    void shouldUseBigintBusinessKeysForCurrentUserIdentity() {
        CurrentUser currentUser = new CurrentUser(10001L, 20001L, "foodie");

        assertThat(currentUser.userId()).isEqualTo(10001L);
        assertThat(currentUser.accountId()).isEqualTo(20001L);
        assertThat(currentUser.accountName()).isEqualTo("foodie");
    }
}
