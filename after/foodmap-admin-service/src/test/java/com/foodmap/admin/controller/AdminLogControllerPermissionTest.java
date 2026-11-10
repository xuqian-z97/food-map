package com.foodmap.admin.controller;

import com.foodmap.admin.dto.AdminApiAccessLogQueryRequest;
import com.foodmap.admin.dto.AdminApiAccessLogResponse;
import com.foodmap.admin.security.AdminPermissionGuard;
import com.foodmap.admin.security.AuthenticatedAdmin;
import com.foodmap.admin.service.AdminApiAccessLogQueryService;
import com.foodmap.common.api.PageResponse;
import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AdminLogControllerPermissionTest {

    @Test
    void shouldRejectApiAccessLogQueryWhenAdminContextMissing() {
        CapturingService service = new CapturingService();
        AdminLogController controller = new AdminLogController(service, new AdminPermissionGuard());
        HttpServletRequest request = mock(HttpServletRequest.class);

        assertThatThrownBy(() -> controller.searchApiAccessLogs(
                request, "req-1", null, null, null, null, null, null, 0, 20
        ))
                .isInstanceOf(FoodMapException.class)
                .extracting(ex -> ((FoodMapException) ex).errorCode())
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
        assertThat(service.called).isFalse();
    }

    @Test
    void shouldRejectApiAccessLogQueryWhenPermissionMissing() {
        CapturingService service = new CapturingService();
        AdminLogController controller = new AdminLogController(service, new AdminPermissionGuard());
        HttpServletRequest request = requestWithAdmin(adminWithPermissions(Set.of()));

        assertThatThrownBy(() -> controller.searchApiAccessLogs(
                request, "req-1", null, null, null, null, null, null, 0, 20
        ))
                .isInstanceOf(FoodMapException.class)
                .extracting(ex -> ((FoodMapException) ex).errorCode())
                .isEqualTo(CommonErrorCode.FORBIDDEN);
        assertThat(service.called).isFalse();
    }

    @Test
    void shouldAllowApiAccessLogQueryWhenPermissionPresent() {
        CapturingService service = new CapturingService();
        AdminLogController controller = new AdminLogController(service, new AdminPermissionGuard());
        HttpServletRequest request = requestWithAdmin(adminWithPermissions(Set.of("LOG_ACCESS_READ")));

        var response = controller.searchApiAccessLogs(
                request, "req-1", null, null, null, null, null, null, 0, 20
        );

        assertThat(response.success()).isTrue();
        assertThat(service.called).isTrue();
        assertThat(service.request.requestId()).isEqualTo("req-1");
    }

    /**
     * 客户端自带旧权限头不能影响判定：权限只来自服务端装配的上下文。
     */
    @Test
    void shouldIgnoreClientSuppliedAdminHeaders() {
        CapturingService service = new CapturingService();
        AdminLogController controller = new AdminLogController(service, new AdminPermissionGuard());
        HttpServletRequest request = requestWithAdmin(adminWithPermissions(Set.of()));

        assertThatThrownBy(() -> controller.searchApiAccessLogs(
                request, "req-1", null, null, null, null, null, null, 0, 20
        ))
                .isInstanceOf(FoodMapException.class)
                .extracting(ex -> ((FoodMapException) ex).errorCode())
                .isEqualTo(CommonErrorCode.FORBIDDEN);
        assertThat(service.called).isFalse();
    }

    private static AuthenticatedAdmin adminWithPermissions(Set<String> permissions) {
        return new AuthenticatedAdmin(1001L, "token-id-1", 1L, permissions);
    }

    private static HttpServletRequest requestWithAdmin(AuthenticatedAdmin admin) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        org.mockito.Mockito.when(request.getAttribute("authenticatedAdmin")).thenReturn(admin);
        return request;
    }

    private static class CapturingService extends AdminApiAccessLogQueryService {
        private boolean called;
        private AdminApiAccessLogQueryRequest request;

        CapturingService() {
            super(queryRequest -> PageResponse.of(List.of(), 0, 0, 20));
        }

        @Override
        public PageResponse<AdminApiAccessLogResponse> searchApiAccessLogs(AdminApiAccessLogQueryRequest request) {
            this.called = true;
            this.request = request;
            return PageResponse.of(List.of(), 0, 0, 20);
        }
    }
}
