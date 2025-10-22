#!/bin/bash
#
# sync_vendor.sh
#
# This script provides a unified method for synchronizing all custom StoneOS source
# code from the stone-os development repository to the AOSP source tree.
# It is the single source of truth for preparing a build.
#
# Usage:
#   ./scripts/sync_vendor.sh           # Syncs all code
#   ./scripts/sync_vendor.sh --build   # Syncs all code and then builds the entire StoneOS product
#

set -e

# --- Configuration ---
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

STONE_OS_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
AOSP_ROOT="$HOME/aosp"

SRC_DIR="$STONE_OS_ROOT/vendor/stone"
DEST_DIR="$AOSP_ROOT/vendor/stone"

# --- Main Logic ---

echo -e "${BLUE}Starting unified sync of 'vendor/stone' to AOSP source tree...${NC}"

if [ ! -d "$SRC_DIR" ]; then
    echo "Error: Source directory $SRC_DIR not found."
    exit 1
fi

if [ ! -d "$AOSP_ROOT" ]; then
    echo "Error: AOSP source directory $AOSP_ROOT not found."
    exit 1
fi

# Use rsync to efficiently copy all custom code.
# The --delete flag ensures that files removed from our dev repo are also removed from the AOSP tree.
rsync -av --delete "$SRC_DIR/" "$DEST_DIR/"

echo -e "${GREEN}✓ Sync complete.${NC}"

# --- Optional Build Step ---
if [[ "$1" == "--build" ]]; then
  echo -e "${BLUE}Build flag detected. Starting full StoneOS product build...${NC}"
  echo -e "This will build the entire system image, including all custom components."
  
  cd "$AOSP_ROOT"
  source build/envsetup.sh >/dev/null 2>&1
  
  # Use the Cuttlefish target, which is our current standard
  lunch aosp_cf_x86_64_phone-eng >/dev/null 2>&1
  
  # Build the entire product
  m
  
  echo -e "${GREEN}✓ StoneOS build command finished.${NC}"
fi
