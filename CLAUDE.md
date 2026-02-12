# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

慢愛 (Man AI) — Android manga reader with on-device AI for text detection, OCR, furigana, and translation. Native Android app (Kotlin + Jetpack Compose).

## Repo Structure

- Root — repo-level config (CLAUDE.md, README, LICENSE, .github, .gitignore)
- `android/` — Android project (Gradle root)

## TDD — THE PRIME DIRECTIVE

**Every single line of production code in this project MUST be driven by a failing test.** This is not a guideline — it is the foundational principle upon which the entire codebase is built. The project was reset to zero specifically because this rule was not followed, and no code written without TDD can be trusted.

### Why TDD is Non-Negotiable

1. **Confidence**: Every feature works because a test proves it works — not because it "looks right"
2. **Regression safety**: Every bug fix has a test that reproduces the bug, so it can never silently return
3. **Design pressure**: Writing tests first forces better API design — if it's hard to test, it's hard to use
4. **Living documentation**: Tests describe what the code does, not what comments claim it does
5. **Fearless refactoring**: With full test coverage, any refactor is safe — tests catch breakage immediately

### The Red-Green-Refactor Cycle

Every change follows this exact cycle. No exceptions.

```
1. RED    — Write a test that describes the desired behavior. Run it. It MUST FAIL.
            If it passes, the test is useless — it doesn't actually verify new behavior.

2. GREEN  — Write the MINIMUM production code to make the test pass. Nothing more.
            No "while I'm here" improvements. No premature abstractions.

3. REFACTOR — Clean up both test and production code while keeping all tests green.
              Extract helpers, remove duplication, improve naming — but change no behavior.
```

### Critical Rules

- **NEVER write production code without a failing test first.** Not even "obvious" code, not even "simple" utilities, not even configuration classes. If it has logic, it needs a test.
- **Tests MUST exercise REAL code.** Import and call the actual function/class under test. A test that recreates logic in isolation without importing the source is WORTHLESS — it only tests your assumptions, not the code.
- **Verify tests catch failures.** After making a test pass, mentally (or actually) revert the production code. The test MUST fail. If it still passes, the test is a false positive — delete it and write a real one.
- **For bug fixes**: First write a test that reproduces the bug (RED). Then fix the bug (GREEN). This guarantees the bug can never return undetected.
- **Visibility for testability**: If a function needs to be tested but is `private`, make it `internal`. Testability trumps encapsulation.
- **`android.util.Log` is NOT available in unit tests.** Never use it in ViewModels or domain logic. Use a testable logging abstraction if needed.

### Test Infrastructure

- **MockK** for mocking dependencies
- **StandardTestDispatcher** for coroutine testing
- **Turbine** for Flow testing
- **JUnit 4** as test runner
- Test files mirror source structure under `src/test/java/`
- Instrumented Compose UI tests in `src/androidTest/java/` — use `createComposeRule()`

## Architecture

MVVM + Clean Architecture with three layers (inside `android/app/src/main/java/com/highliuk/manai/`):

- **data/** — Room entities, DAOs, repositories impl, API services, ML model wrappers
- **domain/** — Use cases, repository interfaces, domain models
- **ui/** — Compose screens, ViewModels, navigation, theme

DI via Hilt. Each layer only depends inward (ui → domain ← data).

### Planned Modules

- **Reader**: PDF rendering via `PdfRenderer`, page navigation, zoom/pan with Compose gestures, RTL mode
- **AI Pipeline**: Text detection → OCR → MeCab for furigana → word alignment
- **Translation**: Strategy pattern across multiple translation backends
- **Balloon UI**: Overlay Compose layer on manga pages showing detected text regions

### Data Flow (Target)

PDF import → pages rendered as bitmaps → AI pipeline detects balloons → OCR extracts text → MeCab segments words + generates furigana → translation engine translates → UI overlays results on page

## Versioning

Semantic versioning (MAJOR.MINOR.PATCH). Each release: bump `versionName` + `versionCode` in `android/app/build.gradle.kts`, update `CHANGELOG.md`, and create annotated git tag (`git tag -a vX.Y.Z`). Use the `/version` skill to automate this.

## Conventions

- All code, comments, and git commits in **English**
- Kotlin with Jetpack Compose (no XML layouts)
- Material 3 theming
- Coroutines + Flow for async (no RxJava)
- Room for local persistence
- Repository pattern: interface in domain/, implementation in data/

## MCP Servers

The project includes `.mcp.json` with shared MCP servers (context7, memory, fetch, exa, mobile) that work out of the box for all contributors.

### Optional per-user MCP setup

These require personal credentials or machine-specific paths — set them up at user level:

```bash
# GitHub MCP (requires a Personal Access Token with repo scope)
claude mcp add-json github --scope user '{"type":"http","url":"https://api.githubcopilot.com/mcp","headers":{"Authorization":"Bearer YOUR_GITHUB_PAT"}}'

# Filesystem MCP (adjust paths to your machine)
claude mcp add filesystem --scope user -- npx -y @modelcontextprotocol/server-filesystem /path/to/code /path/to/.gradle
```

### Prerequisites

- Node.js 18+ and npx (for context7, memory, mobile)
- Python 3.10+ and uvx (for fetch)
- ADB in PATH (for mobile — requires a running emulator or connected device)
