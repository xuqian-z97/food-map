#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "$ROOT_DIR"

FLUENT_BIT_DIR="deploy/logging/fluent-bit"
FLUENT_BIT_CONF="$FLUENT_BIT_DIR/fluent-bit.conf"
PARSERS_CONF="$FLUENT_BIT_DIR/parsers.conf"
ELASTICSEARCH_DIR="deploy/logging/elasticsearch"
INDEX_TEMPLATE="$ELASTICSEARCH_DIR/foodmap-logs-index-template.json"
LOGSTASH_DIR="deploy/logging/logstash"
LOGSTASH_PIPELINE="$LOGSTASH_DIR/pipeline/foodmap-logs.conf"
LOG_POSTGRES_MIGRATION="deploy/logging/postgres/migration/V1__create_api_access_log.sql"
LOG_ARCHIVE_MIGRATION="deploy/logging/postgres/migration/V2__create_log_archive_records.sql"

[ -f "$FLUENT_BIT_CONF" ] || {
  printf 'FAIL: %s is required for the B1.5-b logging collector.\n' "$FLUENT_BIT_CONF" >&2
  exit 1
}

[ -f "$PARSERS_CONF" ] || {
  printf 'FAIL: %s is required for the B1.5-b logging collector.\n' "$PARSERS_CONF" >&2
  exit 1
}

for topic in \
  foodmap.logs.application \
  foodmap.logs.api-access \
  foodmap.logs.sql \
  foodmap.logs.audit \
  foodmap.logs.security
do
  if ! grep -q "$topic" "$FLUENT_BIT_CONF"; then
    printf 'FAIL: %s must route logs to %s.\n' "$FLUENT_BIT_CONF" "$topic" >&2
    exit 1
  fi
done

if ! grep -q "foodmap-fluent-bit" deploy/docker-compose.dev.yml; then
  printf 'FAIL: deploy/docker-compose.dev.yml must define the foodmap-fluent-bit collector service.\n' >&2
  exit 1
fi

[ -f "$INDEX_TEMPLATE" ] || {
  printf 'FAIL: %s is required for the B1.5-b Elasticsearch hot log store.\n' "$INDEX_TEMPLATE" >&2
  exit 1
}

if ! grep -q "foodmap-elasticsearch" deploy/docker-compose.dev.yml; then
  printf 'FAIL: deploy/docker-compose.dev.yml must define the foodmap-elasticsearch hot log store service.\n' >&2
  exit 1
fi

if ! grep -q "foodmap-logs-*" "$INDEX_TEMPLATE"; then
  printf 'FAIL: %s must define the foodmap-logs-* index pattern.\n' "$INDEX_TEMPLATE" >&2
  exit 1
fi

[ -f "$LOGSTASH_PIPELINE" ] || {
  printf 'FAIL: %s is required for the B1.5-b Kafka to Elasticsearch log consumer.\n' "$LOGSTASH_PIPELINE" >&2
  exit 1
}

for topic in \
  foodmap.logs.application \
  foodmap.logs.api-access \
  foodmap.logs.sql \
  foodmap.logs.audit \
  foodmap.logs.security
do
  if ! grep -q "$topic" "$LOGSTASH_PIPELINE"; then
    printf 'FAIL: %s must consume %s.\n' "$LOGSTASH_PIPELINE" "$topic" >&2
    exit 1
  fi
done

if ! grep -q "foodmap-logs-%{+YYYY.MM.dd}" "$LOGSTASH_PIPELINE"; then
  printf 'FAIL: %s must write daily foodmap-logs-* indexes.\n' "$LOGSTASH_PIPELINE" >&2
  exit 1
fi

if ! grep -q "foodmap-logstash" deploy/docker-compose.dev.yml; then
  printf 'FAIL: deploy/docker-compose.dev.yml must define the foodmap-logstash consumer service.\n' >&2
  exit 1
fi

if ! grep -q "foodmap_log_db" deploy/postgres/init/01-create-foodmap-databases.sql; then
  printf 'FAIL: deploy/postgres/init/01-create-foodmap-databases.sql must create foodmap_log_db.\n' >&2
  exit 1
fi

[ -f "$LOG_POSTGRES_MIGRATION" ] || {
  printf 'FAIL: %s is required for the B1.5-b api_access_log summary table.\n' "$LOG_POSTGRES_MIGRATION" >&2
  exit 1
}

for required_text in \
  "create table if not exists api_access_log" \
  "comment on table api_access_log" \
  "request_id" \
  "trace_id" \
  "service_name" \
  "duration_ms"
do
  if ! grep -q "$required_text" "$LOG_POSTGRES_MIGRATION"; then
    printf 'FAIL: %s must contain %s.\n' "$LOG_POSTGRES_MIGRATION" "$required_text" >&2
    exit 1
  fi
