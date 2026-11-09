#!/usr/bin/env sh
# 本地方案 D：只读预检，运行依赖就绪的场景。严格验收：VERIFY_REQUIRE_FULL=1。
# 不启动服务、不删除历史数据；--dry-run / --list 不访问外部依赖。
set -eu
ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "$ROOT_DIR"
exec node tools/verify/cli.mjs run harness/scenarios/ \
  --dependencies harness/verify-dependencies.json \
  --evidence-dir "${VERIFY_EVIDENCE_DIR:-/tmp/verify-evidence}" "$@"
