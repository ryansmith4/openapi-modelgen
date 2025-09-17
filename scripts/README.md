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

**GitHub Actions Integration:**
The script is automatically called by `.github/workflows/release.yml` when a version tag is pushed.

### `test-changelog-update.sh`

Demonstrates the changelog update functionality with dry run examples.

**Usage:**
```bash
./scripts/test-changelog-update.sh
```

## Release Process

The automated release process works as follows:

1. **Developer**: Creates and pushes a version tag (e.g., `git tag v2.1.0 && git push origin v2.1.0`)

2. **GitHub Actions** (`.github/workflows/release.yml`):
   - Extracts version from tag
   - Updates `gradle.properties` with new version
   - Builds and tests the plugin
   - **Runs `update-changelog.sh`** to update CHANGELOG.md
   - Commits the updated changelog
   - Publishes to Gradle Plugin Portal
   - Creates GitHub release with changelog content

3. **Result**:
   - Plugin published to Gradle Plugin Portal
   - GitHub release created with structured release notes
   - CHANGELOG.md updated and committed back to repository

## Features

- **Automatic**: Zero manual changelog editing required
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