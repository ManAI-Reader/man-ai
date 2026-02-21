---
name: commit
description: Create atomic git commits. Use when committing changes to the repository.
allowed-tools: Bash(git *)
---

Create atomic git commits for the current changes.

## Rules

- Each commit = **one logical change** (one feature, one fix, one refactor)
- Do NOT mix conceptually different changes in a single commit
- If there are unrelated changes staged, split them into separate commits
- Commit message style: imperative mood, concise first line (< 72 chars)
- Always end with `Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>`
- Do NOT push unless the user explicitly asks
- Before committing, verify `./gradlew detekt` passes (the PostToolUse hook catches issues per-file, but run full project detekt before commit as final check)
- Before committing, run **both** test suites:
  - `cd android && ./gradlew testDebugUnitTest` — unit tests (JVM)
  - `cd android && ./gradlew connectedStagingAndroidTest` — instrumented tests (emulator)
  - If no emulator is running, warn the user and ask to start one before committing
  - **NEVER skip instrumented tests** — unit tests alone miss Compose UI and device-level regressions

## Steps

1. Run `git status` and `git diff` to see all changes
2. Group changes by logical unit (feature, fix, refactor, docs, etc.)
3. For each group:
   a. Stage only the files belonging to that group (`git add <specific files>`)
   b. Write a clear commit message describing the **why**, not the **what**
   c. Commit
4. Verify with `git log --oneline` that the history reads cleanly
