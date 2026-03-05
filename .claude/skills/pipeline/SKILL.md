# OCR Pipeline Architecture

## Overview

The OCR pipeline processes manga pages through: text detection (ONNX model) → OCR recognition (per-balloon) → cache results in Room DB. Pipeline state is tracked for an optional debug overlay.

## Pipeline Flow

```
PDF page rendered as Bitmap
    → TextDetector.detect(bitmap) → List<TextRegion>
    → saved to Room (page_ocr_result table) as normalized coordinates
    → for each region: TextRecognizer.recognize(bitmap, region) → OcrResult
    → OCR text saved to Room per-region
```

## Key Components

### ProcessPageUseCase

Orchestrates detection + OCR for a single page. Called from `ReaderViewModel.launchPipeline()`.

- `execute(mangaId, pageIndex, bitmap, detectionOnly, priorityRegionIndex)`
- `detectionOnly = true`: runs detection but skips OCR (used for pre-processing)
- `priorityRegionIndex`: reorders OCR queue to process a tapped balloon first
- Checks `OcrCacheRepository.hasDetectionResults()` to skip re-detection
- Checks `region.ocrText != null` to skip already-recognized balloons

### ReaderViewModel Pipeline Behavior

- **Per-page jobs**: each page gets its own coroutine Job in `pipelineJobs: MutableMap<Int, Job>`
- **No cancellation across pages**: navigating away does NOT cancel the previous page's pipeline
- **Skip-if-active**: `launchPipeline()` is a no-op if the page already has an active job (unless `priorityRegionIndex` forces a re-launch)
- **Direct invocation**: `onPageChanged()` calls `launchPipeline()` directly (no debounce) — every visited page enters the pipeline
- **No pre-fetch**: only pages the user actually visits get processed

### OcrCacheRepository

- `hasDetectionResults(mangaId, pageIndex)` → checks if detection was already run
- **Sentinel row**: when detection finds 0 regions (e.g., cover pages), a sentinel row with `regionIndex = -1` is inserted so `hasDetectionResults` returns `true` on subsequent calls
- `getRegions()` and `observeRegions()` filter out sentinel rows (`regionIndex >= 0`)
- Detection results persist across app sessions in Room

## Debug Overlay (DEBUG_ML)

Build-time flag that enables a visual overlay showing pipeline state on each page.

### Enabling

```bash
DEBUG_ML=true ./gradlew :app:installDebug
```

The flag is injected via `BuildConfig.DEBUG_ML` (always `false` in production).

### What It Shows

**Page-level overlay** (full-page tinted rectangle):
| Color   | Status     | Meaning                              |
|---------|------------|--------------------------------------|
| Orange  | Queued     | Default state before processing      |
| Yellow  | Processing | Detection running                    |
| Green   | Done       | Detection completed in this session  |
| Blue    | CacheHit   | Detection was cached from prior session |
| Red     | Error      | Detection failed                     |

**Balloon-level overlay** (per-region rectangle):
| Color   | Status        | Meaning                          |
|---------|---------------|----------------------------------|
| Orange  | OcrQueued(n)  | Waiting, shows queue position    |
| Yellow  | OcrProcessing | OCR running on this balloon      |
| Green   | OcrDone       | OCR completed                    |
| Blue    | OcrCacheHit   | OCR was cached from prior session|
| Red     | OcrError      | OCR failed                       |

**Toast messages**: model loading/ready events and pipeline errors.

### Architecture

- `PipelineDebugStateHolder` (Singleton): `StateFlow<Map<Int, PagePipelineState>>` — updated by use cases, observed by UI
- `DebugMlEventHolder` (Singleton): `Channel<DebugMlEvent>` — one-shot events for toasts
- `DebugMlOverlay` composable: Canvas drawing gated behind `BuildConfig.DEBUG_ML`
- `OverlayCoordinateMapper`: pure function mapping normalized region coords to screen coords (ContentScale.FillWidth)

### State Guards

- **Done is sticky**: if a page is already `Done`, re-execution with cached detection does NOT overwrite it with `CacheHit`
- Same guard for `OcrDone` vs `OcrCacheHit` at balloon level
- This prevents visual flicker when revisiting pages within a session
