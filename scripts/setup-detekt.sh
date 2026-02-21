#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
HOOKS_DIR="$PROJECT_DIR/.claude/hooks"
DETEKT_VERSION="1.23.8"
DETEKT_JAR="$HOOKS_DIR/detekt-cli-${DETEKT_VERSION}-all.jar"

echo "Setting up detekt CLI v${DETEKT_VERSION}..."

# Download CLI if missing
if [[ ! -f "$DETEKT_JAR" ]]; then
  echo "Downloading detekt CLI..."
  curl -sSL -o "$DETEKT_JAR" \
    "https://github.com/detekt/detekt/releases/download/v${DETEKT_VERSION}/detekt-cli-${DETEKT_VERSION}-all.jar"
  echo "Downloaded to $DETEKT_JAR"
else
  echo "detekt CLI already present."
fi

# Build project if needed and cache classpath
echo "Building project and caching classpath..."
cd "$PROJECT_DIR/android"
./gradlew assembleDebug --no-daemon --console=plain > /dev/null 2>&1
./gradlew printDetektClasspath --no-daemon --console=plain 2>&1 \
  | grep 'DETEKT_CLASSPATH=' | sed 's/DETEKT_CLASSPATH=//' \
  > "$HOOKS_DIR/detekt-classpath.txt"

echo "Setup complete. detekt hook is ready."
