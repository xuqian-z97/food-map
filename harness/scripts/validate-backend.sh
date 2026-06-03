#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "$ROOT_DIR"

if [ ! -d "backend" ]; then
  printf 'INFO: backend/ does not exist yet. Backend code checks are skipped for documentation stage.\n'
  exit 0
fi

[ -f "backend/pom.xml" ] || {
  printf 'FAIL: backend/pom.xml is required after backend/ is created.\n' >&2
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
  [ -d "backend/$service" ] || printf 'WARN: missing planned service: %s\n' "$service"
done

if command -v mvn >/dev/null 2>&1; then
  (cd backend && mvn -q -DskipTests validate)
else
  printf 'INFO: mvn not found; Maven validation skipped.\n'
fi

printf 'PASS: backend harness checks completed.\n'

