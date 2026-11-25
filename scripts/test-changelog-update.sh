#!/bin/bash

# test-changelog-update.sh
# Test script to demonstrate changelog update functionality

set -euo pipefail

echo "🧪 Testing Changelog Update Script"
echo "=================================="
echo ""

# Test with dry run. The version must not have a section in CHANGELOG.md yet, and
# [Unreleased] must have notes; otherwise the script stops with an error.
DEMO_VERSION="${1:-99.0.0}"
echo "1. Testing dry run mode with version $DEMO_VERSION (no changes will be made):"
echo ""
DRY_RUN=true ./scripts/update-changelog.sh "$DEMO_VERSION"

echo ""
echo "2. Available commands:"
echo ""
echo "   # Test the script (dry run):"
echo "   DRY_RUN=true ./scripts/update-changelog.sh 2.1.0"
echo ""
echo "   # Actually update the changelog:"
echo "   ./scripts/update-changelog.sh 2.1.0"
echo ""
echo "   # Update with specific previous version:"
echo "   ./scripts/update-changelog.sh 2.1.0 2.0.2"
echo ""
echo "   # Restore from backup (if needed):"
echo "   mv CHANGELOG.md.backup CHANGELOG.md"
echo ""

echo "3. Integration with GitHub Actions:"
echo ""
echo "   Run the update script and commit CHANGELOG.md before pushing the tag."
echo "   The release workflow does not run the script. It will:"
echo "   - Read the release notes for the tagged version from CHANGELOG.md"
echo "   - Stop before building if a final release has no notes"
echo "   - Use the notes for the GitHub release"
echo ""

echo "✅ Test complete!"
echo ""
echo "💡 To see what the script would do, run:"
echo "   DRY_RUN=true ./scripts/update-changelog.sh 2.1.0"