# 慢愛 Man AI

[![CI](https://github.com/ManAI-Reader/man-ai/actions/workflows/ci.yml/badge.svg)](https://github.com/ManAI-Reader/man-ai/actions/workflows/ci.yml) [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE) [![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)](https://developer.android.com)

An Android manga reader with on-device AI-powered text detection, OCR, and translation support. Designed for Japanese language learners who want to read manga in their original language with intelligent assistance.

## Features

- **Manga library** — Homepage displaying imported manga with cover thumbnails
- **PDF reader** — Fullscreen page viewer with swipe navigation and overlay controls
- **RTL reading mode** — Right-to-left page order for Japanese manga with settings toggle
- **Zoom & pan** — Pinch-to-zoom, double-tap to zoom, and pan gestures with image-bound constraints
- **Page slider** — Bottom progress bar with draggable slider for quick page navigation
- **Reading progress** — Automatic save and resume from last read page

## Requirements

- Android 8.0+ (API 26)

## Build

```bash
cd android
./gradlew assembleDebug
```

### Claude Code setup

After cloning, run the detekt CLI setup for the static analysis hook:

```bash
scripts/setup-detekt.sh
```

## Architecture

MVVM + Clean Architecture with Kotlin, Jetpack Compose, Hilt, Room, and Coroutines/Flow.

```
android/app/src/main/java/com/highliuk/manai/
├── data/       # Room entities, DAOs, repository implementations
├── domain/     # Use cases, repository interfaces, domain models
└── ui/         # Compose screens, ViewModels, navigation, theme
```

## License

MIT License - see [LICENSE](LICENSE) for details.