done

if ! grep -q "foodmap-log-postgres-migrate" deploy/docker-compose.dev.yml; then
  printf 'FAIL: deploy/docker-compose.dev.yml must define the foodmap-log-postgres-migrate Flyway service.\n' >&2
  exit 1
fi

[ -d "after/foodmap-log-service" ] || {
  printf 'FAIL: after/foodmap-log-service is required for the B1.5-b api-access Kafka consumer.\n' >&2
  exit 1
}

if ! grep -q "<module>foodmap-log-service</module>" after/pom.xml; then
  printf 'FAIL: after/pom.xml must include foodmap-log-service.\n' >&2
  exit 1
fi

[ -f "after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/messaging/ApiAccessLogKafkaConsumer.java" ] || {
  printf 'FAIL: ApiAccessLogKafkaConsumer is required for api-access Kafka ingestion.\n' >&2
  exit 1
}

[ -f "after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/persistence/entity/ApiAccessLogEntity.java" ] || {
  printf 'FAIL: ApiAccessLogEntity is required for api_access_log persistence mapping.\n' >&2
  exit 1
}

[ -f "after/foodmap-log-service/src/main/resources/mapper/log/ApiAccessLogMapper.xml" ] || {
  printf 'FAIL: ApiAccessLogMapper.xml is required for api_access_log persistence mapping.\n' >&2
  exit 1
}

if ! grep -q "uk_api_access_log_source" after/foodmap-log-service/src/main/resources/mapper/log/ApiAccessLogMapper.xml; then
  printf 'FAIL: ApiAccessLogMapper.xml must use the Kafka offset idempotency constraint.\n' >&2
  exit 1
fi

if ! grep -q "LOG_SERVICE_API_ACCESS_CONSUMER_ENABLED" after/foodmap-log-service/src/main/resources/application.yml; then
  printf 'FAIL: foodmap-log-service must keep api-access Kafka consumption behind an explicit enable switch.\n' >&2
  exit 1
fi

[ -f "after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/scheduling/ApiAccessLogRetentionScheduler.java" ] || {
  printf 'FAIL: ApiAccessLogRetentionScheduler is required for api_access_log retention cleanup.\n' >&2
  exit 1
}

if ! grep -q "delete from api_access_log" after/foodmap-log-service/src/main/resources/mapper/log/ApiAccessLogMapper.xml; then
  printf 'FAIL: ApiAccessLogMapper.xml must physically delete expired api_access_log rows.\n' >&2
  exit 1
fi

if ! grep -q "LOG_SERVICE_API_ACCESS_RETENTION_CLEANUP_ENABLED" after/foodmap-log-service/src/main/resources/application.yml; then
  printf 'FAIL: api_access_log retention cleanup must stay behind an explicit enable switch.\n' >&2
  exit 1
fi

[ -f "$LOG_ARCHIVE_MIGRATION" ] || {
  printf 'FAIL: %s is required for the B1.5-b OSS archive planning table.\n' "$LOG_ARCHIVE_MIGRATION" >&2
  exit 1
}

for required_text in \
  "create table if not exists log_archive_records" \
  "comment on table log_archive_records" \
  "archive_status" \
  "object_key"
do
  if ! grep -q "$required_text" "$LOG_ARCHIVE_MIGRATION"; then
    printf 'FAIL: %s must contain %s.\n' "$LOG_ARCHIVE_MIGRATION" "$required_text" >&2
    exit 1
  fi
done

[ -f "after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/scheduling/LogArchivePlanningScheduler.java" ] || {
  printf 'FAIL: LogArchivePlanningScheduler is required for full-log archive planning.\n' >&2
  exit 1
}

if ! grep -q "LOG_ARCHIVE_PLANNING_ENABLED" after/foodmap-log-service/src/main/resources/application.yml; then
  printf 'FAIL: log archive planning must stay behind an explicit enable switch.\n' >&2
  exit 1
fi

[ -f "after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/scheduling/LogArchiveExecutionScheduler.java" ] || {
  printf 'FAIL: LogArchiveExecutionScheduler is required for full-log archive execution.\n' >&2
  exit 1
}

if ! grep -q "LOG_ARCHIVE_EXECUTION_ENABLED" after/foodmap-log-service/src/main/resources/application.yml; then
  printf 'FAIL: log archive execution must stay behind an explicit enable switch.\n' >&2
  exit 1
fi

[ -f "after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/archive/ElasticsearchLogArchiveExportClient.java" ] || {
  printf 'FAIL: ElasticsearchLogArchiveExportClient is required for full-log Elasticsearch export.\n' >&2
  exit 1
}

