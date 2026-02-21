---
name: test-dedup
description: Find and safely remove instrumented tests that duplicate existing unit tests. Uses semantic analysis and mutation testing to empirically verify overlap before removal.
---

# Test Dedup — Remove Redundant Instrumented Tests

## Overview

As the project grows, some instrumented tests (slow, require emulator) end up testing the same behavior as existing unit tests. This skill finds and safely removes these duplicates using mutation-based verification to empirically prove two tests cover the same behavior.

**Core principle:** Never remove an instrumented test based on gut feeling. Prove empirically that a unit test already covers the same behavior before removing.

## When to Use

- Periodically (e.g., every few weeks) to keep the test suite lean
- When instrumented test suite runtime is growing noticeably
- After a large feature lands with both unit and instrumented tests

## Safety Rules — NON-NEGOTIABLE

These rules are hardcoded. No exceptions. No overrides.

| Rule | Reason |
|------|--------|
| **Never remove** tests with `@HiltAndroidTest` or `createAndroidComposeRule<Activity>` | They test real DI wiring — unit tests cannot replicate this |
| **Never remove** DAO tests (`data/local/dao/`) | They require real Room database |
| **Never remove** the only test for a source class | Zero coverage is always worse than slow coverage |
| **Never remove** navigation tests (`ManAiNavHostTest` and similar) | Navigation wiring is untestable in unit tests |
| **When in doubt, keep the instrumented test** | False negative (keeping a duplicate) is cheap; false positive (removing needed coverage) is dangerous |
| **Always run full suite after removals** | Verify no regressions before presenting results |

## Phase 0 — Setup and Journal

1. Read `docs/test-dedup-journal.md` (if it exists)
2. For every pair already verified in the journal, check if the test files were modified since last check:

```bash
git log --oneline --since="<last-checked-date>" -- <unit-test-file> <instrumented-test-file>
```

3. If not modified → skip the pair. If modified or new → re-analyze

4. If the journal does not exist, create it with this template:

```markdown
# Test Dedup Journal

Last run: <today>

## Verified Pairs (not duplicates)

(none yet)

## Removed Tests

(none yet)

## Skipped (no counterpart)

(none yet)
```

## Phase 1 — Semantic Triage (no test execution)

**Goal:** Identify candidate pairs without running any tests yet.

### Step 1: Scan test files

Scan all files under:
- `android/app/src/test/java/` (unit tests)
- `android/app/src/androidTest/java/` (instrumented tests)

### Step 2: Build source-to-test map

For each test file, identify the source class/function it exercises by looking at:
- Import statements
- Class instantiation and method calls
- Assertion targets

Build a map: `SourceClass → [unit tests, instrumented tests]`

### Step 3: Classify each source class with coverage from BOTH suites

Read the test code and classify:

| Classification | Criterion | Action |
|---|---|---|
| **Different layer** | The instrumented test asserts UI behavior (node existence, `assertIsDisplayed`, `performClick`, gestures, `onNode*`) | **Skip** — not a duplicate |
| **Suspected overlap** | Both tests verify the same logical behavior (same input → same output/state, similar assertions on domain logic) | → **Phase 2** |
| **No counterpart** | Only unit OR only instrumented test exists | **Skip** — log in journal |

### Step 4: Apply safety rules filter

Before promoting any pair to Phase 2, verify it does NOT match any safety rule:
- Contains `@HiltAndroidTest` → skip
- Contains `createAndroidComposeRule<` with an Activity type parameter → skip
- Is in `data/local/dao/` → skip
- Is a navigation test → skip
- Is the only test for its source class → skip

## Phase 2 — Mutation Verification (only suspected pairs)

For each suspected pair from Phase 1:

### Step 1: Identify the mutation target

Find the line of production code that both tests depend on. This should be:
- A return value
- A state mutation
- A function call
- A boolean condition

### Step 2: Stash current state

```bash
git stash push -m "test-dedup: mutation for <SourceClass>" -- <source-file>
```

### Step 3: Apply a targeted mutation

Examples of mutations:
- Invert a boolean return (`return true` → `return false`)
- Change a return value (`return list` → `return emptyList()`)
- Comment out a critical call
- Swap an argument

**Keep the mutation minimal** — one line change only.

