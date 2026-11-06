#!/usr/bin/env bash
# 跑一个 verify 场景，并把结果写入 status 台账。
#
# 用法：
#   FM_TEST_PW=... ./harness/scripts/verify-record.sh harness/scenarios/b1-ui-login.yml \
#     --scope=b1-ui-login --kind=e2e --covers=F-001,F-003
set -euo pipefail
cd "$(dirname "$0")/../.."

if [ $# -lt 1 ]; then
  echo "用法: $0 <scenario.yml> --scope=<scope> --kind=<kind> --covers=F-001,F-002" >&2
  exit 2
fi

SCENARIO="$1"; shift
EVIDENCE_DIR="${VERIFY_EVIDENCE_DIR:-/tmp/verify-evidence}"
RESULT="/tmp/verify-$(date +%H%M%S).json"

if ! node tools/verify/cli.mjs run "$SCENARIO" --json --evidence-dir "$EVIDENCE_DIR" > "$RESULT" 2>/tmp/verify-last.stderr; then
  echo "场景失败，见 /tmp/verify-last.stderr 与 $RESULT" >&2
fi

read -r TOTAL PASSED FAILED <<EOJ
$(node -e "const r=require('$RESULT');const s=r.summary||{};console.log((s.total??0), (s.passed??0), (s.failed??0))")
EOJ

./status/status test record --total="$TOTAL" --passed="$PASSED" --failed="$FAILED" \
  --evidence="$EVIDENCE_DIR" --command="node tools/verify/cli.mjs run $SCENARIO" "$@"

node -e "const r=require('$RESULT');process.exit(r.exitCode??0)"
