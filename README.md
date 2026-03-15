# 慢愛 Man AI

[![CI](https://github.com/ManAI-Reader/man-ai/actions/workflows/ci.yml/badge.svg)](https://github.com/ManAI-Reader/man-ai/actions/workflows/ci.yml) [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE) [![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)](https://developer.android.com)

An Android manga reader with on-device AI-powered text detection, OCR, and translation support. Designed for Japanese language learners who want to read manga in their original language with intelligent assistance.

## Features

- **Tap-to-OCR** — Tap any text balloon to read Japanese text, entirely on-device with no cloud dependency
- **Selectable OCR text** — Select, copy, and look up recognized text with the native toolbar
- **Reading modes** — Paged, RTL, and webtoon continuous scroll with landscape support
- **Zoom & pan** — Pinch-to-zoom, double-tap zoom, and free pan
- **Manga library** — Import PDFs with cover thumbnails and reading progress
- **Open via intent** — Tap any PDF to jump straight into the reader
- **13 languages** — Full UI localization with per-app language switching
- **Theming** — Light, Dark, and System theme

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
