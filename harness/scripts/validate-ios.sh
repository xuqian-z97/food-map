#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "$ROOT_DIR"

if [ ! -d "ios" ]; then
  printf 'INFO: ios/ does not exist yet. iOS code checks are skipped for documentation stage.\n'
  exit 0
fi

[ -d "ios/FoodMapApp" ] || {
  printf 'FAIL: ios/FoodMapApp is required after ios/ is created.\n' >&2
  exit 1
}

if command -v xcodebuild >/dev/null 2>&1; then
  if find ios -name '*.xcodeproj' -maxdepth 3 | grep -q .; then
    printf 'INFO: xcodebuild is available; project-specific build command must be added once scheme is finalized.\n'
  else
    printf 'INFO: no xcodeproj found; xcodebuild skipped.\n'
  fi
else
  printf 'INFO: xcodebuild not found; iOS build skipped.\n'
fi

printf 'PASS: iOS harness checks completed.\n'

