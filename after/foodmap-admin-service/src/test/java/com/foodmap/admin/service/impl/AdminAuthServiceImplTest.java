package com.foodmap.admin.service.impl;

import com.foodmap.admin.application.port.AdminAccessSessionRepository;
import com.foodmap.admin.domain.AdminStatus;
import com.foodmap.admin.dto.AdminLoginRequest;
import com.foodmap.admin.dto.AdminLoginResponse;
import com.foodmap.admin.dto.CreateAdminUserRequest;
import com.foodmap.admin.infrastructure.persistence.entity.AdminUserEntity;
import com.foodmap.admin.infrastructure.persistence.memory.InMemoryAdminUserRepository;
import com.foodmap.admin.security.AdminTokenIssuer;
import com.foodmap.admin.security.AuthenticatedAdmin;
import com.foodmap.admin.service.AdminLoginRateLimiter;
import com.foodmap.admin.service.AdminOperationLogService;
import com.foodmap.admin.service.AdminPasswordHashService;
import com.foodmap.admin.service.Pbkdf2AdminPasswordHashService;
import com.foodmap.admin.service.TestAdminBusinessIdGenerator;
import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 后台认证应用服务测试：登录成功、各类失败、限流、数据源故障与登出撤销。
 */
class AdminAuthServiceImplTest {
    private static final String USERNAME = "ops_admin";
    private static final String PASSWORD = "secret123";

