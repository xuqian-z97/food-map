package com.foodmap.admin.controller;

import com.foodmap.admin.dto.AdminApiAccessLogQueryRequest;
import com.foodmap.admin.dto.AdminApiAccessLogResponse;
import com.foodmap.admin.security.AdminPermissionGuard;
import com.foodmap.admin.service.AdminApiAccessLogQueryService;
import com.foodmap.common.api.PageResponse;
import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminLogControllerPermissionTest {

    @Test
    void shouldRejectApiAccessLogQueryWhenAdminIdentityMissing() {
        CapturingService service = new CapturingService();
        AdminLogController controller = new AdminLogController(service, new AdminPermissionGuard());

        assertThatThrownBy(() -> controller.searchApiAccessLogs(
                null,
                "LOG_ACCESS_READ",
                "req-1",
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                20
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

        assertThatThrownBy(() -> controller.searchApiAccessLogs(
                "1001",
                "CONTENT_REVIEW_READ",
                "req-1",
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                20
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

        var response = controller.searchApiAccessLogs(
                "1001",
                "CONTENT_REVIEW_READ, LOG_ACCESS_READ",
                "req-1",
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                20
        );

        assertThat(response.success()).isTrue();
        assertThat(service.called).isTrue();
        assertThat(service.request.requestId()).isEqualTo("req-1");
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
