# Gradle Commands

## Key Rules

- The Android project lives in `android/`. Always `cd` there before running Gradle.
- **Builds** (`assembleDebug`, `assembleRelease`): use `run_in_background: true` — they are slow (minutes) and will timeout if run synchronously. Use `--no-daemon --console=plain` for predictable output.
- **Tests** (`test`, `connectedStagingAndroidTest`): run in **foreground** — you need to see the output immediately to verify RED/GREEN in the TDD cycle.
- Set `timeout: 120000` (2 minutes) for background tasks. Do NOT use longer timeouts.
- **Never block** with `TaskOutput` on a background Gradle command. Instead, periodically check the output file with `Read` to monitor progress.
- **Do other work** while background builds run. Don't poll the output file in a tight loop.

## Pattern

```
# Launch in background
Bash(command="cd .../android && ./gradlew <task> --no-daemon --console=plain 2>&1", run_in_background=true, timeout=120000)

# Check progress by reading the output file (returned in the tool result)
Read(file_path="/private/tmp/.../tasks/<task_id>.output")
```

## Common Tasks

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumented tests (needs device/emulator)
./gradlew test --tests "com.highliuk.manai.SomeTest.someMethod"  # Single test
./gradlew detekt                  # Static analysis (full project, type resolution)
./gradlew detektGenerateConfig    # Regenerate default detekt config
./gradlew lint                   # Android lint
./gradlew ktlintCheck            # Check Kotlin style
./gradlew ktlintFormat           # Auto-fix Kotlin style
```

## Generating the Wrapper

```bash
# Requires system `gradle` CLI (e.g. via Homebrew)
cd android && gradle wrapper --gradle-version <version> --no-daemon
```

This also takes minutes on first run (downloads the distribution). Run in background.

## Gotchas

- First build downloads the Gradle distribution + all dependencies. Can take 5-10 minutes.
- `gradle wrapper` uses the _system_ `gradle`, not `./gradlew`.
- The `--no-daemon` flag prevents orphan daemon processes that consume RAM.
