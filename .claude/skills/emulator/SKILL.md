# Emulator Management

Start the Android emulator, install and launch the app. Use this before returning to the user after any code change.

## List Available AVDs

```bash
emulator -list-avds
```

## Start Emulator

```bash
# Start with visible window (NO -no-window flag — user needs to see it)
emulator -avd <AVD_NAME> -no-audio &>/dev/null &
```

Then wait for boot:

```bash
adb wait-for-device && sleep 20 && adb shell getprop sys.boot_completed
# Returns "1" when ready
```

**Important**: The `emulator` command MUST run in the background (`&`). It never terminates on its own. Do NOT use `run_in_background: true` for the Bash tool — just append `&` and redirect output.

## Install and Launch App

```bash
# Install (after assembleDebug)
adb install -r android/app/build/outputs/apk/debug/app-debug.apk

# Launch
adb shell am start -n com.highliuk.manai/.MainActivity
```

## Full Workflow (build + install + launch)

This is the standard workflow to run after every code change:

```bash
# 1. Build
cd android && ./gradlew assembleDebug --no-daemon --console=plain 2>&1

# 2. Install and launch
adb install -r android/app/build/outputs/apk/debug/app-debug.apk && \
adb shell am start -n com.highliuk.manai/.MainActivity
```

## MCP Mobile — Interacting with Devices

The MCP mobile tools work with both emulators and physical devices.

### Discover device ID

**ALWAYS** start by listing available devices — never hardcode a device ID:

```
mobile_list_available_devices → pick the device from the list
```

Cache the device ID for the rest of the session, but never persist it in code or docs.

### Discover screen size

Screen resolution varies per device/emulator and can change if the user resizes the window. **ALWAYS** call `mobile_get_screen_size` at the start of a session if you need coordinates. Never assume a fixed resolution.

### Golden rule: `list_elements_on_screen` BEFORE clicking

**NEVER guess coordinates.** Always use `mobile_list_elements_on_screen` to get the exact pixel coordinates of elements before clicking. Screenshot images are scaled down and DO NOT match real device pixels — never calculate click coordinates from screenshots.

```
1. mobile_list_elements_on_screen → get JSON with real coordinates
2. mobile_click_on_screen_at_coordinates → use coordinates from the JSON
```

### Minimize screenshots and round-trips

**Don't take redundant screenshots.** If you already know what's on screen:

- **Use `list_elements_on_screen`** as the primary source of truth — it's faster than a screenshot and gives exact coordinates
- **Take screenshots only when** you need to verify visual appearance (layout, colors, gradients) or when you genuinely don't know what's on screen
- **If you know the action sequence**, execute all steps in a row without stopping for intermediate screenshots

### Batch actions in rapid sequence

When you need to perform a known sequence (e.g. importing 4 PDFs), do it all in a row:

```
Example: importing 4 PDFs from the Download folder

1. list_elements_on_screen → find FAB "Import PDF"
2. click FAB
3. (file picker opens) → list_elements_on_screen ONCE to learn the layout
4. click first PDF
5. click FAB again
6. click second PDF
... and so on

DON'T take screenshots after every click. After the first import you already know how the file picker works.
```

### Android File Picker

When the file picker opens (`OpenDocument`):

- PDF files in `/sdcard/Download/` appear in the "Downloads" or "Recents" view
- Use `list_elements_on_screen` to find the file names
- Click directly on the file name

### Common patterns

| Action           | How                                                                             |
| ---------------- | ------------------------------------------------------------------------------- |
| Tap element      | `list_elements_on_screen` → find coordinates → `click_on_screen_at_coordinates` |
| Navigate back    | `mobile_press_button` with button="BACK"                                        |
| Scroll list      | `mobile_swipe_on_screen` direction="up"/"down"                                  |
| Type text        | `click_on_screen_at_coordinates` on field → `mobile_type_keys`                  |
| Visual check     | `mobile_take_screenshot` (only for visual appearance)                           |
| Structural check | `mobile_list_elements_on_screen` (for content and position)                     |

### Pushing files and Media Scanner

To upload files to the device:

```bash
# Push file
adb push "/local/path/file.pdf" "/sdcard/Download/file.pdf"

# Trigger media scanner (required for the file picker to find them)
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
  -d "file:///sdcard/Download/file.pdf"
```

**Note**: Files with spaces in the name work, but spaces in the broadcast URL must be encoded as `%20`.

### Clear app data

```bash
adb shell pm clear com.highliuk.manai   # Wipes DB, DataStore, cache
```

## Check Device Status

```bash
adb devices                          # List connected devices
adb shell getprop sys.boot_completed # "1" if booted
```

## Stop Emulator

```bash
adb emu kill
```

## Restart Emulator (if stuck)

```bash
adb emu kill; sleep 2; emulator -avd <AVD_NAME> -no-audio &>/dev/null &
adb wait-for-device && sleep 20 && adb shell getprop sys.boot_completed
```

## Gotchas

- **Never use `-no-window`** — the user needs to see the emulator
- The emulator process runs indefinitely. Don't wait for it to finish.
- `adb wait-for-device` returns as soon as ADB connects, but the OS may not be fully booted yet. Always follow with `sleep 20` + `getprop sys.boot_completed` check.
- If `adb devices` shows "offline", kill and restart the emulator.
- APK path after build: `android/app/build/outputs/apk/debug/app-debug.apk`
- App package: `com.highliuk.manai`, main activity: `.MainActivity`
- **Screenshot coordinates are SCALED** — never use them for clicking. Always use `list_elements_on_screen`.
- **Speed matters**: the user is faster at manual testing. For smoke tests, go with rapid action sequences without intermediate screenshots.
- **Never hardcode** device IDs, AVD names, or screen resolutions — always discover them at runtime.
