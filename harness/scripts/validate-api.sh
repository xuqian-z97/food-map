#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "$ROOT_DIR"

fail() {
  printf 'FAIL: %s\n' "$1" >&2
  exit 1
}

grep -q "PRIVATE" CODEX-after.md || fail "visibility enum PRIVATE missing in backend doc"
grep -q "SPECIFIC_USERS" CODEX-after.md || fail "visibility enum SPECIFIC_USERS missing in backend doc"
grep -q "FRIENDS" CODEX-after.md || fail "visibility enum FRIENDS missing in backend doc"
grep -q "COUPLE" CODEX-after.md || fail "visibility enum COUPLE missing in backend doc"
grep -q "GROUP" CODEX-after.md || fail "visibility enum GROUP missing in backend doc"
grep -q "PUBLIC" CODEX-after.md || fail "visibility enum PUBLIC missing in backend doc"
grep -q "bbox" CODEX-after.md || fail "bbox query missing in backend API draft"
grep -q "分页" CODEX-after.md || fail "pagination rule missing in backend doc"

if [ -d "docs/api" ]; then
  if ! find docs/api -type f | grep -q .; then
    printf 'WARN: docs/api exists but has no API documents yet.\n'
  fi
else
  printf 'INFO: docs/api does not exist yet. API detail docs are expected when API implementation starts.\n'
fi

printf 'PASS: API contract baseline checks completed.\n'

