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

if command -v mvn >/dev/null 2>&1; then
  (cd after && mvn -q -DskipTests validate)
else
  printf 'INFO: mvn not found; Maven validation skipped.\n'
fi

printf 'PASS: backend harness checks completed.\n'
