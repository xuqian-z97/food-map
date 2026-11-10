package com.foodmap.admin.service.impl;

import com.foodmap.admin.application.port.AdminAccessSessionRepository;
import com.foodmap.admin.application.port.AdminAuthorizationSnapshot;
import com.foodmap.admin.infrastructure.persistence.entity.AdminAccessSessionEntity;
import com.foodmap.admin.security.AuthenticatedAdmin;
import com.foodmap.admin.security.VerifiedAdminToken;
import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 后台认证授权服务测试：会话有效性、管理员状态、权限版本与 401/403/503 语义。
 */
class AdminAuthorizationServiceImplTest {
    private static final String TOKEN_ID = "0b2a6d0e-5c9f-4a1e-8f0e-2f4c9e1a7b3d";

    @Test
    void authenticateShouldReturnAdminWithDatabasePermissions() {
        FakeAdminAccessSessionRepository repository = new FakeAdminAccessSessionRepository();
        repository.putSnapshot(TOKEN_ID,
                FakeAdminAccessSessionRepository.activeSnapshot(1001L, 2L, Set.of("LOG_ACCESS_READ")));
        AdminAuthorizationServiceImpl service = new AdminAuthorizationServiceImpl(repository);

        AuthenticatedAdmin admin = service.authenticate(token(1001L, 2L));

        assertThat(admin.adminUserId()).isEqualTo(1001L);
        assertThat(admin.tokenId()).isEqualTo(TOKEN_ID);
        assertThat(admin.permissionVersion()).isEqualTo(2L);
        assertThat(admin.permissions()).containsExactly("LOG_ACCESS_READ");
    }

    @Test
    void authenticateShouldKeepSessionValidWithEmptyPermissions() {
        FakeAdminAccessSessionRepository repository = new FakeAdminAccessSessionRepository();
        repository.putSnapshot(TOKEN_ID, FakeAdminAccessSessionRepository.activeSnapshot(1001L, 1L, Set.of()));
        AdminAuthorizationServiceImpl service = new AdminAuthorizationServiceImpl(repository);

        AuthenticatedAdmin admin = service.authenticate(token(1001L, 1L));

        assertThat(admin).isNotNull();
        assertThat(admin.permissions()).isEmpty();
    }

    @Test
    void authenticateShouldRejectMissingSession() {
        AdminAuthorizationServiceImpl service = new AdminAuthorizationServiceImpl(new FakeAdminAccessSessionRepository());

        assertThatThrownBy(() -> service.authenticate(token(1001L, 1L)))
                .isInstanceOf(FoodMapException.class)
                .extracting(ex -> ((FoodMapException) ex).errorCode())
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }

    @Test
    void authenticateShouldRejectRevokedExpiredOrMismatchedSession() {
        assertRejected(new AdminAuthorizationSnapshot(1001L, "ACTIVE", 1L,
                OffsetDateTime.now().plusMinutes(5), OffsetDateTime.now().minusMinutes(1), Set.of()));
        assertRejected(new AdminAuthorizationSnapshot(1001L, "ACTIVE", 1L,
                OffsetDateTime.now().minusMinutes(1), null, Set.of()));
        assertRejected(new AdminAuthorizationSnapshot(2002L, "ACTIVE", 1L,
                OffsetDateTime.now().plusMinutes(5), null, Set.of()));
        assertRejected(new AdminAuthorizationSnapshot(1001L, "DISABLED", 1L,
                OffsetDateTime.now().plusMinutes(5), null, Set.of()));
    }

    @Test
    void authenticateShouldRejectStalePermissionVersion() {
        FakeAdminAccessSessionRepository repository = new FakeAdminAccessSessionRepository();
        repository.putSnapshot(TOKEN_ID,
                FakeAdminAccessSessionRepository.activeSnapshot(1001L, 7L, Set.of("LOG_ACCESS_READ")));
        AdminAuthorizationServiceImpl service = new AdminAuthorizationServiceImpl(repository);

        assertThatThrownBy(() -> service.authenticate(token(1001L, 3L)))
                .isInstanceOf(FoodMapException.class)
                .hasMessageContaining("重新登录")
                .extracting(ex -> ((FoodMapException) ex).errorCode())
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }

    @Test
    void authenticateShouldReturnServiceUnavailableWhenStoreBroken() {
        AdminAccessSessionRepository broken = new AdminAccessSessionRepository() {
            @Override
            public void register(AdminAccessSessionEntity entity) {
                throw new DataAccessResourceFailureException("db down");
            }

            @Override
            public boolean revokeActive(String tokenId, Long adminUserId, OffsetDateTime revokedTime) {
                throw new DataAccessResourceFailureException("db down");
            }

            @Override
            public Optional<AdminAuthorizationSnapshot> loadAuthorizationSnapshot(String tokenId) {
                throw new DataAccessResourceFailureException("db down");
            }
        };
        AdminAuthorizationServiceImpl service = new AdminAuthorizationServiceImpl(broken);

        assertThatThrownBy(() -> service.authenticate(token(1001L, 1L)))
                .isInstanceOf(FoodMapException.class)
                .extracting(ex -> ((FoodMapException) ex).errorCode())
                .isEqualTo(CommonErrorCode.SERVICE_UNAVAILABLE);
    }

    /**
     * 断言指定快照会被 401 拒绝。
     *
     * @param snapshot 快照。
     */
    private static void assertRejected(AdminAuthorizationSnapshot snapshot) {
        FakeAdminAccessSessionRepository repository = new FakeAdminAccessSessionRepository();
        repository.putSnapshot(TOKEN_ID, snapshot);
        AdminAuthorizationServiceImpl service = new AdminAuthorizationServiceImpl(repository);

        assertThatThrownBy(() -> service.authenticate(token(1001L, snapshot.permissionVersion())))
                .isInstanceOf(FoodMapException.class)
                .extracting(ex -> ((FoodMapException) ex).errorCode())
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }

    /**
     * 构造验签通过的 Token 事实。
     *
     * @param adminUserId 后台管理员业务主键。
     * @param permissionVersion 签发时权限版本。
     * @return Token 事实。
     */
    private static VerifiedAdminToken token(long adminUserId, long permissionVersion) {
        return new VerifiedAdminToken(adminUserId, permissionVersion, TOKEN_ID,
                OffsetDateTime.now().plusMinutes(5));
    }
}
