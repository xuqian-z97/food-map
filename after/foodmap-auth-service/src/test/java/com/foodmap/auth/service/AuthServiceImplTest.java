package com.foodmap.auth.service;

import com.foodmap.auth.application.NoopUserProfileProvisionClient;
import com.foodmap.auth.domain.HmacTokenIssuer;
import com.foodmap.auth.domain.Pbkdf2PasswordHashService;
import com.foodmap.auth.domain.TokenStatus;
import com.foodmap.auth.dto.CurrentAuthResponse;
import com.foodmap.auth.dto.LoginRequest;
import com.foodmap.auth.dto.LoginResponse;
import com.foodmap.auth.dto.LogoutRequest;
import com.foodmap.auth.dto.RefreshTokenRequest;
import com.foodmap.auth.dto.RegisterRequest;
import com.foodmap.auth.dto.RegisterResponse;
import com.foodmap.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.foodmap.auth.infrastructure.persistence.memory.InMemoryAuthAccountRepository;
import com.foodmap.auth.infrastructure.persistence.memory.InMemoryAuthCredentialRepository;
import com.foodmap.auth.infrastructure.persistence.memory.InMemoryLoginLogRepository;
import com.foodmap.auth.infrastructure.persistence.memory.InMemoryRefreshTokenRepository;
import com.foodmap.auth.service.impl.AuthServiceImpl;
import com.foodmap.common.exception.FoodMapException;
import com.foodmap.common.security.AccessTokenDenylistClient;
import com.foodmap.common.security.HmacTokenCodec;
import com.foodmap.common.security.TokenClaims;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("deprecation")
class AuthServiceImplTest {
    private static final String TOKEN_SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void registersUserAndLogsInWithAccountName() {
        AuthService service = newAuthService(new InMemoryRefreshTokenRepository(), new HmacTokenIssuer(TOKEN_SECRET));

        RegisterResponse registerResponse = service.register(defaultRegisterRequest());
        LoginResponse loginResponse = service.login(new LoginRequest("foodie_01", "secret123"));

        assertThat(registerResponse.accountId()).isNull();
        assertThat(registerResponse.userId()).isPositive();
        assertThat(loginResponse.accountId()).isNull();
        assertThat(loginResponse.userId()).isEqualTo(registerResponse.userId());
        assertThat(loginResponse.accessToken()).isNotBlank();
        assertThat(loginResponse.refreshToken()).isNotBlank();

        LoginResponse refreshed = service.refresh(new RefreshTokenRequest(loginResponse.refreshToken()));
        CurrentAuthResponse currentAuth = service.current(refreshed.accessToken());

        assertThat(refreshed.accountId()).isNull();
        assertThat(refreshed.userId()).isEqualTo(registerResponse.userId());
        assertThat(refreshed.accessToken()).isNotBlank();
        assertThat(refreshed.refreshToken()).isEqualTo(loginResponse.refreshToken());
        assertThat(currentAuth.accountId()).isNull();
        assertThat(currentAuth.userId()).isEqualTo(registerResponse.userId());

        service.logout(new LogoutRequest(loginResponse.refreshToken()));

        assertThatThrownBy(() -> service.refresh(new RefreshTokenRequest(loginResponse.refreshToken())))
                .isInstanceOf(FoodMapException.class);
    }

    @Test
    void logoutShouldDenylistUsableAccessTokenUntilOriginalAccessExpiry() {
        HmacTokenIssuer tokenIssuer = new HmacTokenIssuer(TOKEN_SECRET);
        RecordingAccessTokenDenylist recordingDenylist = new RecordingAccessTokenDenylist();
        AuthService service = newAuthServiceWithDenylist(
                new InMemoryRefreshTokenRepository(),
                tokenIssuer,
                recordingDenylist
        );
        RegisterResponse registerResponse = service.register(defaultRegisterRequest());
        LoginResponse loginResponse = service.login(new LoginRequest("foodie_01", "secret123"));
        TokenClaims accessClaims = tokenIssuer.parseAccessToken(loginResponse.accessToken());

        service.logout(new LogoutRequest(loginResponse.refreshToken()), "Bearer " + loginResponse.accessToken());

        assertThat(registerResponse.userId()).isPositive();
        assertThat(recordingDenylist.deniedHash()).isEqualTo(tokenIssuer.tokenHash(loginResponse.accessToken()));
        assertThat(recordingDenylist.expiresTime()).isEqualTo(accessClaims.expiresTime());
        assertThatThrownBy(() -> service.refresh(new RefreshTokenRequest(loginResponse.refreshToken())))
                .isInstanceOf(FoodMapException.class);
    }

