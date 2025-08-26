#!/bin/bash
# Quick AOSP Setup Script - Sets up everything from scratch in ~30-40 minutes
# No need to keep disk storage between sessions!

set -e

echo "🚀 Quick AOSP Setup for StoneOS"
echo "================================"
echo "This script will:"
echo "1. Create a new GCP instance"
echo "2. Download AOSP source (~30 mins)"
echo "3. Copy Stone files"
echo "4. Build SystemUI (~20 mins)"
echo "5. Download the APK"
echo "6. DELETE the instance (no storage charges!)"
echo ""
echo "Total time: ~50-60 minutes"
echo "Total cost: ~$0.77-$1.00"
echo ""
read -p "Press Enter to continue or Ctrl+C to cancel..."

# Create instance
echo "📦 Creating GCP instance..."
./gcp_aosp_instance.sh create

# Wait for instance to be ready
sleep 10

# Install dependencies and download AOSP in one go
echo "📥 Setting up AOSP (this will take ~30 minutes)..."
gcloud compute ssh aosp-build --zone=us-central1-a --command "
    # Install dependencies
    sudo apt-get update
    sudo apt-get install -y git-core gnupg flex bison build-essential zip curl \
        zlib1g-dev gcc-multilib g++-multilib libc6-dev-i386 libncurses5 \
        lib32ncurses5-dev x11proto-core-dev libx11-dev lib32z1-dev libgl1-mesa-dev \
        libxml2-utils xsltproc unzip fontconfig python3 python-is-python3 repo

    # Download AOSP
    mkdir -p ~/aosp && cd ~/aosp
    repo init -u https://android.googlesource.com/platform/manifest -b android-14.0.0_r61 --depth=1
    repo sync -c -j8 --no-tags --no-clone-bundle

    echo 'AOSP download complete!'
"

# Copy Stone files
echo "📝 Copying Stone files..."
gcloud compute ssh aosp-build --zone=us-central1-a --command "
    mkdir -p ~/aosp/frameworks/base/packages/SystemUI/src/com/android/systemui/stone/
"

# Copy the Stone files
gcloud compute ssh aosp-build --zone=us-central1-a --command "cat > ~/aosp/frameworks/base/packages/SystemUI/src/com/android/systemui/stone/StonePanel.java << 'EOF'
$(cat /Users/samuellarson/Pebble/Github/stone-os/SystemUI/stone/StonePanel.java)
EOF"

gcloud compute ssh aosp-build --zone=us-central1-a --command "cat > ~/aosp/frameworks/base/packages/SystemUI/src/com/android/systemui/stone/StoneIcon.java << 'EOF'
$(cat /Users/samuellarson/Pebble/Github/stone-os/SystemUI/stone/StoneIcon.java)
EOF"

# Build SystemUI
echo "🔨 Building SystemUI (this will take ~20 minutes)..."
gcloud compute ssh aosp-build --zone=us-central1-a --command "
    cd ~/aosp
    source build/envsetup.sh
    lunch aosp_x86_64-ap2a-eng
    m SystemUI -j8
"

# Download the built APK
echo "📲 Downloading SystemUI.apk..."
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
gcloud compute scp aosp-build:~/aosp/out/target/product/generic_x86_64/system/system_ext/priv-app/SystemUI/SystemUI.apk \
    /Users/samuellarson/Pebble/Github/stone-os/SystemUI_stone_${TIMESTAMP}.apk \
    --zone=us-central1-a

echo "✅ SystemUI.apk downloaded as SystemUI_stone_${TIMESTAMP}.apk"

# Delete the instance completely
echo "🗑️ Deleting GCP instance to avoid storage charges..."
./gcp_aosp_instance.sh delete

echo ""
echo "🎉 Complete! Total cost: ~\$0.77-\$1.00"
echo "📱 Your SystemUI.apk is ready at: SystemUI_stone_${TIMESTAMP}.apk"
echo ""
echo "To test in emulator:"
echo "  emulator -avd Pixel_8a_API_35 -writable-system"
echo "  adb root && adb remount"
echo "  adb push SystemUI_stone_${TIMESTAMP}.apk /system/system_ext/priv-app/SystemUI/SystemUI.apk"
echo "  adb reboot"