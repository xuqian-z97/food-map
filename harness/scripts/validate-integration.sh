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

contains() {
  file="$1"
  pattern="$2"
  grep -q "$pattern" "$file" || fail "$file does not contain required pattern: $pattern"
}

require_file "docs/integration/README.md"
require_file "docs/integration/templates/integration-plan.md"
require_file "docs/integration/templates/issue-log.md"
require_file "skills/foodmap-integration-coordination/SKILL.md"

contains "docs/integration/README.md" "integration-plan.md"
contains "docs/integration/README.md" "issue-log.md"
contains "docs/integration/README.md" "通过"
contains "docs/integration/README.md" "联调安全点"
contains "docs/integration/README.md" "L2 本地真实联调"
contains "docs/integration/templates/integration-plan.md" "前端职责"
contains "docs/integration/templates/integration-plan.md" "后端职责"
contains "docs/integration/templates/integration-plan.md" "联调安全点"
contains "docs/integration/templates/integration-plan.md" "准入检查"
contains "docs/integration/templates/integration-plan.md" "联调等级"
contains "docs/integration/templates/integration-plan.md" "前端可发起真实请求"
contains "docs/integration/templates/integration-plan.md" "后端接口可被 curl"
contains "docs/integration/templates/integration-plan.md" "requestId"
contains "docs/integration/templates/integration-plan.md" "traceId"
contains "docs/integration/templates/integration-plan.md" "验收判定"
contains "docs/integration/templates/issue-log.md" "复现步骤"
contains "docs/integration/templates/issue-log.md" "测试数据"
contains "docs/integration/templates/issue-log.md" "前端日志"
contains "docs/integration/templates/issue-log.md" "后端日志"
contains "docs/integration/templates/issue-log.md" "复测结果"
contains "skills/foodmap-integration-coordination/SKILL.md" "docs/integration"
contains "skills/foodmap-integration-coordination/SKILL.md" "validate-integration.sh"
contains "skills/foodmap-integration-coordination/SKILL.md" "联调安全点"
contains "skills/foodmap-integration-coordination/SKILL.md" "业务切片"

found_instance=false
for dir in docs/integration/*; do
  [ -d "$dir" ] || continue
  case "$dir" in
    docs/integration/templates) continue ;;
  esac
  found_instance=true
  require_file "$dir/integration-plan.md"
  require_file "$dir/issue-log.md"
  contains "$dir/integration-plan.md" "联调目标"
  contains "$dir/integration-plan.md" "联调安全点"
  contains "$dir/integration-plan.md" "准入检查"
  contains "$dir/integration-plan.md" "前端职责"
  contains "$dir/integration-plan.md" "后端职责"
  contains "$dir/integration-plan.md" "验收场景"
  contains "$dir/integration-plan.md" "requestId"
  contains "$dir/integration-plan.md" "traceId"
  contains "$dir/issue-log.md" "问题总览"
  contains "$dir/issue-log.md" "复现步骤"
  contains "$dir/issue-log.md" "复测结果"
done

[ "$found_instance" = true ] || fail "no integration instance folder found under docs/integration"

printf 'PASS: integration coordination baseline checks completed.\n'
