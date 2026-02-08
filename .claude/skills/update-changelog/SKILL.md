---
name: update-changelog
description: Update CHANGELOG.md with changes for a specific version. Use when preparing a release.
---

Update `CHANGELOG.md` with the changes for version `$ARGUMENTS`.

## Rules

- Each version gets its own `## x.y.z` section
- Newest version at the top (below the `# Changelog` heading)
- List ALL changes: features, bug fixes, refactors, improvements
- Keep entries concise (one line each)
- Use plain text, no bold or special formatting per entry

## Steps

1. Read the current `CHANGELOG.md`
2. Review git log since the last tagged version: `git log $(git describe --tags --abbrev=0 2>/dev/null || git rev-list --max-parents=0 HEAD)..HEAD --oneline`
3. Group changes and write clear entries under the new version heading
4. Add the new version section at the top of the file
