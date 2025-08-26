# AOSP Reference Documentation for StoneOS

> Compiled from official Android Open Source Project documentation
> Purpose: Central reference for StoneOS AOSP development

---

## 1. Build Requirements & Setup

### Hardware Requirements
**Source**: https://source.android.com/docs/setup/build/requirements
- **Storage**: 400 GB free disk space (250 GB to checkout + 150 GB to build)
- **RAM**: Minimum 64 GB RAM (Google standard)
  - Google uses 72-core machines with 64 GB RAM
  - 6-core machine with 64 GB RAM takes ~6 hours for full build
- **System**: 64-bit x86 system

### Operating System
**Source**: https://source.android.com/docs/setup/build/requirements
- 64-bit Linux distribution with GNU C Library (glibc) 2.17 or later
- Ubuntu 18.04 or later recommended
- **Important**: "Android OS development on macOS isn't supported as of June 22, 2021 (Android 11)"

### Software Requirements
**Source**: https://source.android.com/docs/setup/build/requirements
- OpenJDK (prebuilt versions available)
- Make (prebuilt versions available)
- Python 3 (prebuilt versions available)
- Repo (launcher version 2.4 or higher)

---

## 2. Build System & Commands

### Build Environment Setup
**Source**: https://source.android.com/docs/setup/build/building

```bash
# Initialize environment
source build/envsetup.sh

# Choose build target
lunch product_name-release_config-build_variant
```

### Lunch Command Format
**Source**: https://source.android.com/docs/setup/build/building

Format: `lunch product_name-release_config-build_variant`

Components:
- `product_name`: Device/target (e.g., `aosp_husky` for Pixel 8 Pro)
- `release_config`: Feature configuration (e.g., `aosp_current`)
- `build_variant`:
  - `user`: Production build with limited security access
  - `userdebug`: For device developers, follows userdebug guidelines
  - `eng`: Fastest build for day-to-day development

### Build System Architecture
**Source**: https://source.android.com/docs/setup/build
- Uses "Soong" build system
- Build files called "blueprint files" named `Android.bp`
- Similar to Bazel BUILD files
- Supports "feature launch flags" to isolate untested code
- Uses kati (GNU Make clone) and Ninja build system

### Recommended Branch
**Source**: https://source.android.com/docs/setup/build/building
- Use `android-latest-release` instead of `aosp-main` (starting March 27, 2025)

---

## 3. SystemUI Modifications

### Runtime Resource Overlay (RRO) Overview
**Source**: Web search results for "Android SystemUI modification AOSP custom panel overlay"

RRO allows dynamic customization of app resources without modifying APKs:
- Can modify colors, strings, layouts
- Cannot add new Java functionality
- Cannot refer to resource identifiers in target APK
- Overlay APKs placed in `/overlay` directory

### SystemUI Direct Modification Approach
**Source**: Web search results for "Android SystemUI modification AOSP custom panel overlay"

For Java logic changes (required for StoneOS):
- Modify files in `frameworks/base/packages/SystemUI/`
- Register new components in `SystemUI/src/com/android/systemui/qs/QSTileHost.java`
- Build with: `mmm frameworks/base/packages/SystemUI/`
- Push with: `adb push out/target/product/device/system/priv-app/SystemUI /system/priv-app/`

### SystemUI Key Locations
**Source**: Web search results
- Quick Settings: `SystemUI/src/com/android/systemui/qs/`
- Navigation Bar: `SystemUI/src/com/android/systemui/navigationbar/`
- Keyguard: `SystemUI/src/com/android/systemui/keyguard/`
- Status Bar: `SystemUI/src/com/android/systemui/statusbar/`

---

## 4. Window Management & Split Screen

### Window Manager Evolution
**Source**: https://source.android.com/docs/core/display/multi-window

- **Android 9 and lower**: PhoneWindowManager handled display policies
- **Android 10+**: Moved to DisplayPolicy class
- Rotation tracking moved to DisplayRotation class

### Split Screen Implementation
**Source**: https://source.android.com/docs/core/display/split-screen

