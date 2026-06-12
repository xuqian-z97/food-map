# B1.5 Logging Platform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build FoodMap backend logging in staged slices: B1.5-a creates request trace context and access-log foundation; B1.5-b connects the foundation to Kafka, Elasticsearch, log PostgreSQL, OSS archive, and admin query APIs.

**Architecture:** B1.5-a keeps runtime dependencies light. `foodmap-common` owns shared servlet logging primitives and auto-configuration, while `foodmap-gateway-service` owns WebFlux gateway trace header generation and propagation. B1.5-b adds external log infrastructure without changing business service logging call sites.

**Tech Stack:** Java 21, Spring Boot Servlet filters, Spring Cloud Gateway WebFlux filters, SLF4J MDC, Maven, Mermaid documentation.

**Documentation Rule:** Every B1.5 sub-stage must update `CODEX-after.md` with four parts before moving to the next sub-stage: key technologies, implementation principles, Mermaid architecture/flow diagram, and operational/risk notes. The same sub-stage must also update this plan checklist with files changed and verification commands.

---

### Task 1: B1.5-a Common Trace Context

**Files:**
- Create: `after/foodmap-common/src/main/java/com/foodmap/common/logging/LogMdcKeys.java`
- Create: `after/foodmap-common/src/main/java/com/foodmap/common/logging/TraceHeaders.java`
- Create: `after/foodmap-common/src/main/java/com/foodmap/common/logging/TraceIdGenerator.java`
- Create: `after/foodmap-common/src/main/java/com/foodmap/common/logging/LogContext.java`
- Test: `after/foodmap-common/src/test/java/com/foodmap/common/logging/LogContextTest.java`

- [x] **Step 1: Write failing tests**

```java
@Test
void shouldUseValidIncomingRequestAndTraceIds() {
    LogContext context = LogContext.fromIncoming("req-123", "trace-456", "foodmap-auth-service", "1001", "2001");
    assertThat(context.requestId()).isEqualTo("req-123");
    assertThat(context.traceId()).isEqualTo("trace-456");
}

@Test
void shouldGenerateIdsWhenIncomingValuesAreUnsafe() {
    LogContext context = LogContext.fromIncoming("../bad", "", "foodmap-auth-service", null, null);
    assertThat(context.requestId()).matches("[A-Za-z0-9_.-]{16,64}");
    assertThat(context.traceId()).matches("[A-Za-z0-9_.-]{16,64}");
}
```

- [x] **Step 2: Run red test**

Run: `cd after && mvn -pl foodmap-common -Dtest=LogContextTest test`
Expected: compilation failure because `LogContext` does not exist.

- [x] **Step 3: Implement minimal common trace context**

Add stable header names, MDC keys, safe ID generation, incoming ID validation, and MDC population/clear helpers.

- [x] **Step 4: Run green test**

Run: `cd after && mvn -pl foodmap-common -Dtest=LogContextTest test`
Expected: `LogContextTest` passes.

### Task 2: B1.5-a Servlet MDC and Access Log Filters

**Files:**
- Create: `after/foodmap-common/src/main/java/com/foodmap/common/logging/LogMdcFilter.java`
- Create: `after/foodmap-common/src/main/java/com/foodmap/common/logging/ApiAccessLogFilter.java`
- Create: `after/foodmap-common/src/main/java/com/foodmap/common/logging/FoodMapLoggingProperties.java`
- Create: `after/foodmap-common/src/main/java/com/foodmap/common/logging/FoodMapLoggingAutoConfiguration.java`
- Modify: `after/foodmap-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `after/foodmap-common/src/test/java/com/foodmap/common/logging/LogMdcFilterTest.java`

- [x] **Step 1: Write failing filter test**

```java
@Test
void shouldPutTraceFieldsIntoMdcDuringServletRequestAndClearAfterwards() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/auth/health");
    request.addHeader(TraceHeaders.REQUEST_ID, "req-abc");
    request.addHeader(TraceHeaders.TRACE_ID, "trace-abc");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> requestIdDuringChain = new AtomicReference<>();

    new LogMdcFilter("foodmap-auth-service").doFilter(request, response, (servletRequest, servletResponse) -> {
        requestIdDuringChain.set(MDC.get(LogMdcKeys.REQUEST_ID));
    });

    assertThat(requestIdDuringChain.get()).isEqualTo("req-abc");
    assertThat(response.getHeader(TraceHeaders.REQUEST_ID)).isEqualTo("req-abc");
    assertThat(MDC.get(LogMdcKeys.REQUEST_ID)).isNull();
}
```

- [x] **Step 2: Run red test**

Run: `cd after && mvn -pl foodmap-common -Dtest=LogMdcFilterTest test`
Expected: compilation failure because filter classes do not exist.

- [x] **Step 3: Implement servlet filters and auto-configuration**

`LogMdcFilter` creates/propagates context. `ApiAccessLogFilter` logs completed or slow requests with `SafeLog`. Auto-configuration registers both for Servlet applications.

- [x] **Step 4: Run green test**

Run: `cd after && mvn -pl foodmap-common -Dtest='LogContextTest,LogMdcFilterTest,SafeLogTest,LogMaskerTest' test`
Expected: selected logging tests pass.

### Task 3: B1.5-a Gateway Trace Filter

**Files:**
- Create: `after/foodmap-gateway-service/src/main/java/com/foodmap/gateway/filter/GatewayTraceFilter.java`
- Test: `after/foodmap-gateway-service/src/test/java/com/foodmap/gateway/filter/GatewayTraceFilterTest.java`

- [x] **Step 1: Write failing gateway test**

```java
@Test
void shouldGenerateTraceHeadersForGatewayRequestAndForwardDownstream() {
    GatewayTraceFilter filter = new GatewayTraceFilter("foodmap-gateway-service");
    MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/me").build());
    AtomicReference<ServerWebExchange> downstream = new AtomicReference<>();

    filter.filter(exchange, chainExchange -> {
        downstream.set(chainExchange);
        return Mono.empty();
    }).block();

    assertThat(downstream.get().getRequest().getHeaders().getFirst(TraceHeaders.REQUEST_ID)).isNotBlank();
    assertThat(downstream.get().getRequest().getHeaders().getFirst(TraceHeaders.TRACE_ID)).isNotBlank();
    assertThat(exchange.getResponse().getHeaders().getFirst(TraceHeaders.REQUEST_ID)).isNotBlank();
}
```

- [x] **Step 2: Run red test**

Run: `cd after && mvn -pl foodmap-gateway-service -am -Dtest=GatewayTraceFilterTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: compilation failure because `GatewayTraceFilter` does not exist.

