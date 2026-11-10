package com.foodmap.common.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InternalServiceAccessPolicyTest {

    @Test
    void provisionShouldOnlyAllowAuthService() {
        assertThat(InternalServiceAccessPolicy.allowedService("/internal/users/provision"))
                .contains("foodmap-auth-service");
    }

    @Test
    void logsShouldOnlyAllowAdminService() {
        assertThat(InternalServiceAccessPolicy.allowedService("/internal/logs/api-access"))
                .contains("foodmap-admin-service");
    }

    @Test
    void unknownInternalPathShouldBeDeniedByDefault() {
        assertThat(InternalServiceAccessPolicy.allowedService("/internal/users/delete")).isEmpty();
    }

    @Test
    void healthPathShouldBeDetectedAsExempt() {
        assertThat(InternalServiceAccessPolicy.isHealthPath("/internal/auth/health")).isTrue();
        assertThat(InternalServiceAccessPolicy.isHealthPath("/internal/user/health")).isTrue();
        assertThat(InternalServiceAccessPolicy.isHealthPath("/internal/auth/login")).isFalse();
        assertThat(InternalServiceAccessPolicy.isHealthPath("/internal/a/b/health")).isFalse();
    }

    @Test
    void nonInternalPathShouldNotBeTreatedAsInternal() {
        assertThat(InternalServiceAccessPolicy.isInternalPath("/api/users/me")).isFalse();
        assertThat(InternalServiceAccessPolicy.isInternalPath("/internal/users/provision")).isTrue();
    }
}
