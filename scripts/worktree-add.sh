#!/bin/bash
# Usage: source scripts/worktree-add.sh <branch-name>
# Must be run from the project root.

BRANCH="${1:?Usage: source scripts/worktree-add.sh <branch-name>}"
DEST="../man-ai--${BRANCH//\//-}"

git worktree add -b "$BRANCH" "$DEST"
cp -r .claude/hooks/ "$DEST/.claude/hooks/"
cp -r android/.gradle/ "$DEST/android/.gradle/"
cp -r android/.kotlin/ "$DEST/android/.kotlin/" 2>/dev/null
cp -r docs/plans/ "$DEST/docs/plans/" 2>/dev/null
cd "$DEST"
echo "Now in: $(pwd)"