    @Test
    void registerUsesRollbackTransactionForUserProvisionFailure() throws NoSuchMethodException {
        Method registerMethod = AuthServiceImpl.class.getMethod("register", RegisterRequest.class);

        Transactional transactional = registerMethod.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.rollbackFor()).contains(Exception.class);
    }

    @Test
    void registerLoginRefreshAndCurrentResponsesUseUserIdOnlyIdentity() {
        AuthService service = newAuthService(new InMemoryRefreshTokenRepository(), new HmacTokenIssuer(TOKEN_SECRET));

        RegisterResponse registerResponse = service.register(defaultRegisterRequest());
        LoginResponse loginResponse = service.login(new LoginRequest("foodie_01", "secret123"));
        LoginResponse refreshed = service.refresh(new RefreshTokenRequest(loginResponse.refreshToken()));
        CurrentAuthResponse currentAuth = service.current(refreshed.accessToken());

        assertThat(registerResponse.accountId()).isNull();
        assertThat(registerResponse.userId()).isPositive();
        assertThat(loginResponse.accountId()).isNull();
        assertThat(loginResponse.userId()).isEqualTo(registerResponse.userId());
        assertThat(refreshed.accountId()).isNull();
        assertThat(refreshed.userId()).isEqualTo(registerResponse.userId());
        assertThat(currentAuth.accountId()).isNull();
        assertThat(currentAuth.userId()).isEqualTo(registerResponse.userId());
    }

    @Test
    void loginIssuedTokensCarryUserIdOnlyClaims() {
        HmacTokenIssuer tokenIssuer = new HmacTokenIssuer(TOKEN_SECRET);
        AuthService service = newAuthService(new InMemoryRefreshTokenRepository(), tokenIssuer);

        RegisterResponse registerResponse = service.register(defaultRegisterRequest());
        LoginResponse loginResponse = service.login(new LoginRequest("foodie_01", "secret123"));

        TokenClaims accessClaims = tokenIssuer.parseAccessToken(loginResponse.accessToken());
        TokenClaims refreshClaims = tokenIssuer.parseRefreshToken(loginResponse.refreshToken());
        assertThat(accessClaims.accountId()).isNull();
        assertThat(accessClaims.userId()).isEqualTo(registerResponse.userId());
        assertThat(refreshClaims.accountId()).isNull();
        assertThat(refreshClaims.userId()).isEqualTo(registerResponse.userId());
    }

    @Test
    void refreshAcceptsUserIdOnlyRefreshTokenWhenStoredTokenIsUsable() {
        HmacTokenIssuer tokenIssuer = new HmacTokenIssuer(TOKEN_SECRET);
        InMemoryRefreshTokenRepository refreshTokenRepository = new InMemoryRefreshTokenRepository();
        AuthService service = newAuthService(refreshTokenRepository, tokenIssuer);
        RegisterResponse registerResponse = service.register(defaultRegisterRequest());
        OffsetDateTime expiresTime = OffsetDateTime.now().plusDays(30);
        String userIdOnlyRefreshToken = new HmacTokenCodec(TOKEN_SECRET)
                .issueRefreshToken(registerResponse.userId(), expiresTime);
        RefreshTokenEntity storedRefreshToken = new RefreshTokenEntity();
        storedRefreshToken.setTokenId(9001L);
        storedRefreshToken.setAccountId(100001L);
        storedRefreshToken.setTokenHash(tokenIssuer.tokenHash(userIdOnlyRefreshToken));
        storedRefreshToken.setExpiresTime(expiresTime);
        storedRefreshToken.setTokenStatus(TokenStatus.ACTIVE.name());
        storedRefreshToken.setCreatedTime(OffsetDateTime.now());
        storedRefreshToken.setUpdatedTime(OffsetDateTime.now());
        storedRefreshToken.setIsDelete((short) 0);
        refreshTokenRepository.save(storedRefreshToken);

        LoginResponse refreshed = service.refresh(new RefreshTokenRequest(userIdOnlyRefreshToken));

        assertThat(refreshed.accountId()).isNull();
        assertThat(refreshed.userId()).isEqualTo(registerResponse.userId());
        assertThat(tokenIssuer.parseAccessToken(refreshed.accessToken()).accountId()).isNull();
    }

    private static AuthService newAuthService(
            InMemoryRefreshTokenRepository refreshTokenRepository,
            HmacTokenIssuer tokenIssuer
    ) {
        return new AuthServiceImpl(
                new TestAuthBusinessIdGenerator(),
                new InMemoryAuthAccountRepository(),
                new InMemoryAuthCredentialRepository(),
                refreshTokenRepository,
                new InMemoryLoginLogRepository(),
                new Pbkdf2PasswordHashService("test-pepper"),
                tokenIssuer,
                new NoopUserProfileProvisionClient()
        );
    }

    private static AuthService newAuthServiceWithDenylist(
            InMemoryRefreshTokenRepository refreshTokenRepository,
            HmacTokenIssuer tokenIssuer,
            AccessTokenDenylistClient denylist
    ) {
        return new AuthServiceImpl(
                new TestAuthBusinessIdGenerator(),
                new InMemoryAuthAccountRepository(),
                new InMemoryAuthCredentialRepository(),
                refreshTokenRepository,
                new InMemoryLoginLogRepository(),
                new Pbkdf2PasswordHashService("test-pepper"),
                tokenIssuer,
                new NoopUserProfileProvisionClient(),
                denylist
        );
    }

    private static RegisterRequest defaultRegisterRequest() {
        return new RegisterRequest(
                "foodie_01",
                "13800138000",
                "foodie@example.com",
                "secret123",
                "小张",
                "IOS"
        );
    }

    private static final class RecordingAccessTokenDenylist implements AccessTokenDenylistClient {
        private String deniedHash;
        private OffsetDateTime expiresTime;

        @Override
        public void deny(String accessTokenHash, OffsetDateTime expiresTime) {
            this.deniedHash = accessTokenHash;
            this.expiresTime = expiresTime;
        }

        @Override
        public boolean contains(String accessTokenHash) {
            return false;
        }

        private String deniedHash() {
            return deniedHash;
        }

        private OffsetDateTime expiresTime() {
            return expiresTime;
        }
    }
}
