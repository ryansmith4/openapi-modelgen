# Release Scripts

This directory contains automation scripts for the OpenAPI Model Generator plugin release process.

## Scripts

### `update-changelog.sh`

Automatically updates `CHANGELOG.md` when creating a new release by:
- Moving `[Unreleased]` content to a new versioned section
- Creating a fresh `[Unreleased]` section for future changes
- Updating version comparison links

**Usage:**
```bash
# Update changelog for version 2.1.0 (auto-detects previous version)
./scripts/update-changelog.sh 2.1.0

# Update with specific previous version
./scripts/update-changelog.sh 2.1.0 2.0.2

# Test without making changes (dry run)
DRY_RUN=true ./scripts/update-changelog.sh 2.1.0
```

The script stops without changing anything if the version already has a section, or if `[Unreleased]`
is missing or empty.

**GitHub Actions Integration:**
Run the script yourself and commit the result before tagging. The release workflow
(`.github/workflows/release.yml`) does not run it: it reads the release notes from the tagged commit's
`CHANGELOG.md` and stops before building if a final release has none.

### `test-changelog-update.sh`

Demonstrates the changelog update functionality with dry run examples.

**Usage:**
```bash
./scripts/test-changelog-update.sh
```

## Release Process

The full list of steps is in "Release Process" in [docs/development.md](../docs/development.md). The
changelog part works as follows:

1. **Developer**: Runs `update-changelog.sh <version>`, reviews and commits `CHANGELOG.md`, gets the commit
   onto `main`, then tags it and pushes the tag (e.g., `git tag v2.1.0 && git push origin v2.1.0`).
   Pre-release tags (alpha, beta, rc) skip the script.

2. **GitHub Actions** (`.github/workflows/release.yml`):
   - Extracts version from tag
   - Reads the release notes for that version from `CHANGELOG.md`, and stops if a final release has none
   - Builds and tests the plugin
   - Publishes to Gradle Plugin Portal
   - Creates GitHub release with the release notes

3. **Result**:
   - Plugin published to Gradle Plugin Portal
   - GitHub release created with structured release notes

## Features

- **Automatic**: Moves the notes and rewrites the comparison links; no hand editing of headers
- **Flexible**: Works with any semver version format
- **Safe**: Creates backups and supports dry-run testing
- **Compatible**: Integrates seamlessly with existing GitHub Actions
- **Standards-compliant**: Follows [Keep a Changelog](https://keepachangelog.com/) format

## Manual Usage

For local testing or manual releases:

```bash
# Test what would happen
DRY_RUN=true ./scripts/update-changelog.sh 2.1.0

# Actually update the changelog
./scripts/update-changelog.sh 2.1.0

# Review changes
git diff CHANGELOG.md

# Commit and tag
git add CHANGELOG.md
git commit -m "docs: update changelog for v2.1.0"
git tag v2.1.0
git push origin main v2.1.0
```

The GitHub Actions workflow will then handle the rest of the release process.