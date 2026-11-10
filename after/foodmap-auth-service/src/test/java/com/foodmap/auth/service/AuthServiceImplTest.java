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
import com.foodmap.auth.infrastructure.persistence.entity.AuthIdentityBindingEntity;
import com.foodmap.auth.infrastructure.persistence.entity.AuthSessionEntity;
import com.foodmap.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.foodmap.auth.infrastructure.persistence.memory.InMemoryAuthAccountRepository;
import com.foodmap.auth.infrastructure.persistence.memory.InMemoryAuthCredentialRepository;
import com.foodmap.auth.infrastructure.persistence.memory.InMemoryAuthIdentityBindingRepository;
import com.foodmap.auth.infrastructure.persistence.memory.InMemoryAuthSessionRepository;
import com.foodmap.auth.infrastructure.persistence.memory.InMemoryLoginLogRepository;
import com.foodmap.auth.infrastructure.persistence.memory.InMemoryRefreshTokenRepository;
import com.foodmap.auth.service.impl.AuthServiceImpl;
import com.foodmap.common.exception.FoodMapException;
import com.foodmap.common.security.AccessTokenDenylistClient;
import com.foodmap.common.security.HmacTokenCodec;
import com.foodmap.common.security.NoopAccessTokenDenylistClient;
import com.foodmap.common.security.TokenClaims;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("deprecation")
class AuthServiceImplTest {
    private static final String TOKEN_SECRET = "0123456789abcdef0123456789abcdef";
    private static final String ACCOUNT_NAME_HASH =
            "d6aaeee432d19c9a1296354bd856943f68fcf7de5eff36737b2859c2ebe54ec8";
    private static final String PHONE_HASH =
            "a6942f9771d67f34034d2f1926988ed3fad3bf1b4e7cedb9a31f31398dea43bc";
    private static final String EMAIL_HASH =
            "778c1f9441e83cf5196663e510bf94882e36cd7e5804186c31d4c04b277e0815";
    private static final String SHORT_PHONE_HASH =
            "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4";
    private static final String NORMALIZED_EMAIL_HASH =
            "321ba197033e81286fedb719d60d4ed5cecaed170733cb4a92013811afc0e3b6";

