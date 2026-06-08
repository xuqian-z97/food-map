package com.foodmap.auth.application;

import com.foodmap.auth.domain.HmacTokenIssuer;
import com.foodmap.auth.domain.Pbkdf2PasswordHashService;
import com.foodmap.auth.dto.LoginRequest;
import com.foodmap.auth.dto.LoginResponse;
import com.foodmap.auth.dto.RegisterRequest;
import com.foodmap.auth.dto.RegisterResponse;
import com.foodmap.auth.infrastructure.persistence.memory.InMemoryAuthAccountRepository;
import com.foodmap.auth.infrastructure.persistence.memory.InMemoryAuthCredentialRepository;
import com.foodmap.auth.infrastructure.persistence.memory.InMemoryLoginLogRepository;
import com.foodmap.auth.infrastructure.persistence.memory.InMemoryRefreshTokenRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthApplicationServiceTest {

    @Test
    void registersAccountAndLogsInWithAccountName() {
        AuthApplicationService service = new AuthApplicationService(
                new InMemoryAuthAccountRepository(),
                new InMemoryAuthCredentialRepository(),
                new InMemoryRefreshTokenRepository(),
                new InMemoryLoginLogRepository(),
                new Pbkdf2PasswordHashService("test-pepper"),
                new HmacTokenIssuer("0123456789abcdef0123456789abcdef"),
                new NoopUserProfileProvisionClient()
        );

        RegisterResponse registerResponse = service.register(new RegisterRequest(
                "foodie_01",
                "13800138000",
                "foodie@example.com",
                "secret123",
                "小张",
                "IOS"
        ));
        LoginResponse loginResponse = service.login(new LoginRequest("foodie_01", "secret123"));

        assertThat(registerResponse.accountId()).isPositive();
        assertThat(registerResponse.userId()).isPositive();
        assertThat(loginResponse.accountId()).isEqualTo(registerResponse.accountId());
        assertThat(loginResponse.userId()).isEqualTo(registerResponse.userId());
        assertThat(loginResponse.accessToken()).isNotBlank();
        assertThat(loginResponse.refreshToken()).isNotBlank();
    }
}
