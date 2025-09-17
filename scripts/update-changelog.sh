#!/bin/bash

# update-changelog.sh
# Script to update CHANGELOG.md when creating a new release
# Compatible with GitHub Actions and local development

set -euo pipefail

# Function to show usage
usage() {
    echo "Usage: $0 <version> [previous_version]"
    echo ""
    echo "Arguments:"
    echo "  version          New version (e.g., 2.1.0)"
    echo "  previous_version Optional previous version for comparison (auto-detected if not provided)"
    echo ""
    echo "Examples:"
    echo "  $0 2.1.0"
    echo "  $0 2.1.0 2.0.2"
    echo ""
    echo "Environment variables:"
    echo "  GITHUB_REPOSITORY Repository in format owner/repo (for GitHub Actions)"
    echo "  DRY_RUN          Set to 'true' to show changes without modifying files"
    exit 1
}

# Check if version is provided
if [ $# -lt 1 ]; then
    echo "Error: Version is required"
    usage
fi

VERSION="$1"
PREVIOUS_VERSION="${2:-}"
CHANGELOG_FILE="CHANGELOG.md"
DATE=$(date +%Y-%m-%d)
DRY_RUN="${DRY_RUN:-false}"

# Validate version format (basic semver check)
if ! echo "$VERSION" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.-]+)?$'; then
    echo "Error: Version '$VERSION' is not in valid semver format (e.g., 2.1.0 or 2.1.0-alpha.1)"
    exit 1
fi

# Check if CHANGELOG.md exists
if [ ! -f "$CHANGELOG_FILE" ]; then
    echo "Error: $CHANGELOG_FILE not found"
    exit 1
fi

# Auto-detect previous version if not provided
if [ -z "$PREVIOUS_VERSION" ]; then
    echo "Auto-detecting previous version..."
    if command -v git >/dev/null 2>&1; then
        PREVIOUS_VERSION=$(git tag -l "v*" --sort=-version:refname | head -n1 | sed 's/^v//')
        if [ -n "$PREVIOUS_VERSION" ]; then
            echo "Detected previous version: $PREVIOUS_VERSION"
        else
            echo "Warning: No previous git tags found, using placeholder"
            PREVIOUS_VERSION="2.0.2"
        fi
    else
        echo "Warning: Git not available, using default previous version"
        PREVIOUS_VERSION="2.0.2"
    fi
fi

# Create backup if not in dry run mode
if [ "$DRY_RUN" != "true" ]; then
    cp "$CHANGELOG_FILE" "${CHANGELOG_FILE}.backup"
    echo "Created backup: ${CHANGELOG_FILE}.backup"
fi

# Generate repository URL for links
REPO_URL=""
if [ -n "${GITHUB_REPOSITORY:-}" ]; then
    REPO_URL="https://github.com/${GITHUB_REPOSITORY}"
elif command -v git >/dev/null 2>&1; then
    ORIGIN_URL=$(git remote get-url origin 2>/dev/null || echo "")
    if [[ "$ORIGIN_URL" =~ github\.com[:/]([^/]+)/([^/]+)(\.git)?$ ]]; then
        REPO_URL="https://github.com/${BASH_REMATCH[1]}/${BASH_REMATCH[2]}"
    fi
fi

# Create temporary file with updated changelog
TEMP_FILE=$(mktemp)

# Function to update changelog
update_changelog() {
    local version="$1"
    local prev_version="$2"
    local date="$3"
    local repo_url="$4"

    # Read the original file and process it
    awk -v version="$version" -v prev_version="$prev_version" -v date="$date" -v repo_url="$repo_url" '
    BEGIN {
        updated_unreleased = 0
        in_unreleased = 0
        found_unreleased = 0
    }

    # Match the [Unreleased] section header
    /^## \[Unreleased\]/ {
        found_unreleased = 1
        in_unreleased = 1

        # Print new unreleased section
        print "## [Unreleased]"
        print ""
        print "### Added"
        print "### Changed"
        print "### Deprecated"
        print "### Removed"
        print "### Fixed"
        print "### Security"
        print ""

        # Print the new version section
        print "## [v" version "] - " date
        updated_unreleased = 1
        next
    }

    # Skip the next empty line after [Unreleased] and continue until next section
    in_unreleased && /^## / && !/^## \[Unreleased\]/ {
        in_unreleased = 0
        print $0
        next
    }

    # Skip content in unreleased section (it becomes the new version content)
    in_unreleased {
        next
    }

    # Update links section
    /^## Links/ {
        print "## Links"
        if (repo_url != "") {
            print "- [Unreleased]: " repo_url "/compare/v" version "...HEAD"
            print "- [v" version "]: " repo_url "/compare/v" prev_version "...v" version
        }
        print "- [Repository](" repo_url ")"
        print "- [Documentation](docs/)"
        print "- [Issue Tracker](" repo_url "/issues)"

        # Skip the rest of the links section
        while (getline > 0 && !/^---/ && !/^$/ && !/^#/) {
            # Skip existing links
        }
        if (/^---/ || /^$/ || /^#/) {
            print $0
        }
        next
    }

    # Print all other lines as-is
    {
        print $0
    }

    END {
        if (!found_unreleased) {
            print "Warning: [Unreleased] section not found in changelog" > "/dev/stderr"
        }
    }
    ' "$CHANGELOG_FILE" > "$TEMP_FILE"
}

echo "Updating changelog for version $VERSION..."
echo "Previous version: $PREVIOUS_VERSION"
echo "Date: $DATE"

# Update the changelog
update_changelog "$VERSION" "$PREVIOUS_VERSION" "$DATE" "$REPO_URL"

# Show diff if in dry run mode
if [ "$DRY_RUN" = "true" ]; then
    echo ""
    echo "=== DRY RUN: Changes that would be made to $CHANGELOG_FILE ==="
    echo ""
    if command -v diff >/dev/null 2>&1; then
        diff -u "$CHANGELOG_FILE" "$TEMP_FILE" || true
    else
        echo "Diff not available, showing new file content:"
        echo "--- New changelog content ---"
        head -50 "$TEMP_FILE"
        echo "--- (truncated) ---"
    fi
    echo ""
    echo "=== END DRY RUN ==="
else
    # Replace the original file
    mv "$TEMP_FILE" "$CHANGELOG_FILE"
    echo "✅ Successfully updated $CHANGELOG_FILE"
    echo ""
    echo "Changes made:"
    echo "- Moved [Unreleased] content to [v$VERSION] - $DATE"
    echo "- Created new empty [Unreleased] section"
    echo "- Updated links section with version comparison URLs"

    if [ -f "${CHANGELOG_FILE}.backup" ]; then
        echo ""
        echo "💡 Backup created at ${CHANGELOG_FILE}.backup"
        echo "   You can restore with: mv ${CHANGELOG_FILE}.backup $CHANGELOG_FILE"
    fi
fi

# Clean up temp file if it still exists
[ -f "$TEMP_FILE" ] && rm -f "$TEMP_FILE"

echo ""
echo "🎉 Changelog update complete!"

if [ "$DRY_RUN" != "true" ]; then
    echo ""
    echo "Next steps:"
    echo "1. Review the updated $CHANGELOG_FILE"
    echo "2. Commit the changes: git add $CHANGELOG_FILE && git commit -m 'docs: update changelog for v$VERSION'"
    echo "3. Create and push the release tag: git tag v$VERSION && git push origin v$VERSION"
fi