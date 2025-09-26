# StoneOS Build Process Documentation

## Successfully Tested Build Process (Sept 6, 2024)

### What Actually Works

**Build Configuration:**
- Instance: `n2-standard-16` (16 vCPUs, 64GB RAM) on GCP
- AOSP Branch: `android-14.0.0_r61` 
- Build Target: `aosp_x86_64-ap2a-eng` (for emulator)
- Total Time: ~35 minutes
- Total Cost: ~$0.15 (SPOT instance)

### Critical Learnings

1. **AOSP Download MUST use `-j4`** 
   - Higher parallelism (j8, j16, j32) causes HTTP 429 rate limiting
   - Download takes ~15 minutes with j4
   - Retry logic helps with transient failures

2. **Build can use `-j8` successfully**
   - SystemUI build takes ~20 minutes
   - Produces 42MB APK

3. **Directory Must Be `$HOME`**
   - Cannot use `/home` directly - permission denied
   - Must use `$HOME/aosp` for build directory

4. **Startup Script Strategy**
   - Touch `/tmp/ready` immediately 
   - Install packages in background
   - Prevents timeout during instance setup

5. **Label Format Requirements**
   - No periods or underscores in GCP labels
   - Use hyphens only: `stoneos-v0-1-0-20250906-143114`

### Verified Working Commands

```bash
# 1. Initialize AOSP
cd $HOME
mkdir -p aosp && cd aosp
repo init -u https://android.googlesource.com/platform/manifest \
    -b android-14.0.0_r61 --depth=1

# 2. Sync AOSP (MUST use j4)
repo sync -c -j4 --no-tags --no-clone-bundle --current-branch

# 3. Copy Stone components
mkdir -p frameworks/base/packages/SystemUI/src/com/android/systemui/stone/
cp /tmp/stone/stone/*.java \
    frameworks/base/packages/SystemUI/src/com/android/systemui/stone/

# 4. Build
source build/envsetup.sh
lunch aosp_x86_64-ap2a-eng
m SystemUI -j8

# 5. APK Location
out/target/product/generic_x86_64/system/system_ext/priv-app/SystemUI/SystemUI.apk
```

### Files Modified

- `SystemUI/stone/StonePanel.java` - Added version tracking and logging
- `SystemUI/stone/StoneIcon.java` - Original implementation

### Key Learning: Proper AOSP Customization Method

**Device Overlays are the correct way to add custom code to AOSP:**
- Create device tree at `device/vendor/product/`
- Use `DEVICE_PACKAGE_OVERLAYS` in device.mk
- Files automatically merged during build
- This is how Samsung, OnePlus, etc. customize Android

**Why direct Android.bp modification fails:**
- Glob patterns like `"src/**/*.java"` are evaluated at parse time
- Build system caches file lists
- Adding files after initial scan doesn't work
- SystemUI-core library structure makes patching complex

### Build Output

- APK Size: 42MB
- Location: `builds/manual-20250906/StoneOS_SystemUI.apk`
- Contains Stone panel that slides up from bottom (1/3 screen)

### Testing in Emulator

```bash
# After build completes, test locally:
# 1. Install Android Studio: brew install --cask android-studio
#    - Choose CUSTOM setup (not Standard)
#    - Select Android 14 (API 34) to match our build
#    - Choose x86_64 system image (faster than ARM)
# 2. Create AVD: Pixel 8a with API 34
# 3. Start emulator with writable system
~/Library/Android/sdk/emulator/emulator -avd Pixel_8a_StoneOS -writable-system
# 4. Install SystemUI
adb root && adb remount
adb push StoneOS_SystemUI.apk /system/system_ext/priv-app/SystemUI/SystemUI.apk
adb shell pkill -f com.android.systemui
# 5. Monitor for Stone classes
adb logcat | grep -E "StoneOS|StonePanel"
```

## Fork Approach - Current Status (Sept 9, 2024)

### MAJOR BREAKTHROUGH: Forked AOSP Approach
- **Problem Solved**: Stone classes weren't being included because glob patterns are evaluated at parse time
- **Solution**: Forked AOSP frameworks/base repository (like LineageOS does)
- **Fork Created**: https://github.com/stone-pebble/stoneos-frameworks
- **Stone Files Added**: Committed directly to fork at `packages/SystemUI/src/com/android/systemui/stone/`
- **Build Script Updated**: Now uses local manifest to replace Google's frameworks/base with our fork

### Fork Approach Works But Needs Android.bp Modification
- **Discovery**: Files ARE in source tree from fork but NOT being compiled
- **Root Cause**: SystemUI uses multiple modules; our files need proper module integration
- **Solution Required**: Must modify Android.bp in fork to either:
  1. Create separate SystemUI-stone module (recommended)
  2. Explicitly list Stone files in srcs array
  3. Use a filegroup for Stone files
- **Key Learning**: Fork approach is correct but requires proper AOSP build system integration

### The Solution: Android.bp Modification in Fork