    @Test
    void registersUserAndLogsInWithAccountName() {
        TestContext context = newContext();
        AuthService service = context.service();

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
    void registerWritesThreeLocalIdentityBindingsWithV4HashAndMask() {
        TestContext context = newContext();
        AuthService service = context.service();

        RegisterResponse registerResponse = service.register(defaultRegisterRequest());
        List<AuthIdentityBindingEntity> bindings =
                context.identityBindingRepository().findByUserId(registerResponse.userId());

        assertThat(bindings).hasSize(3);

        AuthIdentityBindingEntity accountName = bindingOfType(bindings, "ACCOUNT_NAME");
        assertThat(accountName.getProvider()).isEqualTo("LOCAL");
        assertThat(accountName.getIdentityHash()).isEqualTo(ACCOUNT_NAME_HASH);
        assertThat(accountName.getDisplayMasked()).isEqualTo("foodie_01");
        assertThat(accountName.getPrimaryIdentity()).isEqualTo((short) 1);
        assertThat(accountName.getVerified()).isEqualTo((short) 1);
        assertThat(accountName.getIdentityStatus()).isEqualTo("NORMAL");
        assertThat(accountName.getBoundTime()).isNotNull();
        assertThat(accountName.getUserId()).isEqualTo(registerResponse.userId());

        AuthIdentityBindingEntity phone = bindingOfType(bindings, "PHONE");
        assertThat(phone.getProvider()).isEqualTo("LOCAL");
        assertThat(phone.getIdentityHash()).isEqualTo(PHONE_HASH);
        assertThat(phone.getDisplayMasked()).isEqualTo("138****8000");
        assertThat(phone.getPrimaryIdentity()).isZero();
        assertThat(phone.getVerified()).isEqualTo((short) 1);
        assertThat(phone.getIdentityStatus()).isEqualTo("NORMAL");

        AuthIdentityBindingEntity email = bindingOfType(bindings, "EMAIL");
        assertThat(email.getProvider()).isEqualTo("LOCAL");
        assertThat(email.getIdentityHash()).isEqualTo(EMAIL_HASH);
        assertThat(email.getDisplayMasked()).isEqualTo("f***@example.com");
        assertThat(email.getPrimaryIdentity()).isZero();
        assertThat(email.getVerified()).isEqualTo((short) 1);
        assertThat(email.getIdentityStatus()).isEqualTo("NORMAL");
    }

    @Test
    void registerNormalizesIdentifiersAndMasksShortPhoneByV4Rules() {
        TestContext context = newContext();

        RegisterResponse registerResponse = context.service().register(new RegisterRequest(
                " Foodie_01 ",
                " 1234 ",
                " Foo@Example.COM ",
                "secret123",
                "小张",
                "IOS"
        ));
        List<AuthIdentityBindingEntity> bindings =
                context.identityBindingRepository().findByUserId(registerResponse.userId());

        AuthIdentityBindingEntity accountName = bindingOfType(bindings, "ACCOUNT_NAME");
        assertThat(accountName.getIdentityHash()).isEqualTo(ACCOUNT_NAME_HASH);
        assertThat(accountName.getDisplayMasked()).isEqualTo("Foodie_01");

        AuthIdentityBindingEntity phone = bindingOfType(bindings, "PHONE");
        assertThat(phone.getIdentityHash()).isEqualTo(SHORT_PHONE_HASH);
        assertThat(phone.getDisplayMasked()).isEqualTo("***");

        AuthIdentityBindingEntity email = bindingOfType(bindings, "EMAIL");
        assertThat(email.getIdentityHash()).isEqualTo(NORMALIZED_EMAIL_HASH);
        assertThat(email.getDisplayMasked()).isEqualTo("f***@example.com");
    }

    @Test
    void registerSkipsPhoneAndEmailBindingsWhenIdentifiersAreAbsent() {
        TestContext context = newContext();

        RegisterResponse registerResponse = context.service().register(new RegisterRequest(
                "only_name",
                null,
                null,
                "secret123",
                "小张",
                null
        ));

        List<AuthIdentityBindingEntity> bindings =
                context.identityBindingRepository().findByUserId(registerResponse.userId());
        assertThat(bindings).hasSize(1);
        assertThat(bindings.get(0).getIdentityType()).isEqualTo("ACCOUNT_NAME");
    }

    @Test
    void loginWritesActiveSessionForIssuedRefreshToken() {
        TestContext context = newContext();
        AuthService service = context.service();

        RegisterResponse registerResponse = service.register(defaultRegisterRequest());
        LoginResponse loginResponse = service.login(new LoginRequest("foodie_01", "secret123"));

        String refreshTokenHash = context.tokenIssuer().tokenHash(loginResponse.refreshToken());
        RefreshTokenEntity refreshTokenEntity = context.refreshTokenRepository()
                .findByTokenHash(refreshTokenHash)
                .orElseThrow();
        List<AuthSessionEntity> sessions = context.sessionRepository().findByUserId(registerResponse.userId());

        assertThat(sessions).hasSize(1);
        AuthSessionEntity session = sessions.get(0);
        assertThat(session.getSessionId()).isEqualTo("sess-" + refreshTokenEntity.getTokenId());
        assertThat(session.getUserId()).isEqualTo(registerResponse.userId());
        assertThat(session.getRefreshTokenHash()).isEqualTo(refreshTokenHash);
        assertThat(session.getSessionStatus()).isEqualTo("ACTIVE");
        assertThat(session.getDeviceType()).isEqualTo("IOS");
        assertThat(session.getExpiresTime()).isEqualTo(loginResponse.refreshTokenExpiresTime());
        assertThat(session.getRevokedTime()).isNull();
    }

    @Test
    void logoutRevokesSessionByRefreshTokenHash() {
        TestContext context = newContext();
        AuthService service = context.service();

        service.register(defaultRegisterRequest());
        LoginResponse loginResponse = service.login(new LoginRequest("foodie_01", "secret123"));
        String refreshTokenHash = context.tokenIssuer().tokenHash(loginResponse.refreshToken());
        AuthSessionEntity session = context.sessionRepository()
                .findByRefreshTokenHash(refreshTokenHash)
                .orElseThrow();
        String sessionId = session.getSessionId();
        assertThat(session.getSessionStatus()).isEqualTo("ACTIVE");

        service.logout(new LogoutRequest(loginResponse.refreshToken()));

        assertThat(context.sessionRepository().findBySessionId(sessionId))
                .hasValueSatisfying(revokedSession -> {
                    assertThat(revokedSession.getSessionStatus()).isEqualTo("REVOKED");
                    assertThat(revokedSession.getRevokedTime()).isNotNull();
                });
        assertThat(context.refreshTokenRepository().findByTokenHash(refreshTokenHash))
                .hasValueSatisfying(token -> assertThat(token.getTokenStatus())
                        .isEqualTo(TokenStatus.REVOKED.name()));
    }

    @Test
    void refreshKeepsSessionActiveWithoutRotatingRefreshToken() {
        TestContext context = newContext();
        AuthService service = context.service();

        RegisterResponse registerResponse = service.register(defaultRegisterRequest());
        LoginResponse loginResponse = service.login(new LoginRequest("foodie_01", "secret123"));
        String tokenHash = context.tokenIssuer().tokenHash(loginResponse.refreshToken());
        AuthSessionEntity session = context.sessionRepository().findByRefreshTokenHash(tokenHash).orElseThrow();
        String sessionId = session.getSessionId();

        LoginResponse refreshed = service.refresh(new RefreshTokenRequest(loginResponse.refreshToken()));

        assertThat(refreshed.refreshToken()).isEqualTo(loginResponse.refreshToken());
        assertThat(context.sessionRepository().findBySessionId(sessionId))
                .hasValueSatisfying(current -> {
                    assertThat(current.getSessionStatus()).isEqualTo("ACTIVE");
                    assertThat(current.getRevokedTime()).isNull();
                    assertThat(current.getRefreshTokenHash()).isEqualTo(tokenHash);
                });
        assertThat(context.sessionRepository().findByUserId(registerResponse.userId())).hasSize(1);
    }

    @Test
    void logoutShouldDenylistUsableAccessTokenUntilOriginalAccessExpiry() {
        HmacTokenIssuer tokenIssuer = new HmacTokenIssuer(TOKEN_SECRET);
        RecordingAccessTokenDenylist recordingDenylist = new RecordingAccessTokenDenylist();
        TestContext context = newContext(recordingDenylist);
        AuthService service = context.service();
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
    void logoutShouldRejectInvalidAccessTokenAndNotWriteDenylist() {
        RecordingAccessTokenDenylist recordingDenylist = new RecordingAccessTokenDenylist();
        TestContext context = newContext(recordingDenylist);
        AuthService service = context.service();
        service.register(defaultRegisterRequest());
        LoginResponse loginResponse = service.login(new LoginRequest("foodie_01", "secret123"));

        assertThatThrownBy(() -> service.logout(
                new LogoutRequest(loginResponse.refreshToken()), "Bearer not-a-valid-token"))
                .isInstanceOf(FoodMapException.class)
                .satisfies(ex -> assertThat(((FoodMapException) ex).status()).isEqualTo(401));

        assertThat(recordingDenylist.denyCount()).isZero();
        assertThat(recordingDenylist.deniedHash()).isNull();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"", " ", "Basic invalid", "Bearer ", "Bearer", "Bearer not-a-valid-token"})
    void logoutShouldRejectProvidedInvalidAuthorizationWithoutChangingSession(String authorization) {
        assertLogoutRejectedWithoutChangingSession(authorization, false);
    }

    @Test
    void logoutShouldRejectExpiredAccessTokenWithoutChangingSession() {
        assertLogoutRejectedWithoutChangingSession(null, true);
    }

    /**
     * 验证失败登出在写操作前拒绝，内存替身不依赖 Spring 事务也不能留下半撤销状态。
     *
     * @param authorization 要验证的异常认证头。
     * @param expired 是否由本轮签发器生成已过期 Token。
     */
    private void assertLogoutRejectedWithoutChangingSession(String authorization, boolean expired) {
        RecordingAccessTokenDenylist denylist = new RecordingAccessTokenDenylist();
        TestContext context = newContext(denylist);
        AuthService service = context.service();
        RegisterResponse registered = service.register(defaultRegisterRequest());
        LoginResponse login = service.login(new LoginRequest("foodie_01", "secret123"));
        String header = expired ? "Bearer " + context.tokenIssuer().issueAccessToken(
                registered.userId(), OffsetDateTime.now().minusMinutes(1)) : authorization;
        String hash = context.tokenIssuer().tokenHash(login.refreshToken());

        assertThatThrownBy(() -> service.logout(new LogoutRequest(login.refreshToken()), header))
                .isInstanceOfSatisfying(FoodMapException.class, ex -> assertThat(ex.status()).isEqualTo(401));

        assertThat(denylist.denyCount()).isZero();
        assertThat(context.refreshTokenRepository().findByTokenHash(hash)).hasValueSatisfying(
                token -> assertThat(token.getTokenStatus()).isEqualTo("ACTIVE"));
        assertThat(context.sessionRepository().findByRefreshTokenHash(hash)).hasValueSatisfying(
                session -> assertThat(session.getSessionStatus()).isEqualTo("ACTIVE"));
        assertThat(service.refresh(new RefreshTokenRequest(login.refreshToken())).userId())
                .isEqualTo(registered.userId());
    }

    @Test
    void logoutWithoutAccessTokenShouldOnlyRevokeRefreshToken() {
        RecordingAccessTokenDenylist recordingDenylist = new RecordingAccessTokenDenylist();
        TestContext context = newContext(recordingDenylist);
        AuthService service = context.service();
        RegisterResponse registerResponse = service.register(defaultRegisterRequest());
        LoginResponse loginResponse = service.login(new LoginRequest("foodie_01", "secret123"));

        service.logout(new LogoutRequest(loginResponse.refreshToken()), null);

        assertThat(recordingDenylist.denyCount()).isZero();
        assertThat(context.tokenIssuer().parseAccessToken(loginResponse.accessToken()).userId())
                .isEqualTo(registerResponse.userId());
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
        TestContext context = newContext();
        AuthService service = context.service();

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
        TestContext context = newContext();
        AuthService service = context.service();

        RegisterResponse registerResponse = service.register(defaultRegisterRequest());
        LoginResponse loginResponse = service.login(new LoginRequest("foodie_01", "secret123"));

        TokenClaims accessClaims = context.tokenIssuer().parseAccessToken(loginResponse.accessToken());
        TokenClaims refreshClaims = context.tokenIssuer().parseRefreshToken(loginResponse.refreshToken());
        assertThat(accessClaims.accountId()).isNull();
        assertThat(accessClaims.userId()).isEqualTo(registerResponse.userId());
        assertThat(refreshClaims.accountId()).isNull();
        assertThat(refreshClaims.userId()).isEqualTo(registerResponse.userId());
    }

    @Test
    void refreshAcceptsUserIdOnlyRefreshTokenWhenStoredTokenIsUsable() {
        TestContext context = newContext();
        AuthService service = context.service();
        RegisterResponse registerResponse = service.register(defaultRegisterRequest());
        OffsetDateTime expiresTime = OffsetDateTime.now().plusDays(30);
        String userIdOnlyRefreshToken = new HmacTokenCodec(TOKEN_SECRET)
                .issueRefreshToken(registerResponse.userId(), expiresTime);
        RefreshTokenEntity storedRefreshToken = new RefreshTokenEntity();
        storedRefreshToken.setTokenId(9001L);
        storedRefreshToken.setAccountId(100001L);
        storedRefreshToken.setTokenHash(context.tokenIssuer().tokenHash(userIdOnlyRefreshToken));
        storedRefreshToken.setExpiresTime(expiresTime);
        storedRefreshToken.setTokenStatus(TokenStatus.ACTIVE.name());
        storedRefreshToken.setCreatedTime(OffsetDateTime.now());
        storedRefreshToken.setUpdatedTime(OffsetDateTime.now());
        storedRefreshToken.setIsDelete((short) 0);
        context.refreshTokenRepository().save(storedRefreshToken);

        LoginResponse refreshed = service.refresh(new RefreshTokenRequest(userIdOnlyRefreshToken));

        assertThat(refreshed.accountId()).isNull();
        assertThat(refreshed.userId()).isEqualTo(registerResponse.userId());
        assertThat(context.tokenIssuer().parseAccessToken(refreshed.accessToken()).accountId()).isNull();
        assertThat(refreshed.refreshToken()).isEqualTo(userIdOnlyRefreshToken);
        assertThat(context.sessionRepository().findByUserId(registerResponse.userId())).isEmpty();
    }

    private static TestContext newContext() {
        return newContext(new NoopAccessTokenDenylistClient());
    }

    private static TestContext newContext(AccessTokenDenylistClient denylist) {
        HmacTokenIssuer tokenIssuer = new HmacTokenIssuer(TOKEN_SECRET);
        InMemoryRefreshTokenRepository refreshTokenRepository = new InMemoryRefreshTokenRepository();
        InMemoryAuthIdentityBindingRepository identityBindingRepository = new InMemoryAuthIdentityBindingRepository();
        InMemoryAuthSessionRepository sessionRepository = new InMemoryAuthSessionRepository();
        AuthService service = new AuthServiceImpl(
                new TestAuthBusinessIdGenerator(),
                new InMemoryAuthAccountRepository(),
                new InMemoryAuthCredentialRepository(),
                refreshTokenRepository,
                identityBindingRepository,
                sessionRepository,
                new InMemoryLoginLogRepository(),
                new Pbkdf2PasswordHashService("test-pepper"),
                tokenIssuer,
                new NoopUserProfileProvisionClient(),
                denylist
        );
        return new TestContext(
                service,
                refreshTokenRepository,
                identityBindingRepository,
                sessionRepository,
                tokenIssuer
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

    private static AuthIdentityBindingEntity bindingOfType(
            List<AuthIdentityBindingEntity> bindings,
            String identityType
    ) {
        return bindings.stream()
                .filter(binding -> identityType.equals(binding.getIdentityType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing identity binding: " + identityType));
    }

    private record TestContext(
            AuthService service,
            InMemoryRefreshTokenRepository refreshTokenRepository,
            InMemoryAuthIdentityBindingRepository identityBindingRepository,
            InMemoryAuthSessionRepository sessionRepository,
            HmacTokenIssuer tokenIssuer
    ) {
    }

    private static final class RecordingAccessTokenDenylist implements AccessTokenDenylistClient {
        private String deniedHash;
        private OffsetDateTime expiresTime;
        private int denyCount;

        @Override
        public void deny(String accessTokenHash, OffsetDateTime expiresTime) {
            this.deniedHash = accessTokenHash;
            this.expiresTime = expiresTime;
            this.denyCount++;
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

        private int denyCount() {
            return denyCount;
        }
    }
}