Split-screen is default multi-window implementation:
- Provides two activity panes
- Multi-window enabled by default in Android 7.0+
- User can drag divider to resize panes

### Non-Resizable Activities
**Source**: Web search results for "Android PhoneWindowManager resize app window split screen"

For apps with `resizeableActivity=false`:
- **Android 7-9**: Platform prevents split-screen mode
- **Android 10+**: Uses Size Compatibility Mode (SCM)
  - Apps temporarily scaled if fixed orientation/aspect ratio
  - Defined in `ActivityRecord#shouldUseSizeCompatMode()`
  - Screen configuration fixed in override configuration

### Window Configuration Storage
**Source**: Web search results for "Android PhoneWindowManager"
- Settings stored in `DisplayWindowSettings` class
- Persisted to `/data/display_settings.xml`
- Configurable per-display windowing settings (Android 10+)

---

## 5. SurfaceFlinger & Grayscale Rendering

### SurfaceFlinger Architecture
**Source**: https://source.android.com/docs/core/graphics/surfaceflinger-windowmanager

- Accepts buffers, composes buffers, sends to display
- WindowManager provides buffers and window metadata
- Walks through layers on VSync signal
- Uses Hardware Composer (HWC) for composition

### SurfaceFlinger Source Location
**Source**: Web search results for "Android SurfaceFlinger color matrix grayscale"
- Main implementation: `frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp`
- System properties: `frameworks/native/services/surfaceflinger/sysprop/SurfaceFlingerProperties.sysprop`

### Color Matrix for Grayscale
**Source**: Web search results for "Android SurfaceFlinger color matrix grayscale"

Recommended grayscale matrix (ITU-R BT.709 luma coefficients):
```cpp
float[] mat = {
    0.2126f, 0.7152f, 0.0722f, 0, 0,  // Red
    0.2126f, 0.7152f, 0.0722f, 0, 0,  // Green  
    0.2126f, 0.7152f, 0.0722f, 0, 0,  // Blue
    0, 0, 0, 1, 0                      // Alpha
}
```

Alternative (simpler but less accurate):
```cpp
float[] mat = {
    0.3f, 0.59f, 0.11f, 0, 0,
    0.3f, 0.59f, 0.11f, 0, 0,
    0.3f, 0.59f, 0.11f, 0, 0,
    0, 0, 0, 1, 0
}
```

### System Properties Configuration
**Source**: https://source.android.com/docs/core/graphics/surfaceflinger-props

Android 10+ approach:
- SurfaceFlinger reads system properties first
- Falls back to ConfigStore HAL if no property defined
- Properties defined in `SurfaceFlingerProperties.sysprop`

---

## 6. Testing & Validation

### Flashing & Testing
**Source**: https://source.android.com/docs/setup/test/running

```bash
# Flash build
fastboot flashall -w  # -w wipes /data partition

# Testing options:
# - Cuttlefish emulator
# - Android Emulator virtual devices
# - Physical device flash
```

### Device Preparation
**Source**: https://source.android.com/docs/setup/test/running
- Enable USB debugging
- Enable OEM unlocking in developer options
- Boot to Fastboot: `adb reboot bootloader`

### CTS (Compatibility Test Suite)
**Source**: AOSP documentation references
- Build with: `make cts`
- Run with: `cts-tradefed run cts-dev`
- Critical modules for StoneOS:
  - `CtsWindowManagerTestCases`
  - `CtsSystemUiTestCases`
  - `CtsGraphicsTestCases`
  - `CtsViewTestCases`

---

## 7. Development Workflow

### Source Control with Repo
**Source**: https://source.android.com/docs/setup/create/coding-tasks

```bash
# Start topic branch
repo start BRANCH_NAME .

# View status
repo status

# Sync projects
repo sync

# Upload changes
repo upload
```

### Best Practices
**Source**: https://source.android.com/docs/setup/create/coding-tasks
- Create clear, concise, detailed commit messages
- Use topic branches to isolate work
- Verify changes before uploading
- Stage and commit carefully
- Review via Gerrit

