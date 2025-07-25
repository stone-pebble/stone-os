# StoneOS Development Guide

## Prerequisites

### Hardware Requirements

- **Development Machine**:
  - 64-bit Linux (Ubuntu 20.04+ recommended)
  - Minimum 16GB RAM (32GB+ recommended)
  - 400GB+ free disk space
  - Multi-core processor (8+ cores recommended)

- **Target Device**:
  - Google Pixel 8a (primary target)
  - Unlocked bootloader
  - USB debugging enabled

### Software Requirements

```bash
# Install required packages (Ubuntu/Debian)
sudo apt-get update
sudo apt-get install -y \
    git-core gnupg flex bison build-essential \
    zip curl zlib1g-dev gcc-multilib g++-multilib \
    libc6-dev-i386 libncurses5 lib32ncurses5-dev \
    x11proto-core-dev libx11-dev lib32z1-dev \
    libgl1-mesa-dev libxml2-utils xsltproc unzip \
    fontconfig python3 python3-pip nodejs npm \
    openjdk-11-jdk repo
```

## Environment Setup

### 1. Directory Structure

```bash
# Create workspace
mkdir -p ~/stoneos-workspace
cd ~/stoneos-workspace

# Directory layout
stoneos-workspace/
├── aosp/              # AOSP source tree
├── patches/           # StoneOS patches
├── ui/                # React Native UI
├── mcp/               # Master Control Program
├── agents/            # AI agents
├── tools/             # Build tools
└── out/               # Build output
```

### 2. Download AOSP

```bash
cd ~/stoneos-workspace
mkdir aosp && cd aosp

# Initialize repo with Android 14
repo init -u https://android.googlesource.com/platform/manifest \
    -b android-14.0.0_r1

# Sync source code (this will take several hours)
repo sync -c -j8 --force-sync --no-clone-bundle --no-tags
```

### 3. Clone StoneOS Components

```bash
cd ~/stoneos-workspace

# Clone patches
git clone https://github.com/pebble/stoneos-patches patches

# Clone UI
git clone https://github.com/pebble/stone-ui ui

# Clone MCP
git clone https://github.com/pebble/stoneos-mcp mcp

# Clone agents
git clone https://github.com/pebble/stone-agents agents
```

## Build Process

### 1. Apply StoneOS Patches

```bash
cd ~/stoneos-workspace/aosp

# Apply all patches
../patches/apply-all.sh

# Verify patches applied
git status
```

### 2. Configure Build Environment

```bash
# Set up environment
source build/envsetup.sh

# Select build target
lunch stoneos_pixel8a-userdebug

# Set build options
export TARGET_BUILD_VARIANT=userdebug
export ANDROID_JACK_VM_ARGS="-Xmx8g -Dfile.encoding=UTF-8"
```

### 3. Build Master Control Program

```bash
cd ~/stoneos-workspace/mcp

# Build MCP service
./gradlew build

# Copy to AOSP tree
cp build/outputs/*.jar ../aosp/vendor/pebble/mcp/
```

### 4. Build React Native UI

```bash
cd ~/stoneos-workspace/ui

# Install dependencies
npm install

# Build for Android
npm run build:android

# Generate Android bundle
npx react-native bundle \
    --platform android \
    --dev false \
    --entry-file index.js \
    --bundle-output ../aosp/vendor/pebble/ui/bundle.js \
    --assets-dest ../aosp/vendor/pebble/ui/assets
```

### 5. Build StoneOS

```bash
cd ~/stoneos-workspace/aosp

# Clean build (first time)
make clobber

# Build StoneOS (this will take 2-4 hours)
make -j$(nproc) dist

# Build specific modules (faster for development)
make -j$(nproc) StoneUI MasterControlProgram SystemImage
```

## Development Workflow

### Fast Development Cycle

For rapid UI development without full OS rebuilds:

```bash
# 1. Make UI changes
cd ~/stoneos-workspace/ui
# ... edit files ...

# 2. Build and push UI only
npm run build:android
adb root
adb remount
adb push build/android/* /system/app/StoneUI/

# 3. Restart UI
adb shell am force-stop com.stoneos.ui
adb shell am start com.stoneos.ui/.MainActivity
```

### Testing MCP Changes

