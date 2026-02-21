#!/bin/bash
set -euo pipefail

# Read hook input from stdin
INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

# Only check Kotlin files inside the android project
if [[ -z "$FILE_PATH" ]] || [[ "$FILE_PATH" != *.kt ]]; then
  exit 0
fi

if [[ "$FILE_PATH" != *"/android/"* ]]; then
  exit 0
fi

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "$0")/../.." && pwd)}"
HOOKS_DIR="$PROJECT_DIR/.claude/hooks"
DETEKT_JAR="$HOOKS_DIR/detekt-cli-1.23.8-all.jar"
DETEKT_CONFIG="$PROJECT_DIR/android/config/detekt/detekt.yml"
CLASSPATH_FILE="$HOOKS_DIR/detekt-classpath.txt"

# Verify detekt CLI exists
if [[ ! -f "$DETEKT_JAR" ]]; then
  exit 0
fi

# Verify config exists
if [[ ! -f "$DETEKT_CONFIG" ]]; then
  exit 0
fi

# Build classpath args for type resolution
CLASSPATH_ARGS=""
if [[ -f "$CLASSPATH_FILE" ]]; then
  CLASSPATH=$(cat "$CLASSPATH_FILE")
  if [[ -n "$CLASSPATH" ]]; then
    CLASSPATH_ARGS="--classpath $CLASSPATH --language-version 2.0 --jvm-target 17 --analysis-mode full"
  fi
fi

# Run detekt on the single file
OUTPUT=$(java -jar "$DETEKT_JAR" \
  --input "$FILE_PATH" \
  --config "$DETEKT_CONFIG" \
  $CLASSPATH_ARGS \
  --build-upon-default-config \
  --max-issues 0 2>&1) || {
  echo "detekt found issues in $(basename "$FILE_PATH"):" >&2
  echo "$OUTPUT" >&2
  exit 2
}

exit 0
