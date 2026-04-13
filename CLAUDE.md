# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## ⚠️ MANDATORY WORKFLOW — EXECUTE FOR EVERY TASK

For ANY code change (bug fix, feature, refactor), follow RED-GREEN-REFACTOR:

1. Write FAILING test → run `./gradlew test` → verify RED output
2. Write MINIMUM production code → run tests → verify GREEN output
3. Refactor if needed (tests must stay green)

RULES:

- Complete one full RED-GREEN-REFACTOR cycle per task before moving to the next
- Always write the failing test first, then the production code. If you wrote production code first, delete it and start over with the test
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
- **`android.util.Log` is NOT available in unit tests** — use Timber or constructor-injected loggers in ViewModels and domain logic instead
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
- **Multi-language**: every user-facing or accessibility string goes in `res/values/strings.xml` — always use `stringResource(R.string.xxx)` in composables instead of hardcoded strings

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

## Using Skills

This project vendors [superpowers v4.3.0](https://github.com/obra/superpowers) skills in `.claude/skills/`. Invoke relevant skills BEFORE any response or action — even a 1% chance a skill might apply means you MUST invoke it.

### The Rule

**If a skill applies to your task, you do not have a choice. You MUST use it.** Use the `Skill` tool to invoke skills. Never use the Read tool on skill files.

### Red Flags

These thoughts mean STOP — you're rationalizing:

| Thought | Reality |
|---------|---------|
| "This is just a simple question" | Questions are tasks. Check for skills. |
| "I need more context first" | Skill check comes BEFORE clarifying questions. |
| "Let me explore the codebase first" | Skills tell you HOW to explore. Check first. |
| "This doesn't need a formal skill" | If a skill exists, use it. |
| "I remember this skill" | Skills evolve. Read current version. |
| "The skill is overkill" | Simple things become complex. Use it. |
| "I'll just do this one thing first" | Check BEFORE doing anything. |

### Skill Priority

When multiple skills could apply:

1. **Process skills first** (brainstorming, debugging) — determine HOW to approach the task
2. **Implementation skills second** — guide execution

### Skill Types

**Rigid** (TDD, debugging): Follow exactly. Don't adapt away discipline.
**Flexible** (patterns): Adapt principles to context. The skill itself tells you which.
