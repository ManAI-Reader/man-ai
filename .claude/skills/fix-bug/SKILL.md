---
name: fix-bug
description: Fix a bug using strict TDD. Use when fixing bugs, resolving issues, or when the user reports something broken.
---

# Fix Bug via TDD

For EACH bug fix, follow this cycle internally. Do NOT combine steps.

## For each fix:

### 1. RED — Write the failing test and RUN IT

- Write a test that reproduces the bug
- Choose the right test level:
  - Logic bug → unit test with `cd android && ./gradlew test`
  - Device/permission/UI bug → instrumented test with `cd android && ./gradlew connectedStagingAndroidTest`
- **RUN the test and show the failure output.** A RED you didn't execute is not a RED.
- Compilation failure counts as RED only for unit tests. For instrumented tests, you MUST run on emulator.
- If the test passes, the test is wrong — rewrite it.
- If the test needs extra setup (Hilt `TestDatabaseModule`, `@HiltAndroidTest`, etc.), do the setup — don't skip the test.

### 2. GREEN — Minimal fix

- Write the MINIMUM production code to make the test pass
- Run the SAME test command you used in step 1 (unit or instrumented)
- ALL tests must pass. If not, fix until green.

### 3. REFACTOR (optional)

- Clean up while keeping tests green

## Rules

- NEVER write production code before its test exists and has been shown RED
- **RED means visible failure output.** Not "it would fail" — you must see the error.
- Each task in the plan = one RED-GREEN-REFACTOR cycle
- **Everything is testable.** Navigation, Hilt wiring, DB queries — all of it. "Hard to test" means "requires more setup", not "skip the test".
- If you realize you wrote production code first, DELETE IT, write the test,
  verify RED, then rewrite the production code

## Final verification (after GREEN)

Before declaring the bug fixed, run the **full** test suite — both unit AND instrumented:

```bash
cd android && ./gradlew testDebugUnitTest
cd android && ./gradlew connectedStagingAndroidTest
```

A fix is NOT complete until both suites are green. Unit tests alone miss Compose UI regressions.
