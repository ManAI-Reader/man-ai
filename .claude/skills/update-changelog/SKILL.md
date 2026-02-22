---
name: update-changelog
description: Update CHANGELOG.md with changes for a specific version. Use when preparing a release.
---

Update `CHANGELOG.md` with the changes for version `$ARGUMENTS`.

## Rules

- Each version gets its own `## [x.y.z] - YYYY-MM-DD` section (ISO 8601 date)
- Newest version at the top (below the `# Changelog` heading)
- Follow [Keep a Changelog](https://keepachangelog.com/) format
- Group entries under `### Added`, `### Changed`, `### Deprecated`, `### Removed`, `### Fixed`, `### Security` — only include categories that have entries
- Within each category, order entries by user impact (user-facing features first, infrastructure/tooling last)
- Keep entries concise (one line each)
- Use plain text, no bold or special formatting per entry
- Omit internal dev tooling changes (e.g. MCP config) that don't affect the project
- **Bug fixes must reference bugs that existed in the previous release.** If a commit fixes something introduced by another commit in the same release cycle, it is NOT a changelog-worthy fix — it's just part of developing that feature. Only list fixes for bugs that a user of the previous version could have experienced. When in doubt, check whether the broken behavior existed in the previous tag.

## Steps

1. Read the current `CHANGELOG.md`
2. Review git log since the last tagged version: `git log $(git describe --tags --abbrev=0 2>/dev/null || git rev-list --max-parents=0 HEAD)..HEAD --oneline`
3. Categorize changes into the appropriate Keep a Changelog groups
4. Write clear entries ordered by user impact within each group
5. Add the new version section at the top of the file
