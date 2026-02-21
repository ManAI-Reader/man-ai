# Test Dedup Journal

Last run: 2026-02-20

## Verified Pairs (not duplicates)

(none — no source class has coverage from both unit and instrumented suites)

## Removed Tests

(none)

## Skipped (no counterpart)

### Unit-only (no instrumented counterpart)

- MangaEntityTest — data class mapping, pure logic
- PdfMetadataExtractorTest — mock-only test
- UserPreferencesRepositoryImplTest — DataStore preferences, pure logic
- MangaRepositoryImplTest — repository with mocked DAO
- MangaTest — domain model, pure logic
- MangaRepositoryTest — mock-only interface test
- ReaderGestureStateTest — gesture state object, pure logic (28 tests)
- ReaderViewModelTest — ViewModel with mocked repos
- SettingsViewModelTest — ViewModel with mocked repo
- ThemeModeResolutionTest — extension function, pure logic
- HomeViewModelTest — ViewModel with mocked repos

### Instrumented-only (no unit counterpart)

- MangaDaoTest — requires real Room database (safety rule: never remove)
- HomeScreenTest — Compose UI test for HomeScreen composable
- HomeScreenE2ETest — Compose UI E2E for HomeScreen
- MangaGridItemTest — Compose UI test for MangaGridItem composable
- GoToPageDialogTest — Compose UI test for GoToPageDialog composable
- ReaderBottomBarTest — Compose UI test for ReaderBottomBar composable
- ReaderBottomBarThemeTest — Compose UI pixel/theme test
- SettingsScreenTest — Compose UI test for SettingsScreen composable
- ManAiNavHostTest — Hilt integration + navigation (safety rule: never remove)
- ReaderScreenTest — Compose UI test for ReaderScreen composable (22 tests)

## Notes

The codebase has clean layer separation: unit tests cover ViewModels, state objects, and data/domain classes; instrumented tests cover Compose composables, DAO, and navigation. No source class is tested by both suites, so no dedup candidates exist at this time.
