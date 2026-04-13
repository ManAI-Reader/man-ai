---
name: instrumented-testing
description: Provides anti-flakiness patterns for Android instrumented tests (Compose UI and UiAutomator). Use when writing or debugging instrumented tests, especially when tests are flaky, use Thread.sleep, have StaleObjectException, or fail intermittently on CI.
---

# Instrumented Testing — Anti-Flakiness Guide

## Overview

Flaky instrumented tests waste CI time and erode trust. **Core principle:** wait for conditions, never for time.

## When to Use

- Writing new instrumented tests (Compose or UiAutomator)
- Debugging flaky test failures
- Reviewing instrumented test code
- Migrating tests from `Thread.sleep` to proper waits

## Prerequisites

### testTagsAsResourceId

This project uses `Modifier.semantics { testTagsAsResourceId = true }` on the root composable in `ManAiNavHost`. This exposes all Compose `testTag` values to UiAutomator as `resource-id`, enabling `By.res("tag_name")`.

**Without this, UiAutomator cannot find Compose elements by testTag.**

## Compose Test API — Patterns

Compose tests use `createComposeRule()` with a controlled clock. These are deterministic by design.

```kotlin
// Wait for Compose to finish all pending work
composeTestRule.waitForIdle()

// Wait for a condition with timeout
composeTestRule.waitUntil(timeoutMillis = 10_000) {
    composeTestRule.onAllNodesWithText("Target").fetchSemanticsNodes().isNotEmpty()
}

// Advance virtual clock (animations, LaunchedEffect)
composeTestRule.mainClock.advanceTimeBy(500)
```

These are **not flaky** because Compose test clock is deterministic.

## UiAutomator — Patterns

UiAutomator injects real touch events and reads the accessibility tree. It does **not** know about Compose's internal state.

### The Cardinal Rule

```
NEVER use Thread.sleep(). ALWAYS wait for a condition.
```

### Wait for screen transitions

After navigation (click → new screen), wait for an element **unique to the target screen**:

```kotlin
// BAD — Compose animations are invisible to UiAutomator
Thread.sleep(500)
device.click(x, y)

// GOOD — wait for a concrete element proving you're on the right screen
device.wait(Until.findObject(By.res("webtoon_viewer")), 5_000)
device.click(x, y)
```

Use `By.res("test_tag")` for Compose testTags (requires `testTagsAsResourceId`).

### Never chain wait().click()

`device.wait(Until.findObject(...))` can return `null`. Chaining `.click()` on it causes NPE with no useful message.

```kotlin
// BAD — NPE if element not found
device.wait(Until.findObject(By.text("OK")), 5000).click()

// GOOD — assert before interacting
val button = device.wait(Until.findObject(By.text("OK")), 5000)
assertNotNull("OK button should appear", button)
button.click()
```

### Avoid StaleObjectException

Between `findObject` and `click`, Compose may recompose, invalidating the UiObject2 reference.

```kotlin
// BAD — element may be recomposed between wait and click
device.wait(Until.findObject(By.text("Item")), 5000).click()

// GOOD — wait for stability, then get fresh reference
device.wait(Until.hasObject(By.text("Item")), 5000)
device.waitForIdle()
device.findObject(By.text("Item")).click()
```

Use this pattern after screen transitions (back, navigation, rotation).

### Orientation changes

After rotation, the Activity may be recreated. Wait for a screen-specific element:

```kotlin
device.setOrientationLeft()
// BAD
Thread.sleep(500)
// GOOD — wait for the target screen to re-render
device.wait(Until.findObject(By.res("webtoon_viewer")), 5000)
```

### Wait for disappearance

After toggling UI (e.g., hiding bars), wait for the element to be gone:

```kotlin
device.click(x, y) // toggle bars off
// BAD
Thread.sleep(500)
// GOOD
device.wait(Until.gone(By.textContains("/ 10")), 5000)
```

### Swipe / scroll settling

After a swipe, `waitForIdle()` is acceptable since it waits for the fling to finish:

```kotlin
device.swipe(x1, y1, x2, y2, steps)
device.waitForIdle()
```

## Ambiguous contentDescription

If the same `contentDescription` exists on multiple screens (e.g., "PDF placeholder" on both home and reader), **do not use it to detect navigation**. Use a unique `testTag` via `By.res()` instead.

## Timeout Values

- `3_000ms` — tight, fails on slow CI emulators
- `5_000ms` — safe default for most waits
- `10_000ms` — for first-load or heavy operations

## Flakiness Verification

**After writing or modifying instrumented tests, run them 3 times consecutively** to catch intermittent failures early:

```bash
for i in 1 2 3; do
  echo "=== Run $i ==="
  ./gradlew connectedIsolatedAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.example.MyTest \
    2>&1 | grep -E 'Tests |BUILD'
done
```

Only run the new/changed tests — never the full suite for verification loops.

## Quick Reference

| Situation | Pattern |
|-----------|---------|
| Wait for screen | `device.wait(Until.findObject(By.res("tag")), 5000)` |
| Click safely | `val el = device.wait(...); assertNotNull(msg, el); el.click()` |
| After navigation back | `device.wait(Until.hasObject(By.text("X")), 5000); device.waitForIdle(); device.findObject(By.text("X")).click()` |
| After orientation change | `device.wait(Until.findObject(By.res("screen_tag")), 5000)` |
| After toggle/dismiss | `device.wait(Until.gone(By.text("X")), 5000)` |
| After swipe | `device.waitForIdle()` |
| Compose idle | `composeTestRule.waitForIdle()` |
| Compose condition | `composeTestRule.waitUntil(timeoutMillis) { condition }` |

## waitForIdle() Traps

`waitForIdle()` means "the UI framework has no pending work RIGHT NOW". It does **not** mean "everything I triggered has finished". These are common traps where `waitForIdle()` returns immediately but the state you need hasn't arrived yet:

| After... | waitForIdle sees | What you actually need |
|----------|-----------------|----------------------|
| `requestedOrientation = LANDSCAPE` | Compose is idle (rotation hasn't started) | `waitUntil { config.orientation == LANDSCAPE }` |
| Navigation click | Compose is idle (animation not started) | `waitUntil { target screen element exists }` |
| `device.click()` on Compose UI | View framework is idle (Compose still processing) | `device.wait(Until.findObject(...))` for result |
| `pressBack()` | View framework is idle (navigation pending) | `device.wait(Until.hasObject(...))` for home element |

**Rule of thumb:** if `waitForIdle()` is followed by an assertion about NEW state (new screen, new orientation, new element), it's almost certainly wrong. Wait for the specific condition instead.

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| `Thread.sleep(N)` | Wait for a condition |
| `device.wait(...).click()` | Split into wait + assertNotNull + click |
| `device.findObject()` without wait | Add `device.wait(Until.hasObject(...))` first |
| `By.desc("X")` on ambiguous description | Use `By.res("unique_test_tag")` |
| `waitForIdle()` after orientation change | `waitUntil { resources.configuration.orientation == expected }` |
| `waitForIdle()` after navigation | Wait for target screen element |
| Timeout = 3000ms | Use 5000ms minimum for CI |
