---
name: version
description: Bump the app version, update changelog, and create a git tag. Use when releasing a new version.
---

Release version `$ARGUMENTS`.

## Steps

1. Update `versionName` to `$ARGUMENTS` in `android/app/build.gradle.kts`
2. Bump `versionCode` by 1
3. Run `/update-changelog $ARGUMENTS` to update `CHANGELOG.md`
4. Run `/update-readme` to ensure README features are current
5. Commit all changes with message: `Release v$ARGUMENTS`
6. Create an annotated git tag: `git tag -a v$ARGUMENTS -m "Release v$ARGUMENTS"`
7. Do NOT push unless the user explicitly asks
8. If the user asks to push, also create a GitHub release: `gh release create v$ARGUMENTS --title "v$ARGUMENTS" --notes "<changelog entries for this version>"`

## Version format

- Semantic versioning: MAJOR.MINOR.PATCH
- MAJOR = breaking changes
- MINOR = new features
- PATCH = bug fixes