---

## 8. Key Implementation Details for StoneOS

### DisplayPolicy (Android 10+) vs PhoneWindowManager (Android 9-)
**Sources**: Combined from web searches

For Android 10+, modify:
- `/frameworks/base/services/core/java/com/android/server/wm/DisplayPolicy.java`
- `/frameworks/base/services/core/java/com/android/server/wm/TaskDisplayArea.java`

For Android 9 and below, modify:
- `/frameworks/base/services/core/java/com/android/server/policy/PhoneWindowManager.java`

### Gesture Detection
**Source**: Web searches and AOSP structure

Primary file for system gestures:
- `/frameworks/base/services/core/java/com/android/server/wm/SystemGesturesPointerEventListener.java`

### Build Optimization for Limited RAM
**Source**: Combined from multiple sources

For 16GB RAM systems:
```bash
# Limit parallel jobs
make -j4  # Instead of -j$(nproc)

# Set Java heap
export ANDROID_JACK_VM_ARGS="-Xmx4g -Dfile.encoding=UTF-8"

# Enable ccache
export USE_CCACHE=1
export CCACHE_DIR=/path/to/ccache
ccache -M 50G
```

---

## 9. Important Limitations & Considerations

### macOS Development
**Source**: https://source.android.com/docs/setup/build/requirements
- Officially unsupported since Android 11
- May work with workarounds but not guaranteed
- Linux VM or dedicated Linux machine recommended

### RRO Limitations
**Source**: Web search results
- Cannot add new Java source functionality
- Cannot add new resource identifiers
- Only modifies existing resources
- For StoneOS panel logic, direct modification required

### Non-Resizable Apps
**Source**: Multiple Android documentation sources
- Must handle apps with `resizeableActivity=false`
- Use Size Compatibility Mode in Android 10+
- May require letterboxing or scaling

### Performance Considerations
**Source**: Combined from searches
- Grayscale filtering adds GPU overhead
- Monitor with `dumpsys gfxinfo`
- Target < 5% performance impact
- Consider selective application if needed

---

## 10. Debugging & Monitoring Commands

### Essential ADB Commands
**Sources**: Various AOSP documentation

```bash
# SystemUI logs
adb logcat -s SystemUI:*

# SurfaceFlinger state
adb shell dumpsys SurfaceFlinger

# Window manager
adb shell dumpsys window

# Graphics info
adb shell dumpsys gfxinfo com.android.systemui

# Memory usage
adb shell dumpsys meminfo com.android.systemui

# System properties
adb shell getprop | grep stoneos
```

### Performance Analysis
**Source**: AOSP documentation
```bash
# Systrace
python systrace.py --time=10 -o trace.html gfx view wm

# GPU monitoring
adb shell cat /sys/class/kgsl/kgsl-3d0/gpubusy

# Frame stats
adb shell dumpsys gfxinfo com.android.systemui framestats
```

---

## Additional Resources

### Official Documentation
- AOSP Main: https://source.android.com
- Building Guide: https://source.android.com/docs/setup/build/building
- Multi-Window: https://source.android.com/docs/core/display/multi-window
- Split Screen: https://source.android.com/docs/core/display/split-screen
- Testing: https://source.android.com/docs/setup/test

### Source Code Repositories
- SystemUI: https://android.googlesource.com/platform/frameworks/base/+/master/packages/SystemUI/
- SurfaceFlinger: https://android.googlesource.com/platform/frameworks/native/+/master/services/surfaceflinger/
- WindowManager: https://android.googlesource.com/platform/frameworks/base/+/master/services/core/java/com/android/server/wm/

### GitHub Mirrors
- AOSP Mirror: https://github.com/aosp-mirror/
- PhoneWindowManager: https://github.com/aosp-mirror/platform_frameworks_base/blob/master/services/core/java/com/android/server/policy/PhoneWindowManager.java

---

*Note: This documentation compiled from official AOSP sources and web searches conducted in November 2024. Always verify with latest AOSP documentation as APIs and structures may change between Android versions.*