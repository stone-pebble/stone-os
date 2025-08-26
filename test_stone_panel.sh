#!/bin/bash
# Stone Panel Testing Script

echo "==================================="
echo "Stone Panel Testing Environment"
echo "==================================="

# 1. Download SystemUI.apk from GCP
echo "1. Downloading SystemUI.apk from GCP..."
gcloud compute scp aosp-build:~/aosp/out/target/product/generic_x86_64/system/system_ext/priv-app/SystemUI/SystemUI.apk \
    ~/Desktop/SystemUI_stone.apk \
    --zone=us-central1-a

# 2. Start Android Emulator
echo "2. Starting Android emulator..."
emulator -avd Pixel_8a_API_35 -writable-system &
EMULATOR_PID=$!

# Wait for emulator to boot
echo "Waiting for emulator to boot..."
adb wait-for-device
sleep 30

# 3. Install modified SystemUI
echo "3. Installing Stone SystemUI..."
adb root
adb remount
adb push ~/Desktop/SystemUI_stone.apk /system/system_ext/priv-app/SystemUI/SystemUI.apk
adb shell chmod 644 /system/system_ext/priv-app/SystemUI/SystemUI.apk

# 4. Restart SystemUI
echo "4. Restarting SystemUI..."
adb shell pkill -f systemui
sleep 5

# 5. Test Stone panel
echo "5. Testing Stone panel..."
echo ""
echo "=== TEST COMMANDS ==="
echo "Swipe up from bottom to show Stone panel:"
echo "  adb shell input swipe 540 2000 540 1500"
echo ""
echo "Swipe down to hide panel:"
echo "  adb shell input swipe 540 1500 540 2000"
echo ""
echo "Watch logs:"
echo "  adb logcat | grep -E 'Stone|Panel'"
echo ""
echo "==================================="