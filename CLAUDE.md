# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

慢愛 (Man AI) — Android manga reader with on-device AI for text detection, OCR, furigana, and translation. Native Android app (Kotlin + Jetpack Compose).

## Build & Run

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests
./gradlew test --tests "com.highliuk.manai.SomeTest.someMethod"  # Single test
./gradlew lint                   # Run Android lint
./gradlew ktlintCheck            # Check Kotlin style
./gradlew ktlintFormat           # Auto-fix Kotlin style
```

## Architecture

MVVM + Clean Architecture with three layers:

- **data/** — Room entities, DAOs, repositories impl, API services, ML model wrappers
- **domain/** — Use cases, repository interfaces, domain models
- **ui/** — Compose screens, ViewModels, navigation, theme

DI via Hilt. Each layer only depends inward (ui → domain ← data).

### Key Modules

- **Reader**: PDF rendering via `PdfRenderer`, page navigation, zoom/pan with Compose gestures, RTL mode
- **AI Pipeline**: Text detection (Onnxruntime) → OCR (Onnxruntime) → MeCab (JNI) for furigana → word alignment
- **Translation**: Strategy pattern across Google Translate offline, DeepL API, ChatGPT API
- **Balloon UI**: Overlay Compose layer on manga pages showing detected text regions with interactive modals

### Data Flow

PDF import → pages rendered as bitmaps → AI pipeline detects balloons → OCR extracts text → MeCab segments words + generates furigana → translation engine translates → UI overlays results on page

## Conventions

- All code, comments, and git commits in **English**
- Kotlin with Jetpack Compose (no XML layouts)
- Material 3 theming
- Coroutines + Flow for async (no RxJava)
- Room for local persistence
- Repository pattern: interface in domain/, implementation in data/
