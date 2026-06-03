#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "$ROOT_DIR"

fail() {
  printf 'FAIL: %s\n' "$1" >&2
  exit 1
}

require_file() {
  [ -f "$1" ] || fail "missing required file: $1"
}

require_dir() {
  [ -d "$1" ] || fail "missing required directory: $1"
}

contains() {
  file="$1"
  pattern="$2"
  grep -q "$pattern" "$file" || fail "$file does not contain required pattern: $pattern"
}

require_file "CODEX-product.md"
require_file "CODEX-front.md"
require_file "CODEX-after.md"
require_file "CODEX-gen.md"
require_file "AGENTS.md"
require_dir ".agents"
require_dir "skills"
require_dir "harness"

for skill in skills/*/SKILL.md; do
  [ -f "$skill" ] || fail "no project skills found"
  sed -n '1p' "$skill" | grep -q '^---$' || fail "$skill missing frontmatter start"
  grep -q '^name:' "$skill" || fail "$skill missing name"
  grep -q '^description:' "$skill" || fail "$skill missing description"
done

contains "CODEX-after.md" "微服务"
contains "CODEX-after.md" "独立数据库"
contains "CODEX-product.md" "全部公开"
contains "CODEX-product.md" "纯文字"
contains "AGENTS.md" "多代理开发模式"
contains "AGENTS.md" "项目专属 Skills"
contains "CODEX-gen.md" "harness"

printf 'PASS: documentation structure and core rules are present.\n'