    private InMemoryAdminUserRepository userRepository;
    private FakeAdminRoleRepository roleRepository;
    private FakeAdminAccessSessionRepository sessionRepository;
    private FakeAdminOperationLogRepository operationLogRepository;
    private AdminLoginRateLimiter rateLimiter;
    private AdminAuthServiceImpl authService;
    private Long adminUserId;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryAdminUserRepository();
        roleRepository = new FakeAdminRoleRepository();
        sessionRepository = new FakeAdminAccessSessionRepository();
        operationLogRepository = new FakeAdminOperationLogRepository();
        rateLimiter = new AdminLoginRateLimiter(3, Duration.ofMinutes(5));
        authService = buildService(userRepository, sessionRepository);
        AdminPasswordHashService passwordHashService = new Pbkdf2AdminPasswordHashService("test-pepper");
        AdminUserServiceImpl userService = new AdminUserServiceImpl(
                new TestAdminBusinessIdGenerator(), userRepository, passwordHashService);
        adminUserId = userService.createAdminUser(new CreateAdminUserRequest(
                USERNAME, PASSWORD, "运营管理员", null, null)).adminUserId();
        roleRepository.putAdminRoles(adminUserId, java.util.List.of("OPS_VIEWER"));
    }

    /**
     * 组装被测服务，允许替换仓储注入故障。
     *
     * @param userRepository 用户仓储。
     * @param sessionRepository 会话仓储。
     * @return 被测服务实例。
     */
    private AdminAuthServiceImpl buildService(
            InMemoryAdminUserRepository userRepository,
            AdminAccessSessionRepository sessionRepository
    ) {
        try {
            RSAKey keyPair = new RSAKeyGenerator(2048).keyUse(KeyUse.SIGNATURE).keyID("test-key").generate();
            return new AdminAuthServiceImpl(
                    userRepository,
                    roleRepository,
                    sessionRepository,
                    new TestAdminBusinessIdGenerator(),
                    new Pbkdf2AdminPasswordHashService("test-pepper"),
                    new AdminTokenIssuer(keyPair.toRSAPublicKey(), keyPair.toRSAPrivateKey(), Duration.ofMinutes(15)),
                    new AdminOperationLogService(operationLogRepository, new TestAdminBusinessIdGenerator()),
                    rateLimiter,
                    TransactionOperations.withoutTransaction()
            );
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void loginShouldIssueTokenAndRegisterSession() {
        AdminLoginResponse response = authService.login(new AdminLoginRequest(USERNAME, PASSWORD));
        String tokenId = sessionRepository.lastRegisteredTokenId();

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresTime()).isAfter(OffsetDateTime.now().plusMinutes(10));
        assertThat(sessionRepository.session(tokenId)).isPresent();
        assertThat(sessionRepository.session(tokenId).orElseThrow().getAdminUserId()).isEqualTo(adminUserId);
        assertThat(userRepository.findByUsername(USERNAME).orElseThrow().getLastLoginTime()).isNotNull();
        assertThat(operationLogRepository.appended()).hasSize(1);
        assertThat(operationLogRepository.appended().get(0).getOperationType()).isEqualTo("LOGIN");
        assertThat(operationLogRepository.appended().get(0).getOperationResult()).isEqualTo("SUCCESS");
    }

    @Test
    void loginShouldRejectUnknownUsernameWithoutSessionOrAuditRow() {
        assertThatThrownBy(() -> authService.login(new AdminLoginRequest("nobody", PASSWORD)))
                .isInstanceOf(FoodMapException.class)
                .extracting(ex -> ((FoodMapException) ex).errorCode())
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);

        assertThat(sessionRepository.lastRegisteredTokenId()).isNull();
        assertThat(operationLogRepository.appended()).isEmpty();
    }

    @Test
    void loginShouldRejectWrongPassword() {
        assertThatThrownBy(() -> authService.login(new AdminLoginRequest(USERNAME, "wrong")))
                .isInstanceOf(FoodMapException.class)
                .extracting(ex -> ((FoodMapException) ex).errorCode())
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);

        assertThat(sessionRepository.lastRegisteredTokenId()).isNull();
    }

    @Test
    void loginShouldRejectDisabledAccount() {
        disableAccount();

        assertThatThrownBy(() -> authService.login(new AdminLoginRequest(USERNAME, PASSWORD)))
                .isInstanceOf(FoodMapException.class)
                .extracting(ex -> ((FoodMapException) ex).errorCode())
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }

    @Test
    void loginShouldFailClosedWhenLockedRowMissing() {
        InMemoryAdminUserRepository lockFails = new InMemoryAdminUserRepository() {
            @Override
            public Optional<AdminUserEntity> lockByAdminUserId(Long id) {
                return Optional.empty();
            }
        };
        lockFails.save(userRepository.findByUsername(USERNAME).orElseThrow());
        authService = buildService(lockFails, sessionRepository);

        assertThatThrownBy(() -> authService.login(new AdminLoginRequest(USERNAME, PASSWORD)))
                .isInstanceOf(FoodMapException.class)
                .extracting(ex -> ((FoodMapException) ex).errorCode())
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);

        assertThat(sessionRepository.lastRegisteredTokenId()).isNull();
    }

    @Test
    void loginShouldRejectWhenRateLimited() {
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> authService.login(new AdminLoginRequest(USERNAME, "wrong")))
                    .isInstanceOf(FoodMapException.class);
        }

        assertThatThrownBy(() -> authService.login(new AdminLoginRequest(USERNAME, PASSWORD)))
                .isInstanceOf(FoodMapException.class)
                .extracting(ex -> ((FoodMapException) ex).errorCode())
                .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    void successfulLoginShouldResetFailureCounter() {
        assertThatThrownBy(() -> authService.login(new AdminLoginRequest(USERNAME, "wrong")))
                .isInstanceOf(FoodMapException.class);

        assertThat(authService.login(new AdminLoginRequest(USERNAME, PASSWORD)).accessToken()).isNotBlank();

        assertThatThrownBy(() -> authService.login(new AdminLoginRequest(USERNAME, "wrong")))
                .isInstanceOf(FoodMapException.class)
                .extracting(ex -> ((FoodMapException) ex).errorCode())
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }

    @Test
    void loginShouldReturnServiceUnavailableWhenIdentityStoreBroken() {
        InMemoryAdminUserRepository broken = new InMemoryAdminUserRepository() {
            @Override
            public Optional<AdminUserEntity> findByUsername(String username) {
                throw new DataAccessResourceFailureException("db down");
            }
        };
        broken.save(userRepository.findByUsername(USERNAME).orElseThrow());
        authService = buildService(broken, sessionRepository);

        assertThatThrownBy(() -> authService.login(new AdminLoginRequest(USERNAME, PASSWORD)))
                .isInstanceOf(FoodMapException.class)
                .extracting(ex -> ((FoodMapException) ex).errorCode())
                .isEqualTo(CommonErrorCode.SERVICE_UNAVAILABLE);
    }

    @Test
    void logoutShouldRevokeSessionAndStayIdempotent() {
        AdminLoginResponse response = authService.login(new AdminLoginRequest(USERNAME, PASSWORD));
        String tokenId = sessionRepository.lastRegisteredTokenId();
        AuthenticatedAdmin admin = new AuthenticatedAdmin(adminUserId, tokenId, 1L, Set.of());

        authService.logout(admin);
        authService.logout(admin);

        assertThat(sessionRepository.session(tokenId).orElseThrow().getRevokedTime()).isNotNull();
        assertThat(operationLogRepository.appended())
                .filteredOn(entity -> "LOGOUT".equals(entity.getOperationType()))
                .hasSize(1);
    }

    @Test
    void logoutShouldReturnServiceUnavailableWhenSessionStoreBroken() {
        authService.login(new AdminLoginRequest(USERNAME, PASSWORD));
        String tokenId = sessionRepository.lastRegisteredTokenId();
        AdminAccessSessionRepository broken = new AdminAccessSessionRepository() {
            @Override
            public void register(com.foodmap.admin.infrastructure.persistence.entity.AdminAccessSessionEntity entity) {
                throw new DataAccessResourceFailureException("db down");
            }

            @Override
            public boolean revokeActive(String id, Long adminUserId, OffsetDateTime revokedTime) {
                throw new DataAccessResourceFailureException("db down");
            }

            @Override
            public Optional<com.foodmap.admin.application.port.AdminAuthorizationSnapshot> loadAuthorizationSnapshot(String id) {
                throw new DataAccessResourceFailureException("db down");
            }
        };
        authService = buildService(userRepository, broken);
        AuthenticatedAdmin admin = new AuthenticatedAdmin(adminUserId, tokenId, 1L, Set.of());

        assertThatThrownBy(() -> authService.logout(admin))
                .isInstanceOf(FoodMapException.class)
                .extracting(ex -> ((FoodMapException) ex).errorCode())
                .isEqualTo(CommonErrorCode.SERVICE_UNAVAILABLE);
    }

    /**
     * 停用当前测试管理员。
     */
    private void disableAccount() {
        AdminUserEntity entity = userRepository.findByUsername(USERNAME).orElseThrow();
        entity.setAdminStatus(AdminStatus.DISABLED.name());
    }
}
