#!/bin/bash
# Sync Stone components from central repo to fork workspace

set -e

STONE_OS_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FORK_PATH="$STONE_OS_ROOT/development/fork-workspace/stoneos-frameworks"

echo "=== Syncing Stone Components to Fork ==="
echo "Source: $STONE_OS_ROOT/stone/"
echo "Target: $FORK_PATH/packages/SystemUI/src/com/android/systemui/stone/"

# Ensure fork workspace exists
if [ ! -d "$FORK_PATH" ]; then
    echo "ERROR: Fork workspace not found at $FORK_PATH"
    echo "Run: cd development/fork-workspace && git clone https://github.com/stone-pebble/stoneos-frameworks.git"
    exit 1
fi

# Copy Stone files
cp "$STONE_OS_ROOT/stone/"*.java "$FORK_PATH/packages/SystemUI/src/com/android/systemui/stone/"

echo "✅ Stone files synced successfully"
echo ""
echo "Next steps:"
echo "1. cd $FORK_PATH"
echo "2. git add -A && git commit -m 'Update Stone components'"
echo "3. git push origin android-14.0.0_r61"
echo "4. Rebuild with: cd ~/stone-os && ./scripts/build_stoneos.sh"