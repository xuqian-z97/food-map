package com.foodmap.log.service;

import com.foodmap.common.api.PageResponse;
import com.foodmap.log.application.port.ApiAccessLogQueryCriteria;
import com.foodmap.log.application.port.ApiAccessLogRepository;
import com.foodmap.log.dto.ApiAccessLogQueryRequest;
import com.foodmap.log.dto.ApiAccessLogResponse;
import com.foodmap.log.infrastructure.persistence.entity.ApiAccessLogEntity;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiAccessLogQueryServiceTest {

    @Test
    void shouldNormalizeQueryAndReturnPagedApiAccessLogs() {
        CapturingRepository repository = new CapturingRepository();
        ApiAccessLogQueryService service = new ApiAccessLogQueryService(repository);
        ApiAccessLogQueryRequest request = new ApiAccessLogQueryRequest(
                " req-1 ",
                null,
                " foodmap-auth-service ",
                " INFO ",
                200,
                null,
                null,
                -1,
                500
        );

        PageResponse<ApiAccessLogResponse> response = service.search(request);

        assertThat(repository.criteria.requestId()).isEqualTo("req-1");
        assertThat(repository.criteria.serviceName()).isEqualTo("foodmap-auth-service");
        assertThat(repository.criteria.logLevel()).isEqualTo("INFO");
        assertThat(repository.criteria.pageIndex()).isZero();
        assertThat(repository.criteria.pageSize()).isEqualTo(100);
        assertThat(repository.criteria.offset()).isZero();
        assertThat(response.total()).isEqualTo(1L);
        assertThat(response.pageIndex()).isZero();
        assertThat(response.pageSize()).isEqualTo(100);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().requestId()).isEqualTo("req-1");
        assertThat(response.items().getFirst().requestPath()).isEqualTo("/api/auth/login");
    }

    private static class CapturingRepository implements ApiAccessLogRepository {
        private ApiAccessLogQueryCriteria criteria;

        @Override
        public int saveIgnoreDuplicate(ApiAccessLogEntity entity) {
            return 0;
        }

        @Override
        public int deleteOccurredBefore(OffsetDateTime cutoffTime) {
            return 0;
        }

        @Override
        public List<ApiAccessLogEntity> search(ApiAccessLogQueryCriteria criteria) {
            this.criteria = criteria;
            ApiAccessLogEntity entity = new ApiAccessLogEntity();
            entity.setAccessLogId(101L);
            entity.setRequestId("req-1");
            entity.setTraceId("trace-1");
            entity.setSpanId("span-1");
            entity.setServiceName("foodmap-auth-service");
            entity.setEventName("api.access.completed");
            entity.setLogLevel("INFO");
            entity.setHttpMethod("POST");
            entity.setRequestPath("/api/auth/login");
            entity.setHttpStatus(200);
            entity.setDurationMs(43L);
            entity.setClientIpMasked("192.168.*.*");
            entity.setAccountId(1001L);
            entity.setUserId(2001L);
            entity.setGatewayRouteId("auth-service");
            entity.setErrorCode(null);
            entity.setOccurredTime(OffsetDateTime.parse("2026-06-13T01:00:00+08:00"));
            return List.of(entity);
        }

        @Override
        public long count(ApiAccessLogQueryCriteria criteria) {
            return 1L;
        }
    }
}