if ! grep -q "LOG_ARCHIVE_ELASTICSEARCH_ENABLED" after/foodmap-log-service/src/main/resources/application.yml; then
  printf 'FAIL: Elasticsearch archive export must stay behind an explicit enable switch.\n' >&2
  exit 1
fi

if ! grep -q "search_after" after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/archive/ElasticsearchLogArchiveExportClient.java; then
  printf 'FAIL: Elasticsearch archive export must use search_after pagination.\n' >&2
  exit 1
fi

[ -f "after/foodmap-common/src/main/java/com/foodmap/common/search/SearchClient.java" ] || {
  printf 'FAIL: common SearchClient is required as the unified Elasticsearch access entry.\n' >&2
  exit 1
}

[ -f "after/foodmap-common/src/main/java/com/foodmap/common/search/elasticsearch/ElasticsearchSearchClient.java" ] || {
  printf 'FAIL: common ElasticsearchSearchClient is required as the shared Elasticsearch adapter.\n' >&2
  exit 1
}

if ! grep -q "FOODMAP_SEARCH_ELASTICSEARCH_ENABLED" after/foodmap-log-service/src/main/resources/application.yml; then
  printf 'FAIL: foodmap-log-service must map archive ES export to the common SearchClient configuration.\n' >&2
  exit 1
fi

[ -f "after/foodmap-log-service/src/main/java/com/foodmap/log/infrastructure/archive/ObjectStorageLogArchiveObjectStorageClient.java" ] || {
  printf 'FAIL: ObjectStorageLogArchiveObjectStorageClient is required for full-log archive object storage upload.\n' >&2
  exit 1
}

if ! grep -q "LOG_ARCHIVE_STORAGE_UPLOAD_ENABLED" after/foodmap-log-service/src/main/resources/application.yml; then
  printf 'FAIL: log archive object storage upload must stay behind an explicit enable switch.\n' >&2
  exit 1
fi

if ! grep -q "systemUpload" after/foodmap-common/src/main/java/com/foodmap/common/storage/ObjectStorageCommand.java; then
  printf 'FAIL: ObjectStorageCommand must support system-owned archive uploads.\n' >&2
  exit 1
fi

[ -f "after/foodmap-common/src/main/java/com/foodmap/common/storage/minio/MinioObjectStorageClient.java" ] || {
  printf 'FAIL: common MinioObjectStorageClient is required as the shared MinIO object storage adapter.\n' >&2
  exit 1
}

[ -f "after/foodmap-common/src/main/java/com/foodmap/common/storage/minio/MinioHttpObjectStorageGateway.java" ] || {
  printf 'FAIL: common MinioHttpObjectStorageGateway is required for MinIO/S3 compatible uploads.\n' >&2
  exit 1
}

if ! grep -q "FOODMAP_STORAGE_MINIO_ENABLED" after/foodmap-log-service/src/main/resources/application.yml; then
  printf 'FAIL: foodmap-log-service must expose common MinIO ObjectStorageClient configuration.\n' >&2
  exit 1
fi

[ -f "after/foodmap-log-service/src/main/java/com/foodmap/log/controller/LogApiAccessLogController.java" ] || {
  printf 'FAIL: LogApiAccessLogController is required for internal admin log summary queries.\n' >&2
  exit 1
}

if ! grep -q "/internal/logs/api-access" after/foodmap-log-service/src/main/java/com/foodmap/log/controller/LogApiAccessLogController.java; then
  printf 'FAIL: log-service must expose /internal/logs/api-access for admin log summary queries.\n' >&2
  exit 1
fi

for required_text in \
  "selectByCriteria" \
  "countByCriteria" \
  "order by occurred_time desc, access_log_id desc"
do
  if ! grep -q "$required_text" after/foodmap-log-service/src/main/resources/mapper/log/ApiAccessLogMapper.xml; then
    printf 'FAIL: ApiAccessLogMapper.xml must contain query support %s.\n' "$required_text" >&2
    exit 1
  fi
done

for required_text in \
  "archive_status = 'RUNNING'" \
  "archive_status = 'SUCCESS'" \
  "archive_status = 'FAILED'"
do
  if ! grep -q "$required_text" after/foodmap-log-service/src/main/resources/mapper/log/LogArchiveRecordMapper.xml; then
    printf 'FAIL: LogArchiveRecordMapper.xml must contain archive state transition %s.\n' "$required_text" >&2
    exit 1
  fi
done

if command -v docker >/dev/null 2>&1; then
  docker compose -f deploy/docker-compose.dev.yml --profile logging config >/dev/null
else
  printf 'INFO: docker not found; compose logging config validation skipped.\n'
fi

printf 'PASS: logging collector baseline checks completed.\n'
