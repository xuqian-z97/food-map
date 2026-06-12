package com.foodmap.admin.service;

import com.foodmap.admin.application.port.LogApiAccessLogClient;
import com.foodmap.admin.dto.AdminApiAccessLogQueryRequest;
import com.foodmap.admin.dto.AdminApiAccessLogResponse;
import com.foodmap.common.api.PageResponse;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminApiAccessLogQueryServiceTest {

    @Test
    void shouldDelegateApiAccessLogQueryToLogServiceClient() {
        CapturingClient client = new CapturingClient();
        AdminApiAccessLogQueryService service = new AdminApiAccessLogQueryService(client);
        AdminApiAccessLogQueryRequest request = new AdminApiAccessLogQueryRequest(
                " req-admin ",
                " trace-admin ",
                " foodmap-auth-service ",
                " INFO ",
                200,
                null,
                null,
                0,
                20
        );

        PageResponse<AdminApiAccessLogResponse> response = service.searchApiAccessLogs(request);

        assertThat(client.request).isSameAs(request);
        assertThat(response.total()).isEqualTo(1L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().requestId()).isEqualTo("req-admin");
        assertThat(response.items().getFirst().serviceName()).isEqualTo("foodmap-auth-service");
    }

    private static class CapturingClient implements LogApiAccessLogClient {
        private AdminApiAccessLogQueryRequest request;

        @Override
        public PageResponse<AdminApiAccessLogResponse> search(AdminApiAccessLogQueryRequest request) {
            this.request = request;
            return PageResponse.of(List.of(new AdminApiAccessLogResponse(
                    1001L,
                    "req-admin",
                    "trace-admin",
                    "span-admin",
                    "foodmap-auth-service",
                    "api.access.completed",
                    "INFO",
                    "POST",
                    "/api/auth/login",
                    200,
                    32L,
                    "192.168.*.*",
                    11L,
                    22L,
                    "auth-service",
                    null,
                    OffsetDateTime.parse("2026-06-13T01:00:00+08:00")
            )), 1L, 0, 20);
        }
    }
}