- [x] **Step 3: Implement gateway trace filter**

Use `LogContext` to validate or generate IDs, mutate request headers, set response headers, and log gateway access completion.

- [x] **Step 4: Run green test**

Run: `cd after && mvn -pl foodmap-gateway-service -am -Dtest='GatewayTraceFilterTest,GatewayAuthFilterTest' -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: gateway tests pass.

### Task 4: B1.5-a Documentation Sync

**Files:**
- Modify: `CODEX-after.md`
- Modify: `CODEX-gen.md`
- Modify: `AGENTS.md`
- Optional Modify: `after/README.md`

- [x] **Step 1: Add architecture Mermaid diagram**

Insert a `flowchart LR` diagram showing client, gateway trace filter, servlet MDC/access filters, service logs, and B1.5-b log platform sinks.

- [x] **Step 2: Add implementation principle text**

Document that B1.5-a only guarantees in-process trace context and access-log foundation, while B1.5-b owns Kafka/Elasticsearch/log PostgreSQL/OSS/admin query implementation.

- [x] **Step 3: Run docs verification**

Run: `./harness/scripts/validate-docs.sh`
Expected: documentation structure passes.

### Task 5: Final Verification

**Files:**
- No new files.

- [x] **Step 1: Run focused tests**

Run: `cd after && mvn -pl foodmap-common,foodmap-gateway-service -am -Dtest='LogContextTest,LogMdcFilterTest,SafeLogTest,LogMaskerTest,GatewayTraceFilterTest,GatewayAuthFilterTest,!RedissonDistributedLockClientTest' test`
Expected: selected tests pass.

- [x] **Step 2: Run harness**

Run: `./harness/scripts/run-all.sh`
Expected: harness passes; git check may warn about uncommitted changes.

- [x] **Step 3: Run whitespace check**

Run: `git diff --check`
Expected: no output.

### B1.5-b Follow-up Scope

B1.5-b starts after B1.5-a lands. It will add SQL logging controls, Kafka/log collector Docker Compose, Elasticsearch index templates, independent log PostgreSQL `api_access_log` and `audit_log`, OSS archive jobs, and admin `/api/admin/logs/**` query APIs.

### B1.5-b Task 1: MyBatis SQL Logging Foundation

**Files:**
- Modify: `after/foodmap-common/pom.xml`
- Modify: `after/foodmap-common/src/main/java/com/foodmap/common/logging/SafeLog.java`
- Modify: `after/foodmap-common/src/main/java/com/foodmap/common/logging/FoodMapLoggingProperties.java`
- Create: `after/foodmap-common/src/main/java/com/foodmap/common/logging/mybatis/SqlParameterValue.java`
- Create: `after/foodmap-common/src/main/java/com/foodmap/common/logging/mybatis/SqlLogFormatter.java`
- Create: `after/foodmap-common/src/main/java/com/foodmap/common/logging/mybatis/SqlLogPolicy.java`
- Create: `after/foodmap-common/src/main/java/com/foodmap/common/logging/mybatis/SqlLogConfigProvider.java`
- Create: `after/foodmap-common/src/main/java/com/foodmap/common/logging/mybatis/EnvironmentSqlLogConfigProvider.java`
- Create: `after/foodmap-common/src/main/java/com/foodmap/common/logging/mybatis/StaticSqlLogConfigProvider.java`
- Create: `after/foodmap-common/src/main/java/com/foodmap/common/logging/mybatis/MyBatisSqlLogInterceptor.java`
- Create: `after/foodmap-common/src/main/java/com/foodmap/common/logging/mybatis/FoodMapMyBatisLoggingAutoConfiguration.java`
- Modify: `after/foodmap-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `after/foodmap-common/src/test/java/com/foodmap/common/logging/mybatis/SqlLogFormatterTest.java`
- Test: `after/foodmap-common/src/test/java/com/foodmap/common/logging/mybatis/SqlLogPolicyTest.java`
- Test: `after/foodmap-common/src/test/java/com/foodmap/common/logging/mybatis/EnvironmentSqlLogConfigProviderTest.java`

- [x] **Step 1: Write failing formatter and policy tests**
- [x] **Step 2: Run red tests**
- [x] **Step 3: Implement formatter, policy, interceptor, and auto-configuration**
- [x] **Step 4: Run green tests**
- [x] **Step 5: Run focused backend verification**
- [x] **Step 6: Document key technologies, implementation principles, and Mermaid SQL execution flow**

### B1.5-b Task 2: SQL Dynamic Configuration Read Layer

**Files:**
- Create: `after/foodmap-common/src/test/java/com/foodmap/common/logging/mybatis/EnvironmentSqlLogConfigProviderTest.java`
- Create: `after/foodmap-common/src/main/java/com/foodmap/common/logging/mybatis/SqlLogConfigProvider.java`
- Create: `after/foodmap-common/src/main/java/com/foodmap/common/logging/mybatis/EnvironmentSqlLogConfigProvider.java`
- Create: `after/foodmap-common/src/main/java/com/foodmap/common/logging/mybatis/StaticSqlLogConfigProvider.java`
- Modify: `after/foodmap-common/src/main/java/com/foodmap/common/logging/mybatis/MyBatisSqlLogInterceptor.java`
- Modify: `after/foodmap-common/src/main/java/com/foodmap/common/logging/mybatis/FoodMapMyBatisLoggingAutoConfiguration.java`

- [x] **Step 1: Write failing Environment provider tests**
- [x] **Step 2: Run red tests**
- [x] **Step 3: Implement Environment provider and rewire interceptor**
- [x] **Step 4: Run green tests**
- [x] **Step 5: Document key technologies, implementation principles, and Mermaid dynamic configuration flow**

### B1.5-b Task 3: Nacos Config Import and Logging Template

**Files:**
- Modify: `after/*/pom.xml`
- Modify: `after/*/src/main/resources/application.yml`
- Create: `deploy/nacos/foodmap-logging-template.yml`
- Modify: `deploy/env.example`
- Modify: `deploy/README.md`
- Modify: `CODEX-after.md`
- Modify: `CODEX-gen.md`
- Modify: `after/README.md`
- Modify: `AGENTS.md`

- [x] **Step 1: Add Nacos Config starter to backend services**
- [x] **Step 2: Add optional Nacos config imports for shared logging and service config**
- [x] **Step 3: Add shared logging config template**
- [x] **Step 4: Sync deployment and architecture docs**
- [x] **Step 5: Document key technologies, implementation principles, and Mermaid Nacos import flow**

### B1.5-b Task 4: Kafka Logging Buffer for Local Compose

**Files:**
- Modify: `deploy/docker-compose.dev.yml`
- Modify: `deploy/env.example`
- Modify: `deploy/dev-env/README.md`
- Modify: `CODEX-after.md`
- Modify: `CODEX-gen.md`
- Modify: `docs/superpowers/plans/2026-06-11-b15-logging-platform.md`

- [x] **Key technologies:** Docker Compose profile, Kafka KRaft, topic initialization container.
- [x] **Implementation principles:** Kafka is the logging buffer; services do not directly depend on Kafka SDK in this task.
- [x] **Diagram:** add Mermaid local Kafka logging buffer flow.
- [x] **Operational notes:** document profile startup, ports, topic names, partitions, retention, and fallback principle.
- [x] **Verification:** run compose config, docs validation, and whitespace check.

Verification commands:

```sh
docker compose -f deploy/docker-compose.dev.yml --profile logging config
./harness/scripts/validate-docs.sh
git diff --check
./harness/scripts/run-all.sh
mvn -pl foodmap-common -Dtest=LogContextTest,LogMdcFilterTest,EnvironmentSqlLogConfigProviderTest,SqlLogFormatterTest,SqlLogPolicyTest test
mvn -pl foodmap-common,foodmap-gateway-service -am -Dtest=GatewayTraceFilterTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test
```

Note: `mvn -pl foodmap-common,foodmap-gateway-service -am test` still fails in the pre-existing Redisson Mockito inline test because the current Oracle JDK 21 runtime cannot self-attach Byte Buddy. The B1.5 logging-focused tests above pass.

### B1.5-b Task 5: Fluent Bit Local Collector

**Files:**
- Add: `deploy/logging/fluent-bit/fluent-bit.conf`
- Add: `deploy/logging/fluent-bit/parsers.conf`
- Add: `harness/scripts/validate-logging.sh`
- Modify: `harness/scripts/run-all.sh`
- Modify: `deploy/docker-compose.dev.yml`
- Modify: `deploy/env.example`
- Modify: `deploy/dev-env/README.md`
- Modify: `deploy/README.md`
- Modify: `CODEX-after.md`
- Modify: `CODEX-gen.md`
- Modify: `after/README.md`
- Modify: `docs/superpowers/plans/2026-06-11-b15-logging-platform.md`

- [x] **Key technologies:** Fluent Bit tail input, parser filter, rewrite_tag filter, Kafka output, Docker Compose `logging` profile, shared app log volume.
- [x] **Implementation principles:** business services keep local logs; collector reads `/foodmap/logs/*.log`, classifies by event-name prefix, and writes to Kafka topics without business code depending on Kafka SDK.
- [x] **Diagram:** add Mermaid Fluent Bit collector flow in `CODEX-after.md`.
- [x] **Operational notes:** document startup command, topic routing, shared volume requirement, and fallback principle.
- [x] **Verification:** run RED/green logging validation, compose config, docs validation, whitespace check, and harness.

Verification commands:

```sh
sh harness/scripts/validate-logging.sh
docker compose -f deploy/docker-compose.dev.yml --profile logging config
./harness/scripts/validate-docs.sh
git diff --check
./harness/scripts/run-all.sh
```

TDD note: `sh harness/scripts/validate-logging.sh` first failed with missing `deploy/logging/fluent-bit/fluent-bit.conf`, then passed after adding the collector config and Compose service.

### B1.5-b Task 6: Elasticsearch Hot Log Store

**Files:**
- Add: `deploy/logging/elasticsearch/foodmap-logs-ilm-policy.json`
- Add: `deploy/logging/elasticsearch/foodmap-logs-index-template.json`
- Modify: `deploy/docker-compose.dev.yml`
- Modify: `deploy/env.example`
- Modify: `deploy/dev-env/README.md`
- Modify: `deploy/README.md`
- Modify: `harness/scripts/validate-logging.sh`
- Modify: `CODEX-after.md`
- Modify: `CODEX-gen.md`
- Modify: `after/README.md`
- Modify: `AGENTS.md`
- Modify: `docs/superpowers/plans/2026-06-11-b15-logging-platform.md`

- [x] **Key technologies:** Elasticsearch single-node local service, `curlimages/curl` init container, ILM policy, index template, Docker Compose `logging` profile.
- [x] **Implementation principles:** Elasticsearch owns 7-day hot query storage; index template fixes query-critical field types; Kafka-to-Elasticsearch consumer is explicitly deferred to a later B1.5-b task.
- [x] **Diagram:** add Mermaid Elasticsearch hot query foundation flow in `CODEX-after.md`.
- [x] **Operational notes:** document startup command, `localhost:9200`, local security setting, ILM retention, and production archive-before-delete risk.
- [x] **Verification:** run RED/green logging validation, compose config, JSON validation, docs validation, whitespace check, and harness.

Verification commands:

```sh
sh harness/scripts/validate-logging.sh
docker compose -f deploy/docker-compose.dev.yml --profile logging config
jq empty deploy/logging/elasticsearch/foodmap-logs-ilm-policy.json deploy/logging/elasticsearch/foodmap-logs-index-template.json
./harness/scripts/validate-docs.sh
git diff --check
./harness/scripts/run-all.sh
```

TDD note: `sh harness/scripts/validate-logging.sh` first failed with missing `deploy/logging/elasticsearch/foodmap-logs-index-template.json`, then passed after adding the Elasticsearch template and Compose services.

### B1.5-b Task 7: Kafka to Elasticsearch Consumer

**Files:**
- Add: `deploy/logging/logstash/config/logstash.yml`
- Add: `deploy/logging/logstash/pipeline/foodmap-logs.conf`
- Modify: `deploy/docker-compose.dev.yml`
- Modify: `deploy/env.example`
- Modify: `deploy/dev-env/README.md`
- Modify: `deploy/README.md`
- Modify: `harness/scripts/validate-logging.sh`
- Modify: `CODEX-after.md`
- Modify: `CODEX-gen.md`
- Modify: `after/README.md`
- Modify: `AGENTS.md`
- Modify: `docs/superpowers/plans/2026-06-11-b15-logging-platform.md`

- [x] **Key technologies:** Logstash Kafka input, filter stage, Elasticsearch output, Docker Compose `logging` profile, daily `foodmap-logs-*` indexes.
- [x] **Implementation principles:** Logstash consumes five Kafka log topics, derives `logType` from topic, normalizes `eventName` and `@timestamp`, and writes to Elasticsearch without business services depending on Kafka or Elasticsearch SDKs.
- [x] **Diagram:** add Mermaid Kafka-to-Elasticsearch consumer flow in `CODEX-after.md`.
- [x] **Operational notes:** document full startup command, consumer group, local JVM settings, retry/fallback principle, and production TLS/auth requirements.
- [x] **Verification:** run RED/green logging validation, compose config, Logstash config presence checks, docs validation, whitespace check, and harness.

Verification commands:

```sh
sh harness/scripts/validate-logging.sh
docker compose -f deploy/docker-compose.dev.yml --profile logging config
rg -n "input \\{|kafka \\{|foodmap.logs.application|foodmap.logs.api-access|foodmap.logs.sql|foodmap.logs.audit|foodmap.logs.security|elasticsearch \\{|foodmap-logs-%\\{\\+YYYY\\.MM\\.dd\\}" deploy/logging/logstash/pipeline/foodmap-logs.conf
./harness/scripts/validate-docs.sh
git diff --check
./harness/scripts/run-all.sh
```

TDD note: `sh harness/scripts/validate-logging.sh` first failed with missing `deploy/logging/logstash/pipeline/foodmap-logs.conf`, then passed after adding the Logstash pipeline and Compose service.

### B1.5-b Task 8: Log PostgreSQL API Access Schema

**Files:**
- Add: `deploy/logging/postgres/migration/V1__create_api_access_log.sql`
- Modify: `deploy/postgres/init/01-create-foodmap-databases.sql`
- Modify: `deploy/docker-compose.dev.yml`
- Modify: `deploy/env.example`
- Modify: `deploy/dev-env/README.md`
- Modify: `deploy/README.md`
- Modify: `harness/scripts/validate-logging.sh`
- Modify: `CODEX-after.md`
- Modify: `CODEX-gen.md`
- Modify: `after/README.md`
- Modify: `AGENTS.md`
- Modify: `docs/superpowers/plans/2026-06-11-b15-logging-platform.md`

- [x] **Key technologies:** PostgreSQL independent log database, Flyway migration container, `api_access_log` summary table, Kafka offset idempotency constraint, query indexes.
- [x] **Implementation principles:** full logs stay in Elasticsearch/OSS; `foodmap_log_db.api_access_log` stores only 15-day API access summaries and source Kafka offsets for later idempotent ingestion.
- [x] **Diagram:** add Mermaid independent log PostgreSQL schema flow in `CODEX-after.md`.
- [x] **Operational notes:** document database creation, Flyway migration command, existing-volume caveat, 15-day retention, and no-full-log-in-admin-db rule.
- [x] **Verification:** run RED/green logging validation, compose config, SQL content checks, docs validation, whitespace check, and harness.

Verification commands:

```sh
sh harness/scripts/validate-logging.sh
docker compose -f deploy/docker-compose.dev.yml --profile logging config
rg -n "create table if not exists api_access_log|comment on table api_access_log|request_id|trace_id|service_name|duration_ms|source_topic|source_partition|source_offset" deploy/logging/postgres/migration/V1__create_api_access_log.sql
./harness/scripts/validate-docs.sh
git diff --check
./harness/scripts/run-all.sh
```

TDD note: `sh harness/scripts/validate-logging.sh` first failed because `foodmap_log_db` was missing from the PostgreSQL init script, then passed after adding the log database, Flyway migration, and `log-postgres-migrate` Compose service.

### B1.5-b Task 9: API Access Kafka Consumer Service

**Files:**
- Add: `after/foodmap-log-service/pom.xml`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/LogServiceApplication.java`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/service/ApiAccessLogIngestionService.java`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/application/port/ApiAccessLogRepository.java`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/messaging/ApiAccessLogKafkaConsumer.java`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/messaging/ApiAccessLogEventParser.java`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/persistence/entity/ApiAccessLogEntity.java`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/persistence/mapper/ApiAccessLogMapper.java`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/persistence/mybatis/ApiAccessLogRepositoryImpl.java`
- Add: `after/foodmap-log-service/src/main/resources/mapper/log/ApiAccessLogMapper.xml`
- Add: `after/foodmap-log-service/src/main/resources/application.yml`
- Add: `after/foodmap-log-service/src/main/resources/application-local.yml`
- Add: `after/foodmap-log-service/src/main/resources/application-orbstack.yml`
- Add: `after/foodmap-log-service/src/main/resources/application-prod.yml`
- Add tests under `after/foodmap-log-service/src/test/java`
- Modify: `after/pom.xml`
- Modify: `harness/scripts/validate-backend.sh`
- Modify: `harness/scripts/validate-logging.sh`
- Modify: `deploy/env.example`
- Modify: `deploy/dev-env/README.md`
- Modify: `deploy/README.md`
- Modify: `CODEX-after.md`
- Modify: `CODEX-gen.md`
- Modify: `after/README.md`
- Modify: `AGENTS.md`

- [x] **Key technologies:** Spring Kafka, conditional Kafka consumer, MyBatis Mapper/XML, PostgreSQL independent log database, Kafka offset idempotency constraint, Nacos optional config import.
- [x] **Implementation principles:** `foodmap-log-service` owns log summary ingestion; Kafka listener only receives records, application service owns the transaction, parser maps safe fields, repository writes with `on conflict do nothing`.
- [x] **Diagram:** add Mermaid Kafka api-access to PostgreSQL ingestion flow in `CODEX-after.md`.
- [x] **Operational notes:** document default-off consumer switch, local Kafka broker settings, log DB datasource settings, startup prerequisites, and failure isolation.
- [x] **Verification:** run RED/green logging validation, focused parser/ingestion tests, compile, docs validation, whitespace check, and harness.

Verification commands:

```sh
sh harness/scripts/validate-logging.sh
mvn -pl foodmap-log-service -am -Dtest=ApiAccessLogEventParserTest,ApiAccessLogIngestionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl foodmap-log-service -am -DskipTests compile
./harness/scripts/validate-docs.sh
git diff --check
./harness/scripts/run-all.sh
```

TDD note: `sh harness/scripts/validate-logging.sh` first failed with missing `after/foodmap-log-service`, then passed after adding the log service module, Kafka consumer, parser, repository, Mapper XML, configuration and docs. A full `mvn -pl foodmap-log-service -am test` still hits the pre-existing `RedissonDistributedLockClientTest` Mockito inline self-attach issue in `foodmap-common`; the focused B1.5-b log service tests pass.

### B1.5-b Task 10: API Access Log Retention Cleanup

**Files:**
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/config/LogRetentionConfiguration.java`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/service/ApiAccessLogRetentionService.java`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/scheduling/ApiAccessLogRetentionScheduler.java`
- Add: `after/foodmap-log-service/src/test/java/com/foodmap/log/service/ApiAccessLogRetentionServiceTest.java`
- Modify: `after/foodmap-log-service/src/main/java/com/foodmap/log/application/port/ApiAccessLogRepository.java`
- Modify: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/persistence/mapper/ApiAccessLogMapper.java`
- Modify: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/persistence/mybatis/ApiAccessLogRepositoryImpl.java`
- Modify: `after/foodmap-log-service/src/main/resources/mapper/log/ApiAccessLogMapper.xml`
- Modify: `after/foodmap-log-service/src/main/resources/application.yml`
- Modify: `deploy/env.example`
- Modify: `harness/scripts/validate-logging.sh`
- Modify: `deploy/dev-env/README.md`
- Modify: `deploy/README.md`
- Modify: `CODEX-after.md`
- Modify: `CODEX-gen.md`
- Modify: `after/README.md`
- Modify: `AGENTS.md`

- [x] **Key technologies:** Spring Scheduling, conditional scheduler switch, UTC `Clock`, MyBatis physical delete SQL, `SafeLog` cleanup result event.
- [x] **Implementation principles:** `ApiAccessLogRetentionService` calculates `now - retentionDays`, repository deletes rows with `occurred_time < cutoffTime`, scheduler remains disabled unless explicitly enabled.
- [x] **Diagram:** add Mermaid retention cleanup flow in `CODEX-after.md`.
- [x] **Operational notes:** document default-off cleanup switch, default 15-day retention, cron/zone settings, physical delete risk, and troubleshooting entry points.
- [x] **Verification:** run RED/green retention test, focused log-service tests, compile, logging validation, docs validation, whitespace check, and harness.

Verification commands:

```sh
mvn -pl foodmap-log-service -am -Dtest=ApiAccessLogRetentionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl foodmap-log-service -am -Dtest=ApiAccessLogRetentionServiceTest,ApiAccessLogEventParserTest,ApiAccessLogIngestionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl foodmap-log-service -am -DskipTests compile
sh harness/scripts/validate-logging.sh
./harness/scripts/validate-docs.sh
git diff --check
./harness/scripts/run-all.sh
```

TDD note: `ApiAccessLogRetentionServiceTest` first failed because `ApiAccessLogRetentionService` and `deleteOccurredBefore` did not exist, then passed after adding the service, scheduler, repository method, MyBatis delete SQL and configuration.

### B1.5-b Task 11: Full Log OSS Archive Planning

**Files:**
- Add: `deploy/logging/postgres/migration/V2__create_log_archive_records.sql`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/application/port/LogArchiveRecordRepository.java`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/persistence/entity/LogArchiveRecordEntity.java`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/persistence/mapper/LogArchiveRecordMapper.java`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/persistence/mybatis/LogArchiveRecordRepositoryImpl.java`
- Add: `after/foodmap-log-service/src/main/resources/mapper/log/LogArchiveRecordMapper.xml`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/service/LogArchivePlanningService.java`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/scheduling/LogArchivePlanningScheduler.java`
- Add: `after/foodmap-log-service/src/test/java/com/foodmap/log/service/LogArchivePlanningServiceTest.java`
- Modify: `after/foodmap-log-service/src/main/resources/application.yml`
- Modify: `deploy/env.example`
- Modify: `harness/scripts/validate-logging.sh`
- Modify: `deploy/dev-env/README.md`
- Modify: `deploy/README.md`
- Modify: `CODEX-after.md`
- Modify: `CODEX-gen.md`
- Modify: `after/README.md`
- Modify: `AGENTS.md`

- [x] **Key technologies:** PostgreSQL archive manifest table, Flyway V2 migration, Spring Scheduling, MyBatis Mapper/XML, stable OSS object key partitioning, default-off archive planning switch.
- [x] **Implementation principles:** planning service calculates the UTC day that just exceeded Elasticsearch hot retention, creates one PENDING record per archive window, and relies on a unique window constraint for idempotency.
- [x] **Diagram:** add Mermaid full-log archive planning flow in `CODEX-after.md`.
- [x] **Operational notes:** document default-off planning switch, 7-day hot retention alignment with Elasticsearch ILM, PENDING versus SUCCESS semantics, and future executor boundary.
- [x] **Verification:** run RED/green planning test, focused log-service tests, compile, logging validation, docs validation, whitespace check, and harness.

Verification commands:

```sh
mvn -pl foodmap-log-service -am -Dtest=LogArchivePlanningServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl foodmap-log-service -am -Dtest=LogArchivePlanningServiceTest,ApiAccessLogRetentionServiceTest,ApiAccessLogEventParserTest,ApiAccessLogIngestionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl foodmap-log-service -am -DskipTests compile
sh harness/scripts/validate-logging.sh
./harness/scripts/validate-docs.sh
git diff --check
./harness/scripts/run-all.sh
```

TDD note: `LogArchivePlanningServiceTest` first failed because `LogArchiveRecordRepository`, `LogArchiveRecordEntity`, and `LogArchivePlanningService` did not exist, then passed after adding the archive record table, repository, Mapper/XML, planning service, scheduler and configuration.

### B1.5-b Task 12: Full Log Archive Execution State Machine

**Files:**
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/application/port/LogArchivePayload.java`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/application/port/LogArchiveExportClient.java`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/application/port/LogArchiveObjectStorageClient.java`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/archive/NoopLogArchiveExportClient.java`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/archive/NoopLogArchiveObjectStorageClient.java`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/service/LogArchiveExecutionService.java`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/scheduling/LogArchiveExecutionScheduler.java`
- Add: `after/foodmap-log-service/src/test/java/com/foodmap/log/service/LogArchiveExecutionServiceTest.java`
- Modify: `after/foodmap-log-service/src/main/java/com/foodmap/log/application/port/LogArchiveRecordRepository.java`
- Modify: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/persistence/mapper/LogArchiveRecordMapper.java`
- Modify: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/persistence/mybatis/LogArchiveRecordRepositoryImpl.java`
- Modify: `after/foodmap-log-service/src/main/resources/mapper/log/LogArchiveRecordMapper.xml`
- Modify: `after/foodmap-log-service/src/main/resources/application.yml`
- Modify: `deploy/env.example`
- Modify: `harness/scripts/validate-logging.sh`
- Modify: `deploy/dev-env/README.md`
- Modify: `deploy/README.md`
- Modify: `CODEX-after.md`
- Modify: `CODEX-gen.md`
- Modify: `after/README.md`
- Modify: `AGENTS.md`

- [x] **Key technologies:** port-driven archive execution, MyBatis state transition SQL, default-off scheduler, noop export/storage adapters, failure reason truncation.
- [x] **Implementation principles:** execution service claims one PENDING task, marks RUNNING, calls export/storage ports outside a long database transaction, then marks SUCCESS or FAILED.
- [x] **Diagram:** add Mermaid archive execution state-machine flow in `CODEX-after.md`.
- [x] **Operational notes:** document default-off execution switch, noop adapter boundary, no long-held database transaction, and RUNNING compensation risk.
- [x] **Verification:** run RED/green execution tests, focused log-service tests, compile, logging validation, docs validation, whitespace check, and harness.

Verification commands:

```sh
mvn -pl foodmap-log-service -am -Dtest=LogArchiveExecutionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl foodmap-log-service -am -Dtest=LogArchiveExecutionServiceTest,LogArchivePlanningServiceTest,ApiAccessLogRetentionServiceTest,ApiAccessLogEventParserTest,ApiAccessLogIngestionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl foodmap-log-service -am -DskipTests compile
sh harness/scripts/validate-logging.sh
./harness/scripts/validate-docs.sh
git diff --check
./harness/scripts/run-all.sh
```

TDD note: `LogArchiveExecutionServiceTest` first failed because `LogArchiveExecutionService`, `LogArchiveExportClient`, `LogArchiveObjectStorageClient`, and `LogArchivePayload` did not exist, then passed after adding the execution service, ports, noop adapters, state transition SQL, scheduler and configuration.

### B1.5-b Task 13: Elasticsearch Archive Export Adapter

**Files:**
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/archive/ElasticsearchLogArchiveProperties.java`
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/archive/ElasticsearchLogArchiveExportClient.java`
- Add: `after/foodmap-log-service/src/test/java/com/foodmap/log/infrastructure/archive/ElasticsearchLogArchiveExportClientTest.java`
- Modify: `after/foodmap-log-service/pom.xml`
- Modify: `after/foodmap-log-service/src/main/resources/application.yml`
- Modify: `after/foodmap-log-service/src/main/resources/application-orbstack.yml`
- Modify: `deploy/env.example`
- Modify: `harness/scripts/validate-logging.sh`
- Modify: `CODEX-after.md`
- Modify: `CODEX-gen.md`
- Modify: `AGENTS.md`
- Modify: `after/README.md`
- Modify: `deploy/README.md`
- Modify: `deploy/dev-env/README.md`
- Modify: `docs/superpowers/plans/2026-06-11-b15-logging-platform.md`

- [x] **Key technologies:** Java 21 `HttpClient`, Jackson JSON parsing, Elasticsearch `_search`, `search_after` pagination, gzip JSON Lines, default-off Spring conditional adapter.
- [x] **Implementation principles:** archive execution service stays unchanged; infrastructure adapter reads RUNNING archive window, pages Elasticsearch by `@timestamp`, writes each hit `_source` to one gzip JSONL payload, and returns it through `LogArchiveExportClient`.
- [x] **Diagram:** add Mermaid Elasticsearch archive export flow in `CODEX-after.md`.
- [x] **Operational notes:** document `LOG_ARCHIVE_ELASTICSEARCH_ENABLED`, ES URL/auth/page-size/max-pages/timeouts, memory caveat before streaming upload, and hot-retention timing risk.
- [x] **Verification:** run RED/green ES adapter tests, focused log-service tests, compile, logging validation, docs validation, whitespace check, and harness.

Verification commands:

```sh
mvn -pl foodmap-log-service -am -Dtest=ElasticsearchLogArchiveExportClientTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl foodmap-log-service -am -Dtest=ElasticsearchLogArchiveExportClientTest,LogArchiveExecutionServiceTest,LogArchivePlanningServiceTest,ApiAccessLogRetentionServiceTest,ApiAccessLogEventParserTest,ApiAccessLogIngestionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl foodmap-log-service -am -DskipTests compile
sh harness/scripts/validate-logging.sh
./harness/scripts/validate-docs.sh
sh harness/scripts/validate-backend.sh
git diff --check
./harness/scripts/run-all.sh
```

TDD note: `ElasticsearchLogArchiveExportClientTest` first failed because the Elasticsearch adapter and properties class did not exist, then a pagination test failed because only the first page was exported. The tests passed after adding the adapter and `search_after` loop.

### B1.5-b Task 14: Common SearchClient for Elasticsearch

**Files:**
- Add: `after/foodmap-common/src/main/java/com/foodmap/common/search/SearchClient.java`
- Add: `after/foodmap-common/src/main/java/com/foodmap/common/search/SearchRequest.java`
- Add: `after/foodmap-common/src/main/java/com/foodmap/common/search/SearchResponse.java`
- Add: `after/foodmap-common/src/main/java/com/foodmap/common/search/SearchHit.java`
- Add: `after/foodmap-common/src/main/java/com/foodmap/common/search/SearchException.java`
- Add: `after/foodmap-common/src/main/java/com/foodmap/common/search/elasticsearch/FoodMapSearchAutoConfiguration.java`
- Add: `after/foodmap-common/src/main/java/com/foodmap/common/search/elasticsearch/ElasticsearchSearchProperties.java`
- Add: `after/foodmap-common/src/main/java/com/foodmap/common/search/elasticsearch/ElasticsearchSearchClient.java`
- Add: `after/foodmap-common/src/test/java/com/foodmap/common/search/elasticsearch/ElasticsearchSearchClientTest.java`
- Modify: `after/foodmap-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/archive/ElasticsearchLogArchiveExportClient.java`
- Modify: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/archive/ElasticsearchLogArchiveProperties.java`
- Modify: `after/foodmap-log-service/src/test/java/com/foodmap/log/infrastructure/archive/ElasticsearchLogArchiveExportClientTest.java`
- Modify: `after/foodmap-log-service/src/main/resources/application.yml`
- Modify: `after/foodmap-log-service/src/main/resources/application-orbstack.yml`
- Modify: `deploy/env.example`
- Modify: `harness/scripts/validate-logging.sh`
- Modify: `CODEX-after.md`
- Modify: `CODEX-gen.md`
- Modify: `AGENTS.md`
- Modify: `after/README.md`
- Modify: `deploy/README.md`
- Modify: `deploy/dev-env/README.md`
- Modify: `docs/superpowers/plans/2026-06-11-b15-logging-platform.md`

- [x] **Key technologies:** `foodmap-common` search port, Elasticsearch HTTP adapter, Spring Boot auto-configuration, Java 21 `HttpClient`, Jackson hits parsing, default-off common ES configuration.
- [x] **Implementation principles:** common owns ES URL/auth/timeout/HTTP error/hits parsing; log service owns archive window DSL, `search_after` iteration, gzip JSONL, and archive state semantics.
- [x] **Diagram:** update Mermaid archive export flow in `CODEX-after.md` to show `ElasticsearchLogArchiveExportClient -> SearchClient -> ElasticsearchSearchClient -> Elasticsearch`.
- [x] **Operational notes:** document `FOODMAP_SEARCH_ELASTICSEARCH_*` common settings, `LOG_ARCHIVE_ELASTICSEARCH_*` compatibility settings, and no scattered ES HTTP/SDK usage rule.
- [x] **Verification:** run RED/green common search tests, archive export tests, focused log-service tests, compile, logging validation, docs validation, whitespace check, and harness.

Verification commands:

```sh
mvn -pl foodmap-common -Dtest=ElasticsearchSearchClientTest test
mvn -pl foodmap-log-service -am -Dtest=ElasticsearchSearchClientTest,ElasticsearchLogArchiveExportClientTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl foodmap-log-service -am -Dtest=ElasticsearchSearchClientTest,ElasticsearchLogArchiveExportClientTest,LogArchiveExecutionServiceTest,LogArchivePlanningServiceTest,ApiAccessLogRetentionServiceTest,ApiAccessLogEventParserTest,ApiAccessLogIngestionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl foodmap-log-service -am -DskipTests compile
sh harness/scripts/validate-logging.sh
./harness/scripts/validate-docs.sh
sh harness/scripts/validate-backend.sh
git diff --check
./harness/scripts/run-all.sh
```

TDD note: `ElasticsearchSearchClientTest` first failed because `SearchClient`, `SearchRequest`, `SearchResponse`, `SearchHit`, `ElasticsearchSearchProperties` and `ElasticsearchSearchClient` did not exist, then passed after adding common search abstractions and the Elasticsearch adapter. `ElasticsearchLogArchiveExportClientTest` then passed after refactoring the archive exporter to use the common `SearchClient`.

### B1.5-b Task 15: Archive Object Storage Upload Bridge

**Files:**
- Add: `after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/archive/ObjectStorageLogArchiveObjectStorageClient.java`
- Add: `after/foodmap-log-service/src/test/java/com/foodmap/log/infrastructure/archive/ObjectStorageLogArchiveObjectStorageClientTest.java`
- Modify: `after/foodmap-common/src/main/java/com/foodmap/common/storage/ObjectStorageCommand.java`
- Modify: `after/foodmap-common/src/test/java/com/foodmap/common/storage/ObjectStorageCommandTest.java`
- Modify: `after/foodmap-log-service/src/main/resources/application.yml`
- Modify: `deploy/env.example`
- Modify: `harness/scripts/validate-logging.sh`
- Modify: `CODEX-after.md`
- Modify: `CODEX-gen.md`
- Modify: `AGENTS.md`
- Modify: `after/README.md`
- Modify: `deploy/README.md`
- Modify: `deploy/dev-env/README.md`
- Modify: `docs/superpowers/plans/2026-06-11-b15-logging-platform.md`

- [x] **Key technologies:** `foodmap-common` ObjectStorageClient port, system-owned ObjectStorageCommand, default-off archive storage bridge, Spring conditional adapter.
- [x] **Implementation principles:** log service translates archive record and payload into a common object storage command; common storage implementation owns MinIO/OSS SDK details; execution service owns SUCCESS/FAILED state transition.
- [x] **Diagram:** add Mermaid archive object storage upload bridge flow in `CODEX-after.md`.
- [x] **Operational notes:** document `LOG_ARCHIVE_STORAGE_UPLOAD_ENABLED`, required ObjectStorageClient Bean, Noop fallback, and current byte-array memory caveat.
- [x] **Verification:** run RED/green storage bridge tests, focused log-service tests, compile, logging validation, docs validation, whitespace check, and harness.

Verification commands:

```sh
mvn -pl foodmap-log-service -am -Dtest=ObjectStorageCommandTest,ObjectStorageLogArchiveObjectStorageClientTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl foodmap-log-service -am -Dtest=ObjectStorageCommandTest,ObjectStorageLogArchiveObjectStorageClientTest,ElasticsearchSearchClientTest,ElasticsearchLogArchiveExportClientTest,LogArchiveExecutionServiceTest,LogArchivePlanningServiceTest,ApiAccessLogRetentionServiceTest,ApiAccessLogEventParserTest,ApiAccessLogIngestionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl foodmap-log-service -am -DskipTests compile
sh harness/scripts/validate-logging.sh
./harness/scripts/validate-docs.sh
sh harness/scripts/validate-backend.sh
git diff --check
./harness/scripts/run-all.sh
```

TDD note: `ObjectStorageLogArchiveObjectStorageClientTest` first failed because the bridge adapter did not exist, then passed after adding `ObjectStorageLogArchiveObjectStorageClient` and `ObjectStorageCommand.systemUpload`.

### B1.5-b Task 16: Admin Log Query Permission Skeleton and Closeout

Implement the first admin-side permission guard for log summary queries and close B1.5-b as a delivered logging-platform foundation.

- [x] **Key technologies:** `foodmap-admin-service` controller guard, trusted internal admin headers, stable permission enum `LOG_ACCESS_READ`, `FoodMapException` with common `401/403` error codes, harness backend/logging validation.
- [x] **Implementation principles:** admin log query requests must be authenticated and authorized before the admin service proxies to `foodmap-log-service`; missing admin identity returns `401`, missing log-query permission returns `403`, and failed permission checks must not call the log service.
- [x] **Diagram:** `CODEX-after.md` now includes the B1.5-b admin log permission skeleton flow from gateway trusted headers to `AdminPermissionGuard`, `AdminPermissionCode.LOG_ACCESS_READ`, and the log-service proxy.
- [x] **Operational notes:** current headers are a bridge until real admin login/RBAC lands; they must be generated by the gateway or trusted backend filter, never by public clients. Real-environment Kafka/Elasticsearch/PostgreSQL/MinIO integration and Aliyun OSS production adapter are deployment/media-service follow-ups, not B1.5 blockers.
- [x] **Files changed:** `AdminLogController`, `AdminAuthHeaders`, `AdminPermissionCode`, `AdminPermissionGuard`, `AdminLogControllerPermissionTest`, `harness/scripts/validate-backend.sh`, `CODEX-after.md`, `CODEX-gen.md`, `after/README.md`, `AGENTS.md`.
- [x] **Verification:** 
  - `mvn -pl foodmap-admin-service -am -Dtest=AdminLogControllerPermissionTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - `mvn -pl foodmap-admin-service,foodmap-log-service -am -Dtest=AdminLogControllerPermissionTest,AdminApiAccessLogQueryServiceTest,HttpLogApiAccessLogClientTest,AdminUserServiceImplTest,ApiAccessLogQueryServiceTest,ApiAccessLogIngestionServiceTest,ApiAccessLogRetentionServiceTest,ApiAccessLogEventParserTest,LogArchivePlanningServiceTest,LogArchiveExecutionServiceTest,ElasticsearchLogArchiveExportClientTest,ObjectStorageLogArchiveObjectStorageClientTest,MinioObjectStorageClientTest,MinioHttpObjectStorageGatewayTest,ElasticsearchSearchClientTest,ObjectStorageCommandTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - `mvn -pl foodmap-admin-service,foodmap-log-service -am -DskipTests compile`
  - `./harness/scripts/run-all.sh`

### B1.5-b Next Task Documentation Template

Every following B1.5-b task must include:

- [ ] **Key technologies:** list framework/library/configuration files and service boundaries.
- [ ] **Implementation principles:** explain how data/log events flow at runtime.
- [ ] **Diagram:** add at least one Mermaid `flowchart` or `sequenceDiagram`.
- [ ] **Operational notes:** document default switches, production risks, rollback, and troubleshooting entry points.
- [ ] **Verification:** run focused tests/build/harness and record the commands.
