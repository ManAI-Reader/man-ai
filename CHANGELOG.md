# Changelog

All notable changes to this project will be documented in this file.

## [0.4.0] - 2026-02-20

### Added

- Open PDF via intent directly into reader without home screen flash
- Go-to-page dialog via page indicator tap in reader
- Navigate directly to reader after manga import
- Hide status bar in reader immersive mode
- Light/Dark/System theme mode in settings and reader

### Fixed

- Deduplicate manga by content hash instead of URI

## [0.3.0] - 2026-02-17

### Added

- RTL reading mode with settings toggle and reversed pager/slider
- Double-tap to zoom gesture in manga reader
- Pinch-to-zoom and pan gestures in manga reader
- Bottom progress bar with page slider in manga reader
- Single-tap top bar toggle in manga reader
- Kover code coverage with 80% threshold gate

## [0.2.0] - 2026-02-15

### Added

- PDF reader screen with page viewer and swipe navigation
- Fullscreen reader with overlay TopAppBar and FillWidth scaling
- Tap manga in library to open reader
- Reading progress persistence with automatic save (debounced)
- Resume reading from last saved page
- Detekt static analysis with zero-tolerance CI gate
- Instrumented tests in CI and release workflows
- Release signing configuration

## [0.1.0] - 2026-02-15

### Added

- Homepage with manga library grid layout and cover overlay
- Settings screen
- App logo and "Man AI" branding
- Multi-language support (strings externalized to strings.xml)
- Room database schema for migration validation
- CI pipeline targeting master branch
- Android project scaffold with Clean Architecture and tests
