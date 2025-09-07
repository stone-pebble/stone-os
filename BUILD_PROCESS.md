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