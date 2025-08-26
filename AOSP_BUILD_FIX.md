# AOSP Build Fix for StoneOS

## The Problem
The `trunk_staging` error occurs because we're using the wrong release configuration. The AOSP build system changed in Android 14/15.

## The Solution

### Option 1: Use Correct Release Config (Recommended)

```bash
cd ~/aosp
source build/envsetup.sh

# For Android 14, use ap2a:
lunch aosp_x86_64-ap2a-eng

# For Android 15, use ap3a:
lunch aosp_x86_64-ap3a-eng

# For latest development, use aosp_current:
lunch aosp_x86_64-aosp_current-eng

# Build SystemUI
m SystemUI
```

### Option 2: Use Generic Target

```bash
cd ~/aosp
source build/envsetup.sh

# List available targets
lunch

# Pick a number from the list (usually 1 for generic)
lunch 1

# Build
m SystemUI
```

### Option 3: Build Without Lunch (Direct)

```bash
cd ~/aosp
source build/envsetup.sh

# Set build variables directly
export TARGET_PRODUCT=aosp_x86_64
export TARGET_BUILD_VARIANT=eng
export TARGET_RELEASE=ap3a

# Build
make SystemUI -j8
```

## Complete Build Commands for StoneOS

```bash
# 1. Download AOSP (if not already done)
mkdir ~/aosp && cd ~/aosp
repo init -u https://android.googlesource.com/platform/manifest -b android-14.0.0_r61
repo sync -c -j8

# 2. Add our Stone modifications
mkdir -p frameworks/base/packages/SystemUI/src/com/android/systemui/stone/
# Copy StonePanel.java and StoneIcon.java here

# 3. Build with correct config
source build/envsetup.sh
lunch aosp_x86_64-ap2a-eng  # For Android 14
m SystemUI

# 4. Output location
# SystemUI.apk will be at:
# out/target/product/generic_x86_64/system/system_ext/priv-app/SystemUI/SystemUI.apk
```

## Why This Works

1. **trunk_staging** is Google's internal development config, not available in public AOSP
2. **ap2a** = Android 14 QPR2 (Quarterly Platform Release)
3. **ap3a** = Android 15 QPR1 
4. **aosp_current** = Latest development release

## Testing Without Full Build

If you just want to test the Stone panel concept:

```bash
# Create a test app instead of modifying SystemUI
cd ~/stone-test
npx react-native init StonePanel
cd StonePanel

# Add our panel logic to App.tsx
# This lets us test the UI without AOSP builds
```

## Next Steps

1. Set up a Linux machine (not VM) for faster builds
2. Use the correct release config (ap2a or ap3a)
3. Build incrementally (just SystemUI, not full AOSP)
4. Test in emulator before device