#!/usr/bin/env sh
set -eu

if [ "$#" -lt 1 ]; then
  printf 'Usage: %s <service-module> [profile]\n' "$0" >&2
  printf 'Example: %s foodmap-auth-service local\n' "$0" >&2
  exit 1
fi

SERVICE_MODULE="$1"
FOODMAP_PROFILE="${2:-${FOODMAP_PROFILE:-local}}"
export FOODMAP_PROFILE

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT_DIR"

if [ ! -d "$SERVICE_MODULE" ]; then
  printf 'Service module not found: %s\n' "$SERVICE_MODULE" >&2
  exit 1
fi

printf 'Starting %s with FOODMAP_PROFILE=%s\n' "$SERVICE_MODULE" "$FOODMAP_PROFILE"
mvn -pl "$SERVICE_MODULE" -am -DskipTests install
exec mvn -pl "$SERVICE_MODULE" spring-boot:run
