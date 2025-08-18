#!/bin/bash
# Minimal SystemUI modification - PROOF OF CONCEPT
# This REPLACES SystemUI to prove we can control the OS

echo "=== StoneOS SystemUI Replacement POC ==="
echo "We are REPLACING Android's SystemUI"
echo ""

# Find a simple class to modify
echo "[1/4] Modifying SystemUI boot class..."
TARGET_FILE="SystemUI_decompiled/smali/com/android/systemui/BootCompleteCacheImpl.smali"

if [ ! -f "$TARGET_FILE" ]; then
    echo "ERROR: Run apktool d SystemUI_original.apk first"
    exit 1
fi

# Add Stone indicator to boot
echo "[2/4] Adding Stone OS marker..."
cat > stone_patch.txt << 'EOF'
    # StoneOS Active
    const-string v0, "StoneOS"
    const-string v1, "SystemUI Replaced - Stone Controls This Device"
    invoke-static {v0, v1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
EOF

# Insert our code
sed -i.bak '/.prologue/r stone_patch.txt' "$TARGET_FILE"

echo "[3/4] Rebuilding modified SystemUI..."
apktool b SystemUI_decompiled -o SystemUI_stone.apk 2>&1 | grep -E "I:|W:|E:" | tail -5

echo "[4/4] Signing with debug key..."
jarsigner -keystore ~/.android/debug.keystore -storepass android SystemUI_stone.apk androiddebugkey 2>/dev/null

echo ""
echo "✓ SystemUI modified and ready"
echo "✓ This APK will REPLACE Android's SystemUI"
echo "✓ Run: ./install-systemui.sh"