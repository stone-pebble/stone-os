# Standalone App Testing Workflow Guide

**Author:** Gemini (Research Agent)
**Date:** 2025-10-23
**Related Ticket:** Research Ticket #25

---

## 1. Overview

This document outlines the official workflow for the rapid, iterative testing of standalone StoneOS system applications (e.g., `StoneLauncher`, `StoneSettings`).

This workflow is designed to be fast, allowing developers to test UI and basic app logic in minutes, without waiting for a full multi-hour AOSP build. It achieves this by installing the custom `.apk` file onto a **stock Android Virtual Device (AVD)**.

**Use this workflow for:**
-   UI development and layout testing.
-   Testing non-privileged application logic.
-   Verifying the "Head" (GUI) part of our "Head & Headless" architecture.

**Do NOT use this workflow for:**
-   Testing deep system integrations (e.g., `SurfaceFlinger` mods).
-   Verifying privileged actions that require a full system signature.
-   Final integration testing.

For those tasks, the official **Cuttlefish** environment with a full StoneOS build is required.

## 2. Environment Setup (One-Time Task)

This phase details how to set up the standard Android SDK emulator within our GCP VNC environment.

**Source:** [Android SDK Command-Line Tools](https://developer.android.com/tools)

### Step 2.1: Install SDK Command-Line Tools

```bash
# Navigate to the home directory
cd ~

# Download the latest tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip

# Create the SDK directory and unzip the tools
mkdir -p android-sdk
unzip commandlinetools-linux-11076708_latest.zip -d android-sdk
rm commandlinetools-linux-11076708_latest.zip

# The tools must be placed in a specific subdirectory structure
cd android-sdk/cmdline-tools
mkdir latest
mv bin lib NOTICE.txt source.properties latest/
cd ~
```

### Step 2.2: Install System Image, Platform Tools, and Emulator

Use the `sdkmanager` tool to download the necessary components.

```bash
# Define a shorthand for the sdkmanager
SDK_MANAGER=~/android-sdk/cmdline-tools/latest/bin/sdkmanager

# Accept licenses automatically
yes | $SDK_MANAGER --licenses

# Install the three essential packages
$SDK_MANAGER "system-images;android-34;google_apis;x86_64"
$SDK_MANAGER "platform-tools"
$SDK_MANAGER "emulator"
```

### Step 2.3: Create the Android Virtual Device (AVD)

Use the `avdmanager` tool to create the virtual device.

```bash
# Define a shorthand for the avdmanager
AVD_MANAGER=~/android-sdk/cmdline-tools/latest/bin/avdmanager

# Create the AVD
$AVD_MANAGER create avd -n stoneos_app_test -k "system-images;android-34;google_apis;x86_64" --device "pixel_8"
```

### Step 2.4: Create a Launch Script

Create a convenience script to launch the emulator with the correct settings for our VNC environment.

```bash
cat << 'EOF' > ~/stone-os/scripts/launch_stock_emulator.sh
#!/bin/bash
#
# Launches the stock AVD for standalone app testing.
# Must be run from within the VNC session.
#

echo "Starting stock Android emulator for app testing..."

~/android-sdk/emulator/emulator -avd stoneos_app_test -gpu swiftshader_indirect &
EOF

chmod +x ~/stone-os/scripts/launch_stock_emulator.sh
```

The environment is now fully prepared.

## 3. The Development Workflow

This is the rapid, iterative loop for app development.

### Step 3.1: Build the Standalone App

From the AOSP root, build **only** the target application using the `m` command.

**Example:**
```bash
cd ~/aosp
source build/envsetup.sh
lunch aosp_cf_x86_64_phone-eng
m StoneSettings
```
-   **Build Time:** ~5-10 minutes.
-   **APK Location:** The compiled `.apk` will be located in the `out/` directory, for example: `~/aosp/out/target/product/vsoc_x86_64/system/system_ext/priv-app/StoneSettings/StoneSettings.apk`.

### Step 3.2: Launch the Stock Emulator

From within the VNC session, run the launch script.

```bash
~/stone-os/scripts/launch_stock_emulator.sh
```
Wait for the emulator to fully boot to the stock Google home screen.

### Step 3.3: Install the App

Use `adb` (which was installed by `sdkmanager`) to install the `.apk` onto the running emulator.

**Example:**
```bash
# Define a shorthand for adb
ADB=~/android-sdk/platform-tools/adb

# Install the APK (use -r to reinstall/update)
$ADB install -r ~/aosp/out/target/product/vsoc_x86_64/system/system_ext/priv-app/StoneSettings/StoneSettings.apk
```
A "Success" message indicates the app is installed. It will now appear in the app drawer.

### Step 3.4: Test the App

-   **GUI ("Head") Testing:** Open the app from the emulator's app drawer and interact with the UI using your mouse. Verify layouts, colors, and navigation.
-   **API ("Headless") Testing:** Use `adb shell am broadcast` to test the `BroadcastReceiver` API you've built.

    **Example:**
    ```bash
    # Test the StoneSettings API
    $ADB shell am broadcast -a com.stoneos.settings.SET_WIFI_STATE --ez enabled true
    ```
-   **Logging:** View logs for your specific app using `adb logcat`.

    **Example:**
    ```bash
    $ADB logcat -s StoneSettings:* StoneSettingsAPI:*
    ```

### Step 3.5: Iterate

Make code changes in the `vendor/stone/packages/apps/` directory, then repeat this workflow from Step 3.1. The `adb install -r` command allows you to rapidly update the app on the emulator without needing to reboot it.
