#!/bin/bash
# Install modified SystemUI
# DANGEROUS - can bootloop device

echo "=== Installing Modified SystemUI ==="
echo "⚠️  WARNING: This can bootloop your device!"
echo "Have recovery method ready (fastboot, TWRP, etc)"
echo ""
echo "Type 'yes' to continue:"
read CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo "Aborted"
    exit 1
fi

# Backup original
echo "[1/6] Backing up original SystemUI..."
adb shell "su -c 'cp /system_ext/priv-app/SystemUI/SystemUI.apk /sdcard/SystemUI_backup.apk'"

# Push modified version
echo "[2/6] Pushing modified SystemUI..."
adb push SystemUI_stone.apk /sdcard/

# Mount system as read-write
echo "[3/6] Mounting system..."
adb shell "su -c 'mount -o rw,remount /system_ext'"

# Replace SystemUI
echo "[4/6] Replacing SystemUI..."
adb shell "su -c 'cp /sdcard/SystemUI_stone.apk /system_ext/priv-app/SystemUI/SystemUI.apk'"
adb shell "su -c 'chmod 644 /system_ext/priv-app/SystemUI/SystemUI.apk'"

# Clear SystemUI cache
echo "[5/6] Clearing cache..."
adb shell "su -c 'rm -rf /data/dalvik-cache/*systemui*'"

echo "[6/6] Rebooting..."
adb reboot

echo ""
echo "Device rebooting. Check logs with:"
echo "  adb logcat | grep StoneOS"
echo ""
echo "If bootloop, restore with:"
echo "  fastboot flash system system.img"