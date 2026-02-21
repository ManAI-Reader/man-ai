# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## ⚠️ MANDATORY WORKFLOW — EXECUTE FOR EVERY TASK

For ANY code change (bug fix, feature, refactor), follow RED-GREEN-REFACTOR:

1. Write FAILING test → run `./gradlew test` → verify RED output
2. Write MINIMUM production code → run tests → verify GREEN output
3. Refactor if needed (tests must stay green)

RULES:

- Complete one full RED-GREEN-REFACTOR cycle per task before moving to the next
- NEVER write production code before its failing test exists
- If you wrote production code first, DELETE IT and start over with the test
- Always run and show test output at each step

## Project

慢愛 (Man AI) — Android manga reader with on-device AI for text detection, OCR, furigana, and translation. Native Android app (Kotlin + Jetpack Compose).

## Repo Structure

- Root — repo-level config (CLAUDE.md, README, LICENSE, .github, .gitignore)
- `android/` — Android project (Gradle root)

## Test Infrastructure & Rules

- **MockK** for mocking, **Turbine** for Flow, **StandardTestDispatcher** for coroutines
- **JUnit 4** as test runner
- Test files mirror source structure under `src/test/java/`
- Instrumented Compose UI tests in `src/androidTest/java/` — use `createComposeRule()`
- **`android.util.Log` is NOT available in unit tests** — never use it in ViewModels or domain logic
- **Visibility for testability**: if a function needs testing but is `private`, make it `internal`
- **Tests MUST import and call REAL code** — a test that recreates logic without importing the source is worthless
- **detekt** for static analysis — runs automatically via Claude Code hook on every `.kt` file edit
- `./gradlew detekt` for full project analysis with type resolution (CI gate)
- Zero tolerance: `maxIssues = 0` — all violations must be fixed, not suppressed (unless confirmed false positive)

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
- **Multi-language**: every user-facing or accessibility string goes in `res/values/strings.xml` — use `stringResource(R.string.xxx)` in composables, NEVER hardcode strings

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