#### Option 1: Add Stone as a Separate Module (Recommended)
```java
// In packages/SystemUI/Android.bp, add:
android_library {
    name: "SystemUI-stone",
    srcs: [
        "stone/src/**/*.java",
    ],
    static_libs: [
        "SystemUI-core",
    ],
    manifest: "stone/AndroidManifest.xml",
}

// Then include it in SystemUI app:
android_app {
    name: "SystemUI",
    static_libs: [
        "SystemUI-stone",  // Add this line
        "SystemUI-core",
        // ... other libs
    ],
}
```

#### Option 2: Explicitly List Files in Android.bp
```java
android_library {
    name: "SystemUI-core",
    srcs: [
        "src/**/*.kt",
        "src/**/*.java",
        "src/**/I*.aidl",
        // Add these lines:
        "src/com/android/systemui/stone/StonePanel.java",
        "src/com/android/systemui/stone/StoneIcon.java",
        ":ReleaseJavaFiles",
        // ... rest of srcs
    ],
}
```

#### Option 3: Use a Filegroup
```java
filegroup {
    name: "StoneFiles",
    srcs: [
        "src/com/android/systemui/stone/*.java",
    ],
}

android_library {
    name: "SystemUI-core",
    srcs: [
        "src/**/*.kt",
        "src/**/*.java",
        ":StoneFiles",  // Add this
        // ... rest
    ],
}
```

### Implementation Steps
1. **Update the fork** with one of the above solutions in Android.bp
2. **Push changes** to the fork
3. **Clean build** to ensure no cached file lists
4. **Rebuild** SystemUI

### Testing the Fix
```bash
# On build instance
cd /home/samuellarson/aosp

# Clean everything
rm -rf out/

# Sync to get updated Android.bp from fork
repo sync -c frameworks/base -j4 --force-sync

# Build
source build/envsetup.sh
lunch aosp_x86_64-ap2a-eng
m SystemUI -j16

# Verify
unzip -l out/target/product/generic_x86_64/system/system_ext/priv-app/SystemUI/SystemUI.apk | grep -i stone
```

## Building for Real Pixel 8a Device

### Verified Device Information
- **Codename**: akita (confirmed)
- **Platform**: Zuma (Tensor G3, shared with Pixel 8/8 Pro)
- **Kernel**: 6.1 common kernel
- **Architecture**: ARM64

### Required Components for Pixel 8a Build

1. **Vendor Binaries** (Download from Google)
   ```bash
   # Option A: From developers.google.com/android/drivers
   # Download vendor image for your Android version
   
   # Option B: From ci.android.com
   # Go to: ci.android.com → aosp_akita_16k → download .sh artifact
   ```

2. **Correct Build Target**
   ```bash
   lunch aosp_akita-userdebug      # Standard build
   lunch aosp_akita_16k-userdebug  # 16KB page size (newer)
   ```

3. **Build Commands**
   ```bash
   source build/envsetup.sh
   lunch aosp_akita-userdebug
   m vendorbootimage vendorkernelbootimage target-files-package
   ```

### Why Emulator Build ≠ Device Build
- **x86_64 emulator**: Generic drivers, virtual hardware
- **Pixel 8a (ARM64)**: Needs Tensor G3 drivers, real hardware support
- **Our current build**: Works in emulator only, NOT on device

### Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| HTTP 429 during repo sync | Use `-j4` not higher |
| Permission denied in /home | Use `$HOME` instead |
| Instance timeout | Touch ready file early |
| sed incompatibility Mac/Linux | Use perl instead |
| Label format error | No periods/underscores |
| Stone classes not in APK | Files must exist BEFORE build system starts |
| Glob patterns not picking up files | Patterns evaluated at parse time, not build time |
| Device overlays not working for code | Only work for resources (XML, images), not Java/Kotlin |
| Android.mk not working | SystemUI uses Android.bp (Soong), not Make |
| Custom lunch without device tree | Need full device configuration, not just lunch choice |

## Build Performance Findings

### Optimal Configuration
- **Instance**: n2-standard-32 (32 vCPUs) for faster builds
- **AOSP Download**: MUST use `-j4` (higher causes HTTP 429 rate limiting)
- **Build**: Can use `-j16` or `-j32` successfully
- **Total Time**: ~35-40 minutes
- **Cost**: ~$0.15-0.25 per build with SPOT instances

### Build Phases
1. **AOSP Download**: ~15-20 minutes (limited by Google's servers)
2. **SystemUI Build**: ~15-20 minutes with 32 cores
3. **Verification**: Can take several minutes for dexdump on 42MB APK

## Why Each Alternative Won't Work

### Magisk Modules
- **Problem**: SystemUI requires platform signature
- **Reality**: Can't replace SystemUI without AOSP keys

### APK Modification
- **Problem**: Signature verification
- **Reality**: Modified APK won't install without re-signing

### Overlay Services
- **Problem**: Can't achieve true window split
- **Reality**: Android security model prevents system-level UI changes

### Xposed/LSPosed
- **Problem**: Incompatible with Android 15
- **Reality**: Development has lagged behind AOSP