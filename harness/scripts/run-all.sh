#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"

run_check() {
  name="$1"
  script="$2"
  printf '\n==> %s\n' "$name"
  "$ROOT_DIR/$script"
}

run_check "validate docs" "harness/scripts/validate-docs.sh"
run_check "validate api" "harness/scripts/validate-api.sh"
run_check "validate backend" "harness/scripts/validate-backend.sh"
run_check "validate ios" "harness/scripts/validate-ios.sh"
run_check "validate git" "harness/scripts/validate-git.sh"

printf '\nAll harness checks completed.\n'

