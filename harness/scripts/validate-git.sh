#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "$ROOT_DIR"

git rev-parse --is-inside-work-tree >/dev/null 2>&1 || {
  printf 'FAIL: not inside a git repository.\n' >&2
  exit 1
}

branch="$(git branch --show-current)"
[ "$branch" = "main" ] || {
  printf 'WARN: current branch is %s, expected main.\n' "$branch"
}

remote="$(git remote get-url origin 2>/dev/null || true)"
[ -n "$remote" ] || {
  printf 'FAIL: origin remote is not configured.\n' >&2
  exit 1
}

if [ -n "$(git status --short)" ]; then
  printf 'WARN: working tree has uncommitted changes.\n'
  git status --short
else
  printf 'PASS: working tree is clean.\n'
fi

printf 'INFO: origin=%s\n' "$remote"

