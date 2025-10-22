#!/bin/bash

# Launch Stock Android Emulator for StoneOS App Testing
# This script launches the stock Android 14 emulator configured for VNC use

set -e

# Set Android SDK paths
export ANDROID_SDK_ROOT=/home/samuellarson/stone-os/android-sdk
export PATH=$ANDROID_SDK_ROOT/emulator:$ANDROID_SDK_ROOT/platform-tools:$PATH

# Check if DISPLAY is set (should be :1 in VNC)
if [ -z "$DISPLAY" ]; then
    echo "WARNING: DISPLAY not set. You must run this from within the VNC session."
    echo "Expected DISPLAY=:1"
    exit 1
fi

echo "=========================================="
echo "Launching StoneOS App Test Emulator"
echo "=========================================="
echo "AVD: stoneos_app_test"
echo "System Image: Android 14 (API 34) Google APIs"
echo "GPU Mode: swiftshader_indirect (VNC compatible)"
echo "=========================================="
echo ""

# Launch the emulator in the background
# -gpu swiftshader_indirect is CRITICAL for VNC compatibility
# -accel off disables hardware acceleration (required for cloud VMs without KVM)
# -no-snapshot-load ensures clean boot
$ANDROID_SDK_ROOT/emulator/emulator \
    -avd stoneos_app_test \
    -gpu swiftshader_indirect \
    -accel off \
    -no-snapshot-load \
    -no-boot-anim \
    &

EMULATOR_PID=$!

echo "Emulator starting... (PID: $EMULATOR_PID)"
echo ""
echo "Waiting for emulator to boot..."
echo "This may take 1-2 minutes on first boot."
echo ""
echo "Once booted, you can:"
echo "  - Test APKs: adb install app.apk"
echo "  - Check status: adb devices"
echo "  - View logs: adb logcat"
echo ""
echo "To stop the emulator: adb emu kill"
echo "=========================================="