```bash
# 1. Build MCP module
cd ~/stoneos-workspace/aosp
make -j$(nproc) MasterControlProgram

# 2. Push to device
adb push out/target/product/pixel8a/system/bin/mcp_service /system/bin/

# 3. Restart service
adb shell stop mcp
adb shell start mcp

# 4. Check logs
adb logcat -s MCP:*
```

### Agent Development

```bash
# 1. Set up Python environment
cd ~/stoneos-workspace/agents
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# 2. Test agent locally
python test_agent.py

# 3. Deploy to device
./deploy_agent.sh router_agent
```

## Flashing & Testing

### 1. Unlock Bootloader (One-time)

```bash
# Enable OEM unlocking in Developer Options
adb reboot bootloader
fastboot flashing unlock
# Confirm on device
```

### 2. Flash StoneOS

```bash
cd ~/stoneos-workspace/aosp

# Flash all partitions
fastboot flashall -w

# Or flash specific partitions
fastboot flash boot out/target/product/pixel8a/boot.img
fastboot flash system out/target/product/pixel8a/system.img
fastboot flash vendor out/target/product/pixel8a/vendor.img
fastboot reboot
```

### 3. Debug Tools

```bash
# Monitor boot process
adb wait-for-device
adb logcat -b all > boot.log

# Check StoneOS services
adb shell ps | grep stone
adb shell dumpsys mcp

# UI debugging
adb shell setprop debug.stoneos.ui true
adb shell am broadcast -a com.stoneos.DEBUG_MODE

# Capture bug report
adb bugreport stoneos-bugreport.zip
```

## Common Development Tasks

### Adding a New System Service

1. Create service in `frameworks/base/services/core/java/com/android/server/stoneos/`
2. Register in `SystemServer.java`
3. Add AIDL interface in `frameworks/base/core/java/android/stoneos/`
4. Update SELinux policies
5. Create patch and add to patch system

### Modifying UI Components

1. Edit React Native components in `ui/src/`
2. Test in development mode: `npm run dev`
3. Build production bundle
4. Test on device
5. Commit changes

### Integrating New MCP Module

1. Create module in `mcp/modules/`
2. Implement MCP interface
3. Register in `MasterControlProgram.kt`
4. Add permissions in manifest
5. Test with sample agent

## Build Optimization

### Incremental Builds

```bash
# Use ccache for faster rebuilds
export USE_CCACHE=1
export CCACHE_DIR=~/stoneos-workspace/.ccache
prebuilts/misc/linux-x86/ccache/ccache -M 50G

# Parallel builds
export ANDROID_BUILD_ENVIRONMENT_CPU_COUNT=$(nproc)

# Skip unnecessary steps
export SKIP_ABI_CHECKS=true
export BOARD_PREBUILT_VENDORIMAGE=true
```

### Development Shortcuts

```bash
# Build and flash in one command
alias stoneos-flash='make -j$(nproc) && fastboot flashall'

# Quick UI update
alias ui-push='npm run build:android && adb push build/android/* /system/app/StoneUI/'

# Monitor all StoneOS logs
alias stone-logs='adb logcat -s StoneOS:* MCP:* StoneUI:* Agent:*'
```

## Troubleshooting

### Build Errors

1. **Out of Memory**
   ```bash
   # Increase heap size
   export JACK_SERVER_VM_ARGUMENTS="-Xmx8g"
   ```

2. **Patch Conflicts**
   ```bash
   # Reset and reapply
   repo forall -c 'git reset --hard'
   ../patches/apply-all.sh
   ```

3. **Missing Dependencies**
   ```bash
   # Check all dependencies
   ../tools/check-deps.sh
   ```

### Device Issues

1. **Boot Loop**
   - Flash stock firmware first
   - Re-flash StoneOS
   - Check `adb logcat` during boot

2. **UI Not Starting**
   - Check WebView permissions
   - Verify React bundle is present
   - Review system logs

3. **MCP Service Crashes**
   - Check SELinux denials: `adb logcat -s audit`
   - Verify binder permissions
   - Review service logs

## Next Steps

- Set up [CI/CD pipeline](./ci-cd.md)
- Configure [testing environment](./testing.md)
- Review [security guidelines](../security/README.md)
- Join development [communication channels](./contributing.md) 