### Step 4: Run the unit test

```bash
cd android && ./gradlew testDebugUnitTest --tests "<fully.qualified.TestClass.testMethod>"
```

### Step 5: Run the instrumented test

```bash
cd android && ./gradlew connectedStagingAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<fully.qualified.TestClass>
```

### Step 6: Analyze results

| Unit | Instrumented | Verdict |
|---|---|---|
| FAIL | FAIL | → **Level 2 analysis** (see below) |
| FAIL | PASS | Different behavior tested — **keep both** |
| PASS | FAIL | Different behavior tested — **keep both** |
| PASS | PASS | Mutation ineffective — try a different mutation (max 3 attempts) |

**Level 2 analysis** (when both fail):

Read the failure messages carefully:
- If the instrumented test fails on a **logical assertion** (wrong value, wrong state, wrong count) and NOT on a **UI assertion** (node not found, not displayed, click failed) → **duplicate confirmed**
- If the instrumented test fails on a UI assertion → **not a duplicate**, keep both

### Step 7: Restore

```bash
git stash pop
```

### Step 8: Record result in journal

Whether duplicate or not, record the pair in the journal with:
- Pair names
- Date checked
- Verdict
- Reasoning (one line)

**Repeat for each suspected pair. Maximum 3 mutation attempts per pair before declaring "inconclusive" and keeping both.**

## Phase 3 — Removal and Verification

### Step 1: Remove confirmed duplicates

For each confirmed duplicate:
1. Delete the instrumented test method (or entire class if it has no other tests)
2. Add a comment in the unit test:

```kotlin
// Also covers behavior previously tested in <InstrumentedTestClass>
```

### Step 2: Run full test suite

```bash
cd android && ./gradlew testDebugUnitTest
cd android && ./gradlew connectedStagingAndroidTest
```

**Both must be green.**

### Step 3: Handle failures

- If any test fails → **revert all removals immediately** (`git checkout -- <files>`)
- Investigate the failure — the tests were NOT duplicates
- Update journal with corrected verdict

### Step 4: Update journal

Update `docs/test-dedup-journal.md`:
- Set `Last run: <today>`
- Move confirmed duplicates to `## Removed Tests` with date and reasoning
- Update `## Verified Pairs (not duplicates)` with new non-duplicate pairs
- Update `## Skipped (no counterpart)` with tests that have no counterpart

### Step 5: Present results to user

Show a summary:
- How many pairs analyzed
- How many duplicates found and removed
- How many kept (with reasons)
- Ask for confirmation before committing

**Do NOT commit without user confirmation.**

## Journal Format (`docs/test-dedup-journal.md`)

```markdown
# Test Dedup Journal

Last run: 2026-02-20

## Verified Pairs (not duplicates)

### HomeViewModelTest ↔ HomeScreenTest
- **Last checked**: 2026-02-20
- **Verdict**: Different layers — ViewModel logic vs Compose UI rendering
- **Reason**: HomeScreenTest asserts node existence and display state; HomeViewModelTest asserts Flow emissions with mocked repo

## Removed Tests

### ExampleInstrumentedTest (removed 2026-02-20)
- **Unit counterpart**: ExampleUnitTest
- **Mutation**: Inverted return value in ExampleClass.compute()
- **Evidence**: Both failed with value assertion errors; instrumented test had no UI assertions

## Skipped (no counterpart)

- MangaDaoTest (no unit counterpart — requires real Room)
- GoToPageDialogTest (no unit counterpart — pure UI)
- ManAiNavHostTest (Hilt integration — never a candidate)
```

## Common Rationalizations

| Excuse | Reality |
|--------|---------|
| "The instrumented test is obviously redundant" | Prove it with a mutation. Obvious is not empirical. |
| "Both tests check the same function" | Same function ≠ same behavior. The instrumented test may exercise DI wiring, lifecycle, or UI rendering the unit test cannot. |
| "Removing this will save CI time" | Saving 5 seconds is not worth losing coverage. Only remove if mutation proves redundancy. |
| "I'll just mark it as skipped" | Skipped tests are forgotten tests. Remove or keep — no middle ground. |
| "The mutation was inconclusive, but they look the same" | Inconclusive = keep both. The safety rule is clear. |
