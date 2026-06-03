#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "$ROOT_DIR"

if [ ! -d "front" ]; then
  printf 'INFO: front/ does not exist yet. iOS code checks are skipped for documentation stage.\n'
  exit 0
fi

[ -d "front/FoodMapApp" ] || {
  printf 'FAIL: front/FoodMapApp is required after front/ is created.\n' >&2
  exit 1
}

if command -v xcodebuild >/dev/null 2>&1; then
  if find front -name '*.xcodeproj' -maxdepth 3 | grep -q .; then
    printf 'INFO: xcodebuild is available; project-specific build command must be added once scheme is finalized.\n'
  else
    printf 'INFO: no xcodeproj found; xcodebuild skipped.\n'
  fi
else
  printf 'INFO: xcodebuild not found; iOS build skipped.\n'
fi

printf 'PASS: iOS harness checks completed.\n'
