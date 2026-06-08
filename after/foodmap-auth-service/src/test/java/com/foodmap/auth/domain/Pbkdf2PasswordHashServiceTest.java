package com.foodmap.auth.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Pbkdf2PasswordHashServiceTest {

    @Test
    void hashesPasswordWithoutKeepingPlainTextAndVerifiesIt() {
        Pbkdf2PasswordHashService service = new Pbkdf2PasswordHashService("test-pepper");

        String hash = service.hash("secret123");

        assertThat(hash).doesNotContain("secret123");
        assertThat(service.matches("secret123", hash)).isTrue();
        assertThat(service.matches("wrong-password", hash)).isFalse();
    }
}
