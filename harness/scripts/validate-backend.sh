#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "$ROOT_DIR"

if [ ! -d "after" ]; then
  printf 'INFO: after/ does not exist yet. Backend code checks are skipped for documentation stage.\n'
  exit 0
fi

[ -f "after/pom.xml" ] || {
  printf 'FAIL: after/pom.xml is required after after/ is created.\n' >&2
  exit 1
}

services="
foodmap-gateway-service
foodmap-auth-service
foodmap-user-service
foodmap-relation-service
foodmap-store-service
foodmap-recommendation-service
foodmap-community-service
foodmap-media-service
"

for service in $services; do
  [ -d "after/$service" ] || printf 'WARN: missing planned service: %s\n' "$service"
done

entity_files=$(find after -type f \( \
  -path "*/src/main/java/*/infrastructure/persistence/entity/*Entity.java" \
  -o -path "*/src/main/java/com/foodmap/common/persistence/BaseEntity.java" \
\))

for file in $entity_files; do
  awk '
    /^[[:space:]]*private[[:space:]].*;[[:space:]]*$/ {
      if (previous_line !~ /^[[:space:]]*\*\/[[:space:]]*$/) {
        printf "FAIL: entity field in %s:%d must have field-level Javadoc.\n", FILENAME, FNR > "/dev/stderr"
        failed = 1
      }
    }
    { previous_line = $0 }
    END { exit failed ? 1 : 0 }
  ' "$file"
done

if command -v python3 >/dev/null 2>&1; then
  python3 - <<'PY'
from pathlib import Path
import os
import re
import sys

root = Path("after")
method_pattern = re.compile(
    r"^\s*(public|protected)\s+"
    r"(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?"
    r"(?:<[^>]+>\s+)?"
    r"([\w<>\[\], ? extends super]+)\s+"
    r"(\w+)\s*\(([^)]*)\)\s*(?:throws [^{;]+)?\s*(\{|;)\s*$"
)


def extract_param_names(raw_params):
    """Extract Java parameter names for the lightweight Javadoc scan."""
    if not raw_params.strip():
        return []
    names = []
    for part in raw_params.split(","):
        cleaned = re.sub(r"@\w+(?:\([^)]*\))?\s*", "", part.strip())
        pieces = cleaned.split()
        if pieces:
            names.append(pieces[-1].replace("...", ""))
    return names


def javadoc_before(lines, index):
    """Return the Javadoc block directly above annotations for a method."""
    cursor = index - 1
    while cursor >= 0 and lines[cursor].strip().startswith("@"):
        cursor -= 1
    if cursor < 0 or lines[cursor].strip() != "*/":
        return None
    block = []
    while cursor >= 0:
        block.append(lines[cursor])
        if lines[cursor].strip() == "/**":
            return "\n".join(reversed(block))
        cursor -= 1
    return None


def should_skip_method(method_name, raw_params, path):
    """Skip JavaBean and framework bootstrap methods that are documented elsewhere."""
    if method_name == "main" and str(path).endswith("Application.java"):
        return True
    if method_name.startswith("set") and raw_params.strip():
        return True
    if method_name.startswith("get") and not raw_params.strip():
        return True
    if method_name.startswith("is") and not raw_params.strip():
        return True
    return False


missing = []
for path in sorted(root.rglob("src/main/java/**/*.java")):
    lines = path.read_text(encoding="utf-8").splitlines()
    for index, line in enumerate(lines):
        match = method_pattern.match(line)
        if not match:
            continue
        return_type = match.group(2).strip()
        method_name = match.group(3)
        raw_params = match.group(4)
        if should_skip_method(method_name, raw_params, path):
            continue
        block = javadoc_before(lines, index)
        issues = []
        if block is None:
            issues.append("missing method Javadoc")
        else:
            if return_type != "void" and "@return" not in block:
                issues.append("missing @return")
            for param_name in extract_param_names(raw_params):
                if f"@param {param_name}" not in block:
                    issues.append(f"missing @param {param_name}")
        if issues:
            missing.append(f"{path}:{index + 1} {method_name}: {', '.join(issues)}")

if missing:
    print("WARN: business/API method Javadoc scan found methods missing business purpose/@param/@return.", file=sys.stderr)
    for item in missing[:80]:
        print(f"WARN: {item}", file=sys.stderr)
    if len(missing) > 80:
        print(f"WARN: ... and {len(missing) - 80} more method Javadoc issues.", file=sys.stderr)
    if os.environ.get("FOODMAP_STRICT_METHOD_JAVADOC") == "true":
        sys.exit(1)
PY
else
  printf 'INFO: python3 not found; method Javadoc scan skipped.\n'
fi

if command -v mvn >/dev/null 2>&1; then
  (cd after && mvn -q -DskipTests validate)
else
  printf 'INFO: mvn not found; Maven validation skipped.\n'
fi

printf 'PASS: backend harness checks completed.\n'
