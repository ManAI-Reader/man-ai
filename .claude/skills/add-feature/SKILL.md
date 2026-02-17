---
name: add-feature
description: Add a feature using strict TDD with E2E verification. Use when implementing new functionality.
---

# Add Feature — Full TDD + E2E Verification

## Phase 1: TDD for EVERY task

For each task in the plan, complete a RED-GREEN-REFACTOR cycle:

1. Write failing test → **run it** → verify RED with visible failure output
2. Write minimum production code → run tests → verify GREEN
3. Refactor if needed

**Critical rules:**
- **Every task gets a test. No exceptions.** "Not easily testable" means "requires more setup" — do the setup.
- **RED means you SAW the failure.** Compilation alone is not RED. For instrumented tests, run on emulator with `connectedStagingAndroidTest` and show the assertion error.
- **If the plan says "not testable", the plan is wrong.** Fix the plan, don't skip the test.
- Navigation, Hilt wiring, DB queries — all testable. Use `@HiltAndroidTest`, `TestDatabaseModule`, `createAndroidComposeRule<MainActivity>()` as needed.

## Phase 2: E2E Test (after all unit-level tasks are done)

Write an instrumented test that simulates the REAL USER FLOW end-to-end.

The test must:

- Use `createComposeRule()` or `createAndroidComposeRule<MainActivity>()`
- Simulate actual user actions (tap buttons, select files, navigate)
- Assert the final observable outcome the user would see
- Cover the HAPPY PATH at minimum, plus obvious failure modes

Example for PDF import:

```kotlin
@get:Rule
val composeTestRule = createAndroidComposeRule<MainActivity>()

@Test
fun importPdf_showsInLibrary() {
    // Tap import button
    composeTestRule.onNodeWithText("Import").performClick()
    // ... handle file picker intent with intended()
    // Assert PDF appears in library
    composeTestRule.onNodeWithText("my-manga.pdf").assertIsDisplayed()
}
```

Run: `cd android && ./gradlew connectedStagingAndroidTest`
If RED → fix production code and re-run until GREEN.

## Phase 2.5: Full regression check

After E2E tests pass, run the **complete** test suite to catch regressions:

```bash
cd android && ./gradlew testDebugUnitTest
cd android && ./gradlew connectedStagingAndroidTest
```

Both must be green. Unit tests alone miss Compose UI regressions. Do NOT proceed to smoke test until both suites pass.

## Phase 3: Manual Smoke Test

After all tests pass:

1. Install the app: `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`
2. Launch: `adb shell am start -n com.highliuk.manai/.MainActivity`
3. Manually execute the same flow the E2E test covers
4. Take a screenshot: `adb exec-out screencap -p > /tmp/smoke-test.png`
5. Verify visually that everything looks correct
6. If anything is wrong, use the `/fix-bug` workflow (RED-GREEN-REFACTOR) to fix it — do NOT skip TDD even for issues found during smoke testing

## Rules

- NEVER skip Phase 2. Unit tests alone do NOT verify real device behavior.
- NEVER skip Phase 3. E2E tests can pass while the UX is still broken.
- The E2E test must be written BEFORE you consider the feature complete.
- If E2E tests fail, the feature is NOT done — keep fixing.
- Bugs found in Phase 2 or 3 follow the /fix-bug TDD cycle — no exceptions.
