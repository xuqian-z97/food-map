package com.foodmap.admin.security;

import com.foodmap.admin.service.AdminAuthorizationService;
import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Set;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 后台认证拦截器测试：凭证提取、验签装配与预检请求豁免。
 */
class AdminAuthInterceptorTest {
    private static final String TOKEN = "admin-jwt-value";

    @Test
    void preHandleShouldPassOptionsRequestWithoutToken() {
        RecordingVerifier verifier = new RecordingVerifier();
        AdminAuthInterceptor interceptor = new AdminAuthInterceptor(verifier, new FixedAuthorizationService());
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/admin/logs/api-access");

        boolean passed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(passed).isTrue();
        assertThat(verifier.invoked).isFalse();
    }

    @Test
    void preHandleShouldRejectMissingBearerCredential() {
        AdminAuthInterceptor interceptor = new AdminAuthInterceptor(new RecordingVerifier(), new FixedAuthorizationService());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/logs/api-access");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(FoodMapException.class)
                .extracting(ex -> ((FoodMapException) ex).errorCode())
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }

    @Test
    void preHandleShouldRejectNonBearerOrBlankOrDuplicatedCredential() {
        AdminAuthInterceptor interceptor = new AdminAuthInterceptor(new RecordingVerifier(), new FixedAuthorizationService());

        MockHttpServletRequest basicAuth = new MockHttpServletRequest("GET", "/api/admin/logs/api-access");
        basicAuth.addHeader("Authorization", "Basic abc");
        assertThatThrownBy(() -> interceptor.preHandle(basicAuth, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(FoodMapException.class);

        MockHttpServletRequest blank = new MockHttpServletRequest("GET", "/api/admin/logs/api-access");
        blank.addHeader("Authorization", "Bearer   ");
        assertThatThrownBy(() -> interceptor.preHandle(blank, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(FoodMapException.class);

        MockHttpServletRequest duplicated = new MockHttpServletRequest("GET", "/api/admin/logs/api-access");
        duplicated.addHeader("Authorization", "Bearer " + TOKEN);
        duplicated.addHeader("Authorization", "Bearer " + TOKEN);
        assertThatThrownBy(() -> interceptor.preHandle(duplicated, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(FoodMapException.class);
    }

    @Test
    void preHandleShouldVerifyTokenAndAttachAuthenticatedAdmin() {
        RecordingVerifier verifier = new RecordingVerifier();
        AdminAuthInterceptor interceptor = new AdminAuthInterceptor(verifier, new FixedAuthorizationService());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/logs/api-access");
        request.addHeader("Authorization", "Bearer " + TOKEN);

        boolean passed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(passed).isTrue();
        assertThat(verifier.invoked).isTrue();
        AuthenticatedAdmin admin =
                (AuthenticatedAdmin) request.getAttribute(AdminAuthInterceptor.ATTR_AUTHENTICATED_ADMIN);
        assertThat(admin).isNotNull();
        assertThat(admin.permissions()).containsExactly("LOG_ACCESS_READ");
    }

    /**
     * 固定返回验签通过的验签器替身。
     */
    private static final class RecordingVerifier extends AdminTokenVerifier {
        private boolean invoked;

        RecordingVerifier() {
            super(testPublicKey());
        }

        @Override
        public VerifiedAdminToken verify(String token) {
            invoked = true;
            if (!TOKEN.equals(token)) {
                throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "无效的后台访问令牌");
            }
            return new VerifiedAdminToken(1001L, 1L, "token-id-1",
                    java.time.OffsetDateTime.now().plusMinutes(5));
        }

        private static java.security.interfaces.RSAPublicKey testPublicKey() {
            try {
                java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                return (java.security.interfaces.RSAPublicKey) generator.generateKeyPair().getPublic();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    /**
     * 固定返回已授权管理员上下文的授权服务替身。
     */
    private static final class FixedAuthorizationService implements AdminAuthorizationService {
        @Override
        public AuthenticatedAdmin authenticate(VerifiedAdminToken token) {
            return new AuthenticatedAdmin(token.adminUserId(), token.tokenId(),
                    token.permissionVersion(), Set.of("LOG_ACCESS_READ"));
        }
    }
}
