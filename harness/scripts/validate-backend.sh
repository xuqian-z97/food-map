#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "$ROOT_DIR"

if [ ! -d "after" ]; then
  printf 'INFO: after/ does not exist yet. Backend code checks are skipped for documentation stage.\n'
  exit 0
fi

[ -f "after/pom.xml" ] || {
  printf 'FAIL: after/pom.xml is required after after/ is created.\n' >&2
  exit 1
}

services="
foodmap-gateway-service
foodmap-auth-service
foodmap-user-service
foodmap-relation-service
foodmap-store-service
foodmap-recommendation-service
foodmap-community-service
foodmap-media-service
"

for service in $services; do
  [ -d "after/$service" ] || printf 'WARN: missing planned service: %s\n' "$service"
done

entity_files=$(find after -type f \( \
  -path "*/src/main/java/*/infrastructure/persistence/entity/*Entity.java" \
  -o -path "*/src/main/java/com/foodmap/common/persistence/BaseEntity.java" \
\))

for file in $entity_files; do
  awk '
    /^[[:space:]]*private[[:space:]].*;[[:space:]]*$/ {
      if (previous_line !~ /^[[:space:]]*\*\/[[:space:]]*$/) {
        printf "FAIL: entity field in %s:%d must have field-level Javadoc.\n", FILENAME, FNR > "/dev/stderr"
        failed = 1
      }
    }
    { previous_line = $0 }
    END { exit failed ? 1 : 0 }
  ' "$file"
done

if command -v mvn >/dev/null 2>&1; then
  (cd after && mvn -q -DskipTests validate)
else
  printf 'INFO: mvn not found; Maven validation skipped.\n'
fi

printf 'PASS: backend harness checks completed.\n'
