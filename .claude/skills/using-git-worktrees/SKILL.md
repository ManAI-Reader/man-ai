---
name: using-git-worktrees
description: Creates isolated git worktrees for feature work without affecting the current workspace. Use when starting feature work that needs isolation or before executing implementation plans.
---

# Using Git Worktrees

Git worktrees create isolated workspaces sharing the same repository, allowing work on multiple branches simultaneously without switching.

**Announce at start:** "I'm setting up an isolated worktree."

## Creation

This project has a dedicated script. Run from project root:

```bash
source scripts/worktree-add.sh <branch-name>
```

This will:
1. Create a new branch and worktree at `../man-ai--<branch-name>/`
2. Copy `.claude/hooks/`, `android/.gradle/`, `android/.kotlin/`, `docs/plans/`, and ML model assets
3. `cd` into the new worktree

## Verify Clean Baseline

After creation, run tests to ensure the worktree starts clean:

```bash
./gradlew test
```

**If tests fail:** Report failures, ask whether to proceed or investigate.
**If tests pass:** Report ready.

## Quick Reference

| Situation | Action |
|-----------|--------|
| Need isolated workspace | `source scripts/worktree-add.sh <branch>` |
| Tests fail during baseline | Report failures + ask |
| Done with worktree | `git worktree remove ../man-ai--<branch>` |

## Integration

**Called by:**
- **brainstorming** (Phase 4) - when design is approved and implementation follows
- **subagent-driven-development** - before executing any tasks
- **executing-plans** - before executing any tasks
- Any skill needing isolated workspace

**Pairs with:**
- **finishing-a-development-branch** - for cleanup after work complete
