#!/usr/bin/env bash
# Claude Code PostToolUse hook — runs Checkstyle on edited Java files.
#
# Receives a JSON object on stdin:
#   { "tool_name": "Edit", "tool_input": { "file_path": "..." }, ... }
#
# Exit 0  → Claude continues normally.
# Exit 1  → Claude sees stderr as a warning and will attempt to fix the issues.

set -euo pipefail

MVN="C:/Program Files/JetBrains/IntelliJ IDEA Community Edition 2023.3.5/plugins/maven/lib/maven3/bin/mvn.cmd"
PROJECT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"

# Parse the file path from stdin JSON
FILE_PATH=$(python3 -c "import json,sys; print(json.load(sys.stdin).get('tool_input',{}).get('file_path',''))" 2>/dev/null || true)

# Only act on Java source files
if [[ "$FILE_PATH" != *.java ]]; then
    exit 0
fi

# Run Checkstyle on the whole module (fast — no compilation, no tests)
cd "$PROJECT_DIR"
cmd //c "$MVN" checkstyle:check -q 2>&1
