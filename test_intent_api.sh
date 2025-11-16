#!/bin/bash
#
# Stone Launcher Intent API Test Script
#
# This script tests the Intent API foundation by sending broadcast Intents
# to the StoneApiReceiver and monitoring the results.
#
# Usage:
#   ./test_intent_api.sh
#
# Requirements:
#   - Android device or emulator connected via adb
#   - Stone Launcher app installed
#   - adb in PATH
#

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# App package name
PACKAGE="com.stonelauncher"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Stone Launcher Intent API Test Script${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo -e "${RED}ERROR: adb not found in PATH${NC}"
    echo "Please install Android SDK Platform-Tools"
    exit 1
fi

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo -e "${RED}ERROR: No Android device connected${NC}"
    echo "Please connect a device or start an emulator"
    exit 1
fi

echo -e "${GREEN}✓ adb found${NC}"
echo -e "${GREEN}✓ Device connected${NC}"
echo ""

# Function to send intent and wait for logs
send_intent() {
    local test_name="$1"
    local action="$2"
    shift 2
    local extras="$@"

    echo -e "${YELLOW}Test: $test_name${NC}"
    echo -e "${BLUE}Action: $action${NC}"

    if [ -n "$extras" ]; then
        echo -e "${BLUE}Extras: $extras${NC}"
        adb shell am broadcast -a "$action" $extras
    else
        adb shell am broadcast -a "$action"
    fi

    echo ""
    sleep 0.5
}

# Clear logcat before tests
echo -e "${BLUE}Clearing logcat...${NC}"
adb logcat -c

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Running Tests${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Test 1: Unknown Action (should return error)
send_intent \
    "Unknown Action (should error)" \
    "com.stone.launcher.action.UNKNOWN_ACTION"

# Test 2: Get WiFi State (placeholder implementation)
send_intent \
    "Get WiFi State" \
    "com.stone.launcher.action.GET_WIFI_STATE"

# Test 3: Set WiFi Enabled (placeholder implementation)
send_intent \
    "Set WiFi Enabled" \
    "com.stone.launcher.action.SET_WIFI" \
    "--ez enabled true"

# Test 4: Set WiFi Disabled (placeholder implementation)
send_intent \
    "Set WiFi Disabled" \
    "com.stone.launcher.action.SET_WIFI" \
    "--ez enabled false"

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Test Results (from logcat)${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Show relevant logs
echo -e "${YELLOW}StoneApiReceiver logs:${NC}"
adb logcat -d -s StoneApiReceiver:* | tail -20

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Test Complete${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "${GREEN}Tests completed successfully!${NC}"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "1. Review the logs above to verify Intent handling"
echo "2. Check that placeholder responses are returned correctly"
echo "3. Implement TICKET_002 (WiFi Controller) for real functionality"
echo ""
echo -e "${YELLOW}To monitor live logs:${NC}"
echo "  adb logcat -s StoneApiReceiver:*"
echo ""
echo -e "${YELLOW}To send custom Intents:${NC}"
echo "  adb shell am broadcast -a com.stone.launcher.action.GET_WIFI_STATE"
echo ""